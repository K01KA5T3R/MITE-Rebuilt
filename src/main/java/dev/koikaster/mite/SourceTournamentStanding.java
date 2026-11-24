package dev.koikaster.mite;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Source TournamentStanding, stored once per world so offline and respawned players keep their standing. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceTournamentStanding extends SavedData {
    private static final String SAVE_ID = "mite_r195_tournament_standings";

    enum Milestone {
        FISH("caught_a_fish", 300),
        SKELETON("killed_a_skeleton", 100),
        ZOMBIE("killed_a_zombie", 200),
        SPIDER("killed_a_spider", 200),
        WOOD_SPIDER("killed_a_wood_spider", 200),
        CREEPER("killed_a_creeper", 200),
        LARGE_SLIME("killed_a_large_slime", 300),
        GHOUL("killed_a_ghoul", 300),
        WIGHT("killed_a_wight", 400),
        INVISIBLE_STALKER("killed_an_invisible_stalker", 300),
        WITCH("killed_a_witch", 500),
        SHADOW("killed_a_shadow", 500),
        HELLHOUND("killed_a_hellhound", 700),
        DEMON_SPIDER("killed_a_demon_spider", 1000);

        final String key;
        final int points;

        Milestone(String key, int points) {
            this.key = key;
            this.points = points;
        }
    }

    enum Harvest {
        COPPER_NUGGET("copper_nuggets_harvested", 100),
        SILVER_NUGGET("silver_nuggets_harvested", 200),
        GOLD_NUGGET("gold_nuggets_harvested", 300),
        MITHRIL_NUGGET("mithril_nuggets_harvested", 500),
        ADAMANTIUM_NUGGET("adamantium_nuggets_harvested", 900),
        COPPER_ORE("copper_ore_harvested", 900),
        SILVER_ORE("silver_ore_harvested", 1800),
        GOLD_ORE("gold_ore_harvested", 2700),
        IRON_ORE("iron_ore_harvested", 2700),
        MITHRIL_ORE("mithril_ore_harvested", 4500),
        ADAMANTIUM_ORE("adamantium_ore_harvested", 8100);

        final String key;
        final int points;

        Harvest(String key, int points) {
            this.key = key;
            this.points = points;
        }
    }

    private static final class Standing {
        final CompoundTag counters;
        String name;
        int experience;

        Standing(String name, int experience, CompoundTag counters) {
            this.name = name;
            this.experience = experience;
            this.counters = counters;
        }

        int points() {
            int result = experience;
            for (var milestone : Milestone.values()) {
                if (counters.getBoolean(milestone.key)) result += milestone.points;
            }
            for (var harvest : Harvest.values()) result += counters.getInt(harvest.key) * harvest.points;
            return result;
        }
    }

    private final Map<UUID, Standing> entries = new LinkedHashMap<>();
    private final Map<UUID, Integer> lastSent = new HashMap<>();

    static boolean active(ServerPlayer player) {
        return SourceTournament.mode(player.getServer()) == SourceTournament.Mode.SCORE;
    }

    private static SourceTournamentStanding get(ServerPlayer player) {
        return get(player.getServer());
    }

    private static SourceTournamentStanding get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SourceTournamentStanding::load, SourceTournamentStanding::new, SAVE_ID);
    }

    private Standing update(ServerPlayer player) {
        int experience = PlayerState.data(player).getInt("Xp");
        String name = player.getGameProfile().getName();
        var standing = entries.get(player.getUUID());
        if (standing == null) {
            standing = new Standing(name, experience, new CompoundTag());
            entries.put(player.getUUID(), standing);
            setDirty();
        } else if (standing.experience != experience || !standing.name.equals(name)) {
            standing.experience = experience;
            standing.name = name;
            setDirty();
        }
        return standing;
    }

    static int points(ServerPlayer player) {
        return active(player) ? get(player).update(player).points() : 0;
    }

    private static void mark(ServerPlayer player, Milestone milestone) {
        var state = get(player);
        var standing = state.update(player);
        if (standing.counters.getBoolean(milestone.key)) return;
        standing.counters.putBoolean(milestone.key, true);
        state.setDirty();
    }

    static void harvest(ServerPlayer player, Harvest harvest) {
        var state = get(player);
        var counters = state.update(player).counters;
        counters.putInt(harvest.key, counters.getInt(harvest.key) + 1);
        state.setDirty();
    }

    static void killed(ServerPlayer player, Entity victim) {
        if (!active(player)) return;
        var id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (id == null) return;
        Milestone milestone = switch (id.toString()) {
            case "minecraft:skeleton", "minecraft:wither_skeleton", "mite_reforged:longdead",
                    "mite_reforged:longdead_guardian", "mite_reforged:bone_lord", "mite_reforged:ancient_bone_lord" ->
                    Milestone.SKELETON;
            case "minecraft:zombie", "minecraft:zombie_villager", "minecraft:zombified_piglin",
                    "mite_reforged:revenant" -> Milestone.ZOMBIE;
            case "minecraft:spider" -> Milestone.SPIDER;
            case "mite_reforged:wood_spider", "mite_reforged:phase_spider",
                    "mite_reforged:black_widow_spider" -> Milestone.WOOD_SPIDER;
            case "minecraft:creeper", "mite_reforged:infernal_creeper" -> Milestone.CREEPER;
            case "minecraft:slime" -> victim instanceof Slime slime && slime.getSize() == 4
                    ? Milestone.LARGE_SLIME : null;
            case "mite_reforged:ghoul" -> Milestone.GHOUL;
            case "mite_reforged:wight" -> Milestone.WIGHT;
            case "mite_reforged:invisible_stalker" -> Milestone.INVISIBLE_STALKER;
            case "minecraft:witch" -> Milestone.WITCH;
            case "mite_reforged:shadow" -> Milestone.SHADOW;
            case "mite_reforged:hellhound" -> Milestone.HELLHOUND;
            case "mite_reforged:demon_spider" -> Milestone.DEMON_SPIDER;
            default -> null;
        };
        if (milestone != null) mark(player, milestone);
    }

    /** FishingHook only calls this while spawning the committed loot or its XP orb. */
    public static boolean spawnFishingResult(Level level, Entity result, FishingHook hook) {
        boolean spawned = level.addFreshEntity(result);
        if (spawned && result instanceof ItemEntity item && hook.getPlayerOwner() instanceof ServerPlayer player
                && active(player) && FishingRules.isFish(item.getItem())) {
            mark(player, Milestone.FISH);
        }
        return spawned;
    }

    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player)) {
            var data = get(player);
            data.publish(player, data.update(player));
        }
    }

    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player)) {
            var data = get(player);
            data.update(player);
            data.lastSent.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && active(player)) {
            var data = get(player);
            data.publish(player, data.update(player));
        }
    }

    private void publish(ServerPlayer player, Standing standing) {
        if (player.connection == null || player instanceof FakePlayer) return;
        int points = standing.points();
        Integer previous = lastSent.put(player.getUUID(), points);
        if (previous == null || previous != points) {
            SourceTournamentTransport.sendScore(player, points, previous != null);
        }
    }

    @SubscribeEvent
    public static void appendPeriodicReport(TickEvent.ServerTickEvent event) {
        var server = event.getServer();
        if (event.phase == TickEvent.Phase.END && server.getTickCount() % 1000 == 0
                && SourceTournament.mode(server) == SourceTournament.Mode.SCORE) {
            get(server).appendReport(server, false);
        }
    }

    @SubscribeEvent
    public static void appendFinalReport(ServerStoppingEvent event) {
        var server = event.getServer();
        if (SourceTournament.mode(server) == SourceTournament.Mode.SCORE) {
            get(server).appendReport(server, true);
        }
    }

    private void appendReport(MinecraftServer server, boolean stopping) {
        for (var player : server.getPlayerList().getPlayers()) update(player);
        long time = server.overworld().getDayTime();
        int day = SourceMoonCycle.dayOfWorld(time);
        int hour = (int) (Math.floorMod(time + 6000L, 24000L) / 1000L);
        var text = new StringBuilder();
        if (stopping) text.append("*** Server Shutting Down ***\n\n");
        if (hour == 0) {
            text.append("End of Day ").append(day - 1);
        } else {
            text.append("Day ").append(day).append(", ");
            if (hour == 12) text.append("Noon");
            else text.append(hour > 12 ? hour - 12 : hour).append(hour < 12 ? " AM" : " PM");
        }
        text.append("\n--------------------\n");

        // R195 excludes its own test account from the exported table, while still tracking its score.
        var players = entries.entrySet().stream().filter(row -> !row.getValue().name.equals("avernite"))
                .sorted(Comparator.<Map.Entry<UUID, Standing>>comparingInt(row -> row.getValue().points())
                        .reversed().thenComparing(row -> row.getValue().name).thenComparing(Map.Entry::getKey))
                .toList();
        if (players.isEmpty()) text.append("(No players have joined yet)\n");
        for (var row : players) describe(text, row.getValue());
        text.append('\n');
        try {
            Files.writeString(Path.of("tournament_standings.txt"), text, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException error) {
            LogUtils.getLogger().warn("Unable to append R195 tournament standings", error);
        }
    }

    private static void describe(StringBuilder text, Standing standing) {
        text.append(standing.name).append(" (").append(standing.points()).append(") - ");
        var kills = new StringJoiner(", ");
        for (var milestone : Milestone.values()) {
            if (milestone != Milestone.FISH && standing.counters.getBoolean(milestone.key)) {
                kills.add(milestone.name().toLowerCase(Locale.ROOT).replace('_', ' '));
            }
        }
        var harvests = new StringJoiner(", ");
        for (var harvest : Harvest.values()) {
            int count = standing.counters.getInt(harvest.key);
            if (count != 0) harvests.add(count + " " + harvest.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        }
        text.append("killed ").append(kills.length() == 0 ? "no mobs" : kills)
                .append(", harvested ").append(harvests.length() == 0 ? "no scoring items" : harvests);
        if (standing.counters.getBoolean(Milestone.FISH.key)) text.append(", caught a fish");
        text.append(", ").append(standing.experience).append(" experience.\n");
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        var rows = new ListTag();
        entries.forEach((id, standing) -> {
            var row = new CompoundTag();
            row.putUUID("Player", id);
            row.putString("Name", standing.name);
            row.putInt("Experience", standing.experience);
            row.put("Counters", standing.counters.copy());
            rows.add(row);
        });
        root.put("Standings", rows);
        return root;
    }

    private static SourceTournamentStanding load(CompoundTag root) {
        var state = new SourceTournamentStanding();
        for (Tag raw : root.getList("Standings", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (row.hasUUID("Player")) {
                state.entries.put(row.getUUID("Player"), new Standing(row.getString("Name"),
                        row.getInt("Experience"), row.getCompound("Counters")));
            }
        }
        return state;
    }
}
