package dev.koikaster.mite;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class BossRewardDelivery {
    private static final String PENDING = "PendingBossRewards";
    // The widest active arena of the three bosses is 34 blocks. Each boss still checks its full arena.
    private static final double LOGOUT_SEARCH_RANGE = 40;

    static void settle(ServerLevel level, UUID encounter, Collection<UUID> contributors,
                       Predicate<ServerPlayer> active, BiFunction<UUID, Boolean, List<ItemStack>> rewards) {
        var mailbox = BossRewardMailbox.get(level);
        boolean addedGrant = false;
        for (UUID owner : contributors) {
            if (mailbox.resolved(owner, encounter)) continue;
            var online = level.getServer().getPlayerList().getPlayer(owner);
            if (online != null) {
                if (online.serverLevel() == level && active.test(online)) {
                    for (var reward : rewards.apply(owner, false)) {
                        if (!reward.isEmpty()) give(online, reward);
                    }
                }
            } else if (mailbox.departedFrom(owner, encounter)) {
                mailbox.award(owner, encounter, rewards.apply(owner, true));
                addedGrant = true;
            }
        }
        mailbox.closeEncounter(encounter);
        if (addedGrant) mailbox.persist(level);
    }

    static void close(ServerLevel level, UUID encounter) {
        BossRewardMailbox.get(level).closeEncounter(encounter);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        var encounters = new HashSet<UUID>();
        if (eligibleToReceive(player)) {
            var nearby = player.getBoundingBox().inflate(LOGOUT_SEARCH_RANGE);
            for (var actor : level.getEntitiesOfClass(LivingEntity.class, nearby,
                    entity -> entity.isAlive() && (entity instanceof SourceRedstoneGolem
                        || entity instanceof SourceGiantSkeleton || MythrilKnightEncounter.member(entity)))) {
                UUID encounter;
                if (actor instanceof SourceRedstoneGolem golem) {
                    encounter = golem.canRecordRewardDeparture(player) ? golem.getUUID() : null;
                } else if (actor instanceof SourceGiantSkeleton giant) {
                    encounter = giant.canRecordRewardDeparture(player) ? giant.getUUID() : null;
                } else {
                    encounter = MythrilKnightEncounter.rewardDeparture(level, actor, player);
                }
                if (encounter != null) encounters.add(encounter);
            }
        }
        BossRewardMailbox.get(level).disconnected(player.getUUID(), encounters);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BossRewardMailbox.get(player.serverLevel()).loggedIn(player);
        }
    }

    static void give(ServerPlayer player, ItemStack reward) {
        player.getInventory().add(reward);
        if (reward.isEmpty()) return;

        var level = player.serverLevel();
        var drop = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), reward.copy());
        drop.setTarget(player.getUUID());
        drop.setDefaultPickUpDelay();
        if (level.addFreshEntity(drop)) return;

        // Forge can refuse the item entity after the inventory accepts part of the stack.
        var data = PlayerState.data(player);
        var pending = data.getList(PENDING, Tag.TAG_COMPOUND);
        pending.add(reward.save(new CompoundTag()));
        data.put(PENDING, pending);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0 || !eligibleToReceive(player)) return;

        var data = PlayerState.data(player);
        var pending = data.getList(PENDING, Tag.TAG_COMPOUND);
        if (BossRewardMailbox.get(player.serverLevel()).addToPending(player, pending)) data.put(PENDING, pending);
        boolean changedInventory = false;
        for (int index = 0; index < pending.size();) {
            var reward = ItemStack.of(pending.getCompound(index));
            int before = reward.getCount();
            player.getInventory().add(reward);
            if (reward.getCount() < before) changedInventory = true;

            if (reward.isEmpty()) {
                pending.remove(index);
            } else {
                if (reward.getCount() < before) pending.set(index, reward.save(new CompoundTag()));
                index++;
            }
        }
        if (pending.isEmpty()) data.remove(PENDING);
        if (changedInventory) player.containerMenu.broadcastChanges();
    }

    private static boolean eligibleToReceive(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !Incapacitation2123.incapacitated(player);
    }

    private BossRewardDelivery() {}
}
