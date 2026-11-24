package dev.koikaster.mite;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

final class BossRewardMailbox extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE = "mite_boss_reward_mailbox";
    private static final String RECEIPTS = "AppliedOfflineBossRewards";

    private final Map<UUID, Set<UUID>> departures = new HashMap<>();
    private final Map<UUID, Map<UUID, ListTag>> grants = new HashMap<>();
    private final Set<UUID> firstSkeletonVictories = new HashSet<>();

    boolean firstSkeletonVictoryRecorded(UUID owner) {
        return firstSkeletonVictories.contains(owner);
    }

    static BossRewardMailbox get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
            .computeIfAbsent(BossRewardMailbox::load, BossRewardMailbox::new, FILE);
    }

    void disconnected(UUID player, Set<UUID> encounters) {
        if (encounters.isEmpty()) {
            if (departures.remove(player) != null) setDirty();
        } else {
            var current = new HashSet<>(encounters);
            if (!current.equals(departures.put(player, current))) setDirty();
        }
    }

    boolean departedFrom(UUID player, UUID encounter) {
        return departures.getOrDefault(player, Set.of()).contains(encounter);
    }

    boolean resolved(UUID player, UUID encounter) {
        return grants.getOrDefault(player, Map.of()).containsKey(encounter);
    }

    void award(UUID player, UUID encounter, List<ItemStack> rewards) {
        var inbox = grants.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        if (inbox.containsKey(encounter)) return;
        var items = new ListTag();
        for (var reward : rewards) {
            if (reward.isEmpty()) continue;
            items.add(reward.save(new CompoundTag()));
            if (reward.is(GiantSkeletonContent.BONE.get()) && reward.getCount() == GiantSkeletonFirsts.FIRST_BONES) {
                firstSkeletonVictories.add(player);
            }
        }
        inbox.put(encounter, items);
        setDirty();
    }

    void closeEncounter(UUID encounter) {
        for (var owners = departures.values().iterator(); owners.hasNext();) {
            var associated = owners.next();
            if (associated.remove(encounter)) {
                if (associated.isEmpty()) owners.remove();
                setDirty();
            }
        }
    }

    void loggedIn(ServerPlayer player) {
        UUID owner = player.getUUID();
        disconnected(owner, Set.of());
        var inbox = grants.get(owner);
        if (inbox == null) return;
        // PlayerList has loaded the authoritative player data before posting this event.
        var applied = readReceipts(PlayerState.data(player).getList(RECEIPTS, Tag.TAG_COMPOUND));
        for (var entry : inbox.entrySet()) {
            if (!entry.getValue().isEmpty() && applied.contains(entry.getKey())) {
                // Keep the encounter key after acknowledging its already saved payload.
                entry.setValue(new ListTag());
                setDirty();
            }
        }
    }

    boolean addToPending(ServerPlayer player, ListTag pending) {
        var inbox = grants.get(player.getUUID());
        if (inbox == null || inbox.values().stream().allMatch(ListTag::isEmpty)) return false;

        var state = PlayerState.data(player);
        var receipts = state.getList(RECEIPTS, Tag.TAG_COMPOUND);
        var applied = readReceipts(receipts);
        if (inbox.entrySet().stream().noneMatch(entry -> !entry.getValue().isEmpty() && !applied.contains(entry.getKey()))
                || !persist(player.serverLevel())) return false;

        for (var entry : inbox.entrySet()) {
            if (entry.getValue().isEmpty() || !applied.add(entry.getKey())) continue;
            for (Tag item : entry.getValue()) pending.add(item.copy());
            var receipt = new CompoundTag();
            receipt.putUUID("Encounter", entry.getKey());
            receipts.add(receipt);
        }
        // Both collections are written as part of the same native player data record.
        state.put(RECEIPTS, receipts);
        return true;
    }

    private static Set<UUID> readReceipts(ListTag list) {
        var completed = new HashSet<UUID>();
        for (int index = 0; index < list.size(); index++) {
            var row = list.getCompound(index);
            if (row.hasUUID("Encounter")) completed.add(row.getUUID("Encounter"));
        }
        return completed;
    }

    boolean persist(ServerLevel level) {
        if (isDirty()) {
            var world = level.getServer().getWorldPath(LevelResource.ROOT);
            save(world.resolve("data").resolve(FILE + ".dat").toFile());
        }
        return !isDirty();
    }

    @Override
    public void save(File path) {
        if (!isDirty()) return;
        var envelope = new CompoundTag();
        envelope.put("data", save(new CompoundTag()));
        NbtUtils.addCurrentDataVersion(envelope);
        try {
            AtomicGraveFile207.write(path.toPath(), envelope);
            setDirty(false);
        } catch (IOException failure) {
            LOGGER.error("Could not save disconnected boss rewards at {}", path, failure);
        }
    }

    private static BossRewardMailbox load(CompoundTag data) {
        var saved = new BossRewardMailbox();
        var departures = data.getList("Departures", Tag.TAG_COMPOUND);
        for (int index = 0; index < departures.size(); index++) {
            var row = departures.getCompound(index);
            if (row.hasUUID("Player") && row.hasUUID("Encounter")) {
                saved.departures.computeIfAbsent(row.getUUID("Player"), ignored -> new HashSet<>()).add(row.getUUID("Encounter"));
            }
        }
        var grants = data.getList("Grants", Tag.TAG_COMPOUND);
        for (int index = 0; index < grants.size(); index++) {
            var row = grants.getCompound(index);
            if (row.hasUUID("Player") && row.hasUUID("Encounter") && row.contains("Items", Tag.TAG_LIST)) {
                saved.grants.computeIfAbsent(row.getUUID("Player"), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(row.getUUID("Encounter"), row.getList("Items", Tag.TAG_COMPOUND).copy());
            }
        }
        var victories = data.getList("FirstSkeletonVictories", Tag.TAG_COMPOUND);
        for (int index = 0; index < victories.size(); index++) {
            var row = victories.getCompound(index);
            if (row.hasUUID("Player")) saved.firstSkeletonVictories.add(row.getUUID("Player"));
        }
        return saved;
    }

    @Override
    public CompoundTag save(CompoundTag data) {
        var disconnected = new ListTag();
        departures.forEach((owner, encounters) -> {
            for (UUID encounter : encounters) disconnected.add(entry(owner, encounter));
        });
        data.put("Departures", disconnected);

        var awards = new ListTag();
        grants.forEach((owner, inbox) -> inbox.forEach((encounter, items) -> {
            var row = entry(owner, encounter);
            row.put("Items", items.copy());
            awards.add(row);
        }));
        data.put("Grants", awards);

        var victories = new ListTag();
        for (UUID owner : firstSkeletonVictories) {
            var row = new CompoundTag();
            row.putUUID("Player", owner);
            victories.add(row);
        }
        data.put("FirstSkeletonVictories", victories);
        return data;
    }

    private static CompoundTag entry(UUID player, UUID encounter) {
        var tag = new CompoundTag();
        tag.putUUID("Player", player);
        tag.putUUID("Encounter", encounter);
        return tag;
    }
}
