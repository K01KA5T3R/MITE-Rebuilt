package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class DragonRitual {
    private static final String DATA_KEY = "mite_dragon_ritual";
    private static final int INTRO_TICKS = 120;
    private static final int FLIGHT_START = 80;
    private static final int ARENA_RADIUS = 128;
    private static final double BASE_HEALTH = 950;
    private static final int RETRY_GRACE = 100;
    private static final int VICTORY_XP = 1200; // The ten R195 drops of 100 and the final 200.
    private static final ChunkPos ARENA_CHUNK = new ChunkPos(BlockPos.ZERO);
    private static final TicketType<Unit> ARENA_TICKET = TicketType.create("mite_dragon_ritual", Comparator.<Unit>naturalOrder());
    private static final int ARENA_TICKET_RADIUS = 5;
    private static final Map<ServerPlayer, Long> confirmations = new WeakHashMap<>();

    private DragonRitual() {}

    public static boolean managed(ServerLevel level) {
        return level.dimension() == Level.END && WorldContentMode.rebuiltBosses(level);
    }

    @SubscribeEvent
    public static void allowOnlyRitualDragon(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !managed(level)) return;
        if (event.getEntity() instanceof EnderDragon dragon && !owns(level, dragon)
                || event.loadedFromDisk() && event.getEntity() instanceof AreaEffectCloud cloud
                    && DragonBreathAttack.ritualCloud(cloud)) {
            event.setCanceled(true);
        }
    }

    static boolean owns(ServerLevel level, EnderDragon dragon) {
        return managed(level) && storedRitualDragon(level, dragon);
    }

    static boolean storedRitualDragon(ServerLevel level, EnderDragon dragon) {
        return level.dimension() == Level.END && dragon.getUUID().equals(state(level).dragon);
    }

    static BlockPos altar(ServerLevel level) {
        return state(level).altar;
    }

    @SubscribeEvent
    public static void useAltar(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var pos = event.getPos();
        if (!WorldContentMode.rebuiltBosses(level) || level.dimension() != Level.END || event.getHand() != InteractionHand.MAIN_HAND
                || pos.getX() != 0 || pos.getZ() != 0 || !ritualEye(level, event.getItemStack())
                || !level.getBlockState(pos).is(Blocks.BEDROCK)) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || !player.isAlive()
                || !(level instanceof ServerLevel end) || end.getDragonFight() == null) return;

        var state = state(end);
        if (!pos.equals(state.altar) || state.dragon != null || !level.mayInteract(player, pos)
                || player.distanceToSqr(Vec3.atCenterOf(pos)) > 36) return;
        if (!freeAbove(end, pos)) {
            confirmations.remove(player);
            player.displayClientMessage(Component.translatable("mite_reforged.dragon_ritual.blocked"), true);
            return;
        }

        long now = end.getGameTime();
        Long expires = confirmations.remove(player);
        if (expires == null || expires < now) {
            confirmations.put(player, now + 100);
            player.displayClientMessage(Component.translatable("mite_reforged.dragon_ritual.confirm"), true);
            return;
        }

        state.dragon = UUID.randomUUID();
        state.start = now;
        state.party = Math.max(1, combatants(end).size());
        state.active = false;
        state.spawned = false;
        state.missingTicks = 0;
        state.setDirty();
        if (!player.getAbilities().instabuild) event.getItemStack().shrink(1);
        player.displayClientMessage(Component.empty(), true);
        state.music.sync(end, state.dragon, musicAudience(end));
        for (ServerPlayer viewer : arenaPlayers(end)) DragonIntroPacket.send(viewer, state.dragon, now);
    }

    private static boolean ritualEye(Level level, net.minecraft.world.item.ItemStack stack) {
        return stack.is(CoreProgression.HIGH_ENERGY_EYE.get())
            || !WorldContentMode.rebuilt(level) && stack.is(net.minecraft.world.item.Items.ENDER_EYE);
    }

    private static boolean freeAbove(ServerLevel level, BlockPos altar) {
        for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-1, 1, -1), altar.offset(1, 5, 1))) {
            if (!level.isEmptyBlock(pos)) return false;
        }
        return true;
    }

    /** Returns true while the ritual, rather than the native fight loop, owns the End. */
    public static boolean beforeFightTick(EndDragonFight fight, ServerLevel level) {
        if (!managed(level)) return false;
        DragonRitualTerrain.tickArena(level);
        var state = state(level);
        long time = level.getGameTime();
        if (!state.initialized && time % 20 == 0 && !arenaPlayers(level).isEmpty()) prepare(fight, level, state);
        if (state.dragon == null) return true;
        level.getChunkSource().addRegionTicket(ARENA_TICKET, ARENA_CHUNK, ARENA_TICKET_RADIUS, Unit.INSTANCE);
        state.music.sync(level, state.dragon, musicAudience(level));

        EnderDragon dragon = level.getEntity(state.dragon) instanceof EnderDragon entity ? entity : null;
        boolean defeated = dragon != null && dragon.isDeadOrDying();
        boolean unattended = !defeated && combatants(level).isEmpty();
        if (unattended || state.active && dragon == null) {
            // MITE pauses the End's game time while no player is there.
            if (state.missingTicks++ == 0 && state.active) clearAttackEffects(level, state.dragon);
            if (state.missingTicks >= RETRY_GRACE) {
                if (dragon != null) dragon.discard();
                DragonRitualCrystals.restoreMissing(level);
                clear(level, fight, state);
                return true;
            }
            if (state.active) return true;
        } else {
            state.missingTicks = 0;
        }
        if (state.active) {
            if (!state.dragon.equals(fight.getDragonUUID())) {
                ((DragonRitualAccess) fight).mite$trackRitualDragon(state.dragon);
            }
            if (dragon.getDragonFight() == null) dragon.setDragonFight(fight);
            if (!defeated) applyHealth(dragon, state.party, false);
            return false;
        }

        long elapsed = Math.max(0, time - state.start);
        if (elapsed == FLIGHT_START) {
            for (ServerPlayer viewer : arenaPlayers(level)) DragonIntroPacket.send(viewer, state.dragon, -1);
        }
        if (elapsed > 0 && elapsed <= 70 && elapsed % 7 == 0) lightPillar(level, (int) (elapsed / 7 - 1));
        if (elapsed < FLIGHT_START) return true;

        var from = Vec3.atCenterOf(state.altar.offset(-39, 44, 0));
        var over = Vec3.atCenterOf(state.altar.above(7));
        var to = Vec3.atCenterOf(state.altar.offset(23, 8, 0));
        double progress = Math.min(1, (elapsed - FLIGHT_START) / (double) (INTRO_TICKS - FLIGHT_START));
        Vec3 position = progress < .65 ? from.lerp(over, progress / .65) : over.lerp(to, (progress - .65) / .35);
        if (dragon == null && !state.spawned) {
            dragon = create(level, fight, state, position);
        }
        if (dragon == null) {
            if (elapsed > INTRO_TICKS + 100) clear(level, fight, state);
            return true;
        }
        dragon.moveTo(position.x, position.y, position.z, 90, 0);
        dragon.setDeltaMovement(Vec3.ZERO);
        if (elapsed == FLIGHT_START + 13) state.music.duck(state.dragon, 35, 24);
        if (elapsed == FLIGHT_START + 16) {
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4, .85f);
        }
        if (elapsed < INTRO_TICKS) return true;

        dragon.setInvulnerable(false);
        dragon.setNoAi(false);
        ((DragonRitualAccess) fight).mite$trackRitualDragon(dragon.getUUID());
        state.active = true;
        state.setDirty();
        return false;
    }

    private static void prepare(EndDragonFight fight, ServerLevel level, RitualState state) {
        if (DragonArrivalReturn.ensure(level) == null) return;
        int terrainY = level.getChunk(0, 0).getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        BlockPos top = new BlockPos(0, terrainY + 1, 0);
        BlockPos altar = null;
        for (BlockPos pos = top.below(); pos.getY() >= Math.max(40, level.getMinBuildHeight()); pos = pos.below()) {
            if (level.getBlockState(pos).is(Blocks.BEDROCK) && level.getBlockState(pos.below(2)).is(Blocks.BEDROCK)) {
                altar = pos;
                break;
            }
        }
        if (altar == null) {
            BlockPos base = top.below();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-4, -1, -4), base.offset(4, 5, 4))) {
                var block = level.getBlockState(pos);
                if (!block.isAir() && !block.is(Blocks.END_STONE)) return;
            }
            var nativePortal = (SourceDragonFightAccess) fight;
            nativePortal.mite$dragonPortal(base);
            nativePortal.mite$dragonExit(false);
            altar = base.above(3);
            if (!level.getBlockState(altar).is(Blocks.BEDROCK)) return;
        }

        for (var old : level.getDragons()) old.discard();
        fight.resetSpikeCrystals();
        for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, new AABB(altar).inflate(6))) {
            crystal.setBeamTarget(null);
            crystal.setInvulnerable(false);
        }
        ((DragonRitualAccess) fight).mite$trackRitualDragon(null);
        state.altar = altar;
        state.initialized = true;
        state.setDirty();
    }

    private static EnderDragon create(ServerLevel level, EndDragonFight fight, RitualState state, Vec3 position) {
        var dragon = EntityType.ENDER_DRAGON.create(level);
        if (dragon == null) return null;
        dragon.setUUID(state.dragon);
        dragon.setDragonFight(fight);
        dragon.setFightOrigin(BlockPos.ZERO);
        dragon.moveTo(position.x, position.y, position.z, 90, 0);
        dragon.setNoAi(true);
        dragon.setInvulnerable(true);
        applyHealth(dragon, state.party, true);
        if (!level.addFreshEntity(dragon)) return null;
        state.spawned = true;
        state.setDirty();
        return dragon;
    }

    private static void applyHealth(EnderDragon dragon, int party, boolean fresh) {
        double expected = BASE_HEALTH * (1 + .35 * Math.max(0, Math.min(7, party - 1)));
        BossCombatTuning.applyHealth(dragon, expected, fresh);
    }

    static float healthScale(EnderDragon dragon) {
        double intended = dragon.getAttributeBaseValue(Attributes.MAX_HEALTH);
        // Keep the intended party durability when the native attribute reaches its ceiling.
        return intended > 0 ? (float)Math.min(1, Attributes.MAX_HEALTH.sanitizeValue(intended) / intended) : 1;
    }

    static void attackLock(ServerLevel level, UUID dragon, boolean active) {
        if (active) BossArenaPortals.hold(level, dragon, 0, 0, ARENA_RADIUS, level.getMinBuildHeight(), level.getMaxBuildHeight());
        else BossArenaPortals.release(level, dragon);
    }

    static List<ServerPlayer> arenaPlayers(ServerLevel level) {
        return level.getPlayers(player -> present(level, player));
    }

    static List<ServerPlayer> combatants(ServerLevel level) {
        return level.getPlayers(player -> present(level, player) && !player.isCreative()
            && !Incapacitation2123.incapacitated(player));
    }

    private static boolean present(ServerLevel level, ServerPlayer player) {
        return player.serverLevel() == level && player.isAlive() && !player.isSpectator() && inArena(player);
    }

    private static List<ServerPlayer> musicAudience(ServerLevel level) {
        return level.getPlayers(player -> player.isAlive() && inArena(player));
    }

    private static boolean inArena(ServerPlayer player) {
        return player.getX() * player.getX() + player.getZ() * player.getZ() <= ARENA_RADIUS * ARENA_RADIUS;
    }

    static void contribute(ServerLevel level, EnderDragon dragon, ServerPlayer player) {
        if (!owns(level, dragon) || player.isCreative() || !present(level, player)) return;
        var state = state(level);
        if (state.active && state.contributors.add(player.getUUID())) state.setDirty();
    }

    private static void lightPillar(ServerLevel level, int index) {
        var spikes = SpikeFeature.getSpikesForLevel(level);
        if (index >= spikes.size()) return;
        var spike = spikes.get(index);
        double x = spike.getCenterX() + .5;
        double y = spike.getHeight() + 1.0;
        double z = spike.getCenterZ() + .5;
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 26, 1.0, 2.4, 1.0, .12);
        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, .8f + index * .035f);
    }

    static void completed(ServerLevel level, EnderDragon dragon) {
        if (owns(level, dragon)) clear(level, level.getDragonFight(), state(level));
    }

    static boolean openVictoryPortal(ServerLevel level, EnderDragon dragon) {
        if (!owns(level, dragon) || level.getDragonFight() == null) return false;
        var state = state(level);
        var altar = state.altar;
        var fight = level.getDragonFight();
        var portal = (SourceDragonFightAccess) fight;
        portal.mite$dragonPortal(altar.below(3));
        portal.mite$dragonExit(true);
        if (!fight.hasPreviouslyKilledDragon()) level.setBlockAndUpdate(altar.above(), Blocks.DRAGON_EGG.defaultBlockState());
        if (!state.rewarded) {
            state.rewarded = true;
            state.setDirty();
            var winners = arenaPlayers(level).stream().filter(player -> !player.isCreative()
                && state.contributors.contains(player.getUUID())).sorted(Comparator.comparing(ServerPlayer::getUUID)).toList();
            for (int i = 0; i < winners.size(); i++) {
                var player = winners.get(i);
                int xp = VICTORY_XP / winners.size() + (i < VICTORY_XP % winners.size() ? 1 : 0);
                if (xp <= 0) continue;
                player.giveExperiencePoints(xp);
                player.increaseScore(xp);
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .3f, 1.2f);
            }
        }
        return true;
    }

    static void clearAttackEffects(ServerLevel level, UUID dragon) {
        BossArenaPortals.release(level, dragon);
        DragonBreathAttack.clearRitualClouds(level, dragon);
        DragonRitualTerrain.endEncounter(level, dragon);
    }

    private static void clear(ServerLevel level, EndDragonFight fight, RitualState state) {
        if (fight != null) ((DragonRitualAccess) fight).mite$trackRitualDragon(null);
        clearAttackEffects(level, state.dragon);
        state.music.stop(state.dragon);
        level.getChunkSource().removeRegionTicket(ARENA_TICKET, ARENA_CHUNK, ARENA_TICKET_RADIUS, Unit.INSTANCE);
        state.dragon = null;
        state.active = false;
        state.spawned = false;
        state.rewarded = false;
        state.contributors.clear();
        state.missingTicks = 0;
        state.setDirty();
    }

    public static void setBarVisible(ServerBossEvent bar, UUID dragon) {
        bar.setVisible(dragon != null);
    }

    private static RitualState state(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RitualState::load, RitualState::new, DATA_KEY);
    }

    private static final class RitualState extends SavedData {
        private BlockPos altar;
        private boolean initialized, active, spawned;
        private boolean rewarded;
        private final Set<UUID> contributors = new HashSet<>();
        private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.ENDER_DRAGON);
        private UUID dragon;
        private long start;
        private int party;
        private int missingTicks;

        private static RitualState load(CompoundTag tag) {
            var state = new RitualState();
            if (tag.contains("Altar", Tag.TAG_LONG)) state.altar = BlockPos.of(tag.getLong("Altar"));
            state.initialized = state.altar != null;
            if (state.initialized && tag.hasUUID("Dragon")) {
                state.dragon = tag.getUUID("Dragon");
                state.start = tag.getLong("Start");
                state.party = Math.max(1, tag.getInt("Party"));
                state.active = tag.getBoolean("Active");
                state.spawned = tag.getBoolean("Spawned");
                state.rewarded = tag.getBoolean("Rewarded");
                for (var raw : tag.getList("Contributors", Tag.TAG_COMPOUND)) {
                    var entry = (CompoundTag) raw;
                    if (entry.hasUUID("Player")) state.contributors.add(entry.getUUID("Player"));
                }
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            if (altar != null) tag.putLong("Altar", altar.asLong());
            if (dragon != null) {
                tag.putUUID("Dragon", dragon);
                tag.putLong("Start", start);
                tag.putInt("Party", party);
                tag.putBoolean("Active", active);
                tag.putBoolean("Spawned", spawned);
                tag.putBoolean("Rewarded", rewarded);
                if (!contributors.isEmpty()) {
                    var list = new ListTag();
                    for (UUID id : contributors.stream().sorted().toList()) {
                        var entry = new CompoundTag();
                        entry.putUUID("Player", id);
                        list.add(entry);
                    }
                    tag.put("Contributors", list);
                }
            }
            return tag;
        }
    }
}
