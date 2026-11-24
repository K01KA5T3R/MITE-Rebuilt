package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern.BlockPatternMatch;
import net.minecraft.world.phys.Vec3;

/** Attendance and the opening warning for Withers summoned by the native skull pattern. */
public final class WitherRitual {
    static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> TROPHY =
        BossTrophies.register("wither_trophy", Blocks.POLISHED_BLACKSTONE);

    static void register() {}
    static final float BLAST_POWER = 7;
    static final int WARNING_TICKS = 64;
    private static final double MAX_HEALTH = 500;
    private static final int WARNING_PERIOD = 8;
    private static final int WARNING_POINTS = 48;
    private static final int ARENA_RADIUS = 48;
    private static final int ARENA_HEIGHT = 32;
    private static final int RETURN_GRACE = 100;
    private static final int TICKET_DISTANCE = 2;
    private static final TicketType<UUID> ENCOUNTER_TICKET = TicketType.create("mite_wither_encounter", Comparator.<UUID>naturalOrder());
    private static final SimpleParticleType RIM = ParticleTypes.SOUL_FIRE_FLAME;
    private static final SimpleParticleType INSIDE = ParticleTypes.SMOKE;
    private static final ThreadLocal<Set<PendingSummon>> PENDING_SUMMONS = ThreadLocal.withInitial(HashSet::new);

    private final BossMusicAudience music = new BossMusicAudience(BossMusicTrack.WITHER);
    final WitherCombat combat = new WitherCombat();
    private final Set<UUID> titleSent = new HashSet<>();
    private final BlockPos summonPosition;
    private final long startedAt;
    private ChunkPos ticketChunk;
    private long unattendedSince = -1;

    private WitherRitual(BlockPos summonPosition, long startedAt) {
        this.summonPosition = summonPosition;
        this.startedAt = startedAt;
    }

    public static boolean hasRoom(BlockPatternMatch match, Level level) {
        if (match == null) return false;
        if (!WorldContentMode.rebuiltBosses(level)) return true;
        BlockPos center = match.getBlock(1, 2, 0).getPos();
        Set<BlockPos> pattern = new HashSet<>();
        for (int column = 0; column < match.getWidth(); column++) {
            for (int row = 0; row < match.getHeight(); row++) {
                pattern.add(match.getBlock(column, row, 0).getPos());
            }
        }
        for (var pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 4, 3))) {
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) {
                return false;
            }
            if (pattern.contains(pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty()) return false;
        }
        return true;
    }

    public static boolean commitSummon(Level level, Entity entity, BlockPatternMatch match) {
        var pending = PENDING_SUMMONS.get();
        var key = new PendingSummon(level, match.getBlock(1, 2, 0).getPos());
        if (!pending.add(key)) return false;
        try {
            if (!level.addFreshEntity(entity)) return false;
            CarvedPumpkinBlock.clearPatternBlocks(level, match);
            if (WorldContentMode.rebuiltBosses(level) && level instanceof ServerLevel server && entity instanceof SourceWitherBoss wither) {
                wither.beginRitual(new WitherRitual(wither.blockPosition(), level.getGameTime()));
                wither.ritualTick(server);
            }
            return true;
        } finally {
            pending.remove(key);
            if (pending.isEmpty()) PENDING_SUMMONS.remove();
        }
    }

    private record PendingSummon(Level level, BlockPos anchor) {}

    boolean tick(ServerLevel level, SourceWitherBoss wither) {
        long now = level.getGameTime();
        // An unloaded encounter can first tick again after its original deadline.
        if (unattendedSince >= 0 && now - unattendedSince >= RETURN_GRACE) {
            wither.discard();
            return false;
        }
        holdChunk(level, wither);
        var audience = level.getPlayers(player -> player.isAlive() && inArena(wither, player.position()));
        music.sync(level, wither.getUUID(), audience);
        boolean attended = audience.stream().anyMatch(player -> !player.isSpectator() && !Incapacitation2123.incapacitated(player));
        if (!attended) {
            if (unattendedSince < 0) unattendedSince = now;
            wither.setTarget(null);
            wither.setDeltaMovement(Vec3.ZERO);
            for (int head = 0; head < 3; head++) wither.setAlternativeTarget(head, 0);
            combat.suspend(wither);
            attackLock(level, wither, false);
            return false;
        }
        unattendedSince = -1;
        long age = now - startedAt;
        if (age < WitherIntroPacket.DURATION) {
            for (var player : audience) {
                if (player.connection.isAcceptingMessages() && titleSent.add(player.getUUID())) {
                    WitherIntroPacket.send(player, wither.getUUID(), level.dimension().location(), startedAt);
                }
            }
        }
        int remaining = wither.getInvulnerableTicks();
        if (remaining > 0 && remaining <= WARNING_TICKS && remaining % WARNING_PERIOD == 0) {
            warn(level, wither, remaining);
        }
        return true;
    }

    private void holdChunk(ServerLevel level, SourceWitherBoss wither) {
        ChunkPos current = wither.chunkPosition();
        if (current.equals(ticketChunk)) return;
        level.getChunkSource().addRegionTicket(ENCOUNTER_TICKET, current, TICKET_DISTANCE, wither.getUUID());
        if (ticketChunk != null) {
            level.getChunkSource().removeRegionTicket(ENCOUNTER_TICKET, ticketChunk, TICKET_DISTANCE, wither.getUUID());
        }
        ticketChunk = current;
    }

    void stop(SourceWitherBoss wither) {
        stop(wither, false);
    }

    void stop(SourceWitherBoss wither, boolean keepStagger) {
        music.stop(wither.getUUID());
        combat.suspend(wither, keepStagger);
        if (wither.level() instanceof ServerLevel level) {
            attackLock(level, wither, false);
            if (ticketChunk != null) {
                level.getChunkSource().removeRegionTicket(ENCOUNTER_TICKET, ticketChunk, TICKET_DISTANCE, wither.getUUID());
                ticketChunk = null;
            }
        }
    }

    void applyHealth(SourceWitherBoss wither) {
        BossCombatTuning.applyHealth(wither, MAX_HEALTH, false);
    }

    void attackLock(ServerLevel level, SourceWitherBoss wither, boolean active) {
        if (active) BossArenaPortals.holdFollowing(level, wither, ARENA_RADIUS, ARENA_HEIGHT);
        else BossArenaPortals.release(level, wither.getUUID());
    }

    void headBroke(SourceWitherBoss wither) { music.duck(wither.getUUID(), 45, 20); }

    List<ServerPlayer> combatants(ServerLevel level, SourceWitherBoss wither) {
        return level.getPlayers(player -> player.isAlive() && !player.isCreative() && !player.isSpectator()
            && !Incapacitation2123.incapacitated(player) && inArena(wither, player.position()));
    }

    boolean inArena(SourceWitherBoss wither, Vec3 position) {
        return BossArenaPortals.withinCylinder(wither.position(), position, ARENA_RADIUS * ARENA_RADIUS, ARENA_HEIGHT);
    }

    private static void warn(ServerLevel level, SourceWitherBoss wither, int remaining) {
        double radius = SourceExplosionKernel.livingRadius(BLAST_POWER);
        double advance = 1 - (double) remaining / WARNING_TICKS;
        int ceiling = Mth.floor(wither.getY() + wither.getEyeHeight() + 2);
        for (int point = 0; point < WARNING_POINTS; point++) {
            double angle = (point + (remaining / WARNING_PERIOD % 2) * .5) * Math.PI * 2 / WARNING_POINTS;
            mark(level, wither, angle, radius, ceiling, RIM);
            if (point % 3 == 0) mark(level, wither, angle, radius * (.28 + .60 * advance), ceiling, INSIDE);
        }
    }

    private static void mark(ServerLevel level, SourceWitherBoss wither, double angle, double distance,
                             int ceiling, ParticleOptions particle) {
        double x = wither.getX() + Math.cos(angle) * distance;
        double z = wither.getZ() + Math.sin(angle) * distance;
        BlockPos.MutableBlockPos sample = new BlockPos.MutableBlockPos(Mth.floor(x), ceiling, Mth.floor(z));
        if (!level.hasChunkAt(sample)) return;
        for (int y = ceiling; y >= Math.max(level.getMinBuildHeight(), ceiling - 13); y--) {
            var shape = level.getBlockState(sample.setY(y)).getCollisionShape(level, sample);
            if (shape.isEmpty()) continue;
            level.sendParticles(particle, x, y + shape.max(Direction.Axis.Y) + .08, z, 1, 0, 0, 0, 0);
            return;
        }
    }

    void save(CompoundTag tag) {
        tag.putLong("MiteWitherRitualCenter", summonPosition.asLong());
        tag.putLong("MiteWitherRitualStartedAt", startedAt);
        if (unattendedSince >= 0) tag.putLong("MiteWitherRitualUnattended", unattendedSince);
        combat.save(tag);
    }

    static WitherRitual load(CompoundTag tag) {
        if (!tag.contains("MiteWitherRitualCenter") || !tag.contains("MiteWitherRitualStartedAt")) return null;
        var ritual = new WitherRitual(BlockPos.of(tag.getLong("MiteWitherRitualCenter")), tag.getLong("MiteWitherRitualStartedAt"));
        if (tag.contains("MiteWitherRitualUnattended")) ritual.unattendedSince = tag.getLong("MiteWitherRitualUnattended");
        ritual.combat.load(tag);
        return ritual;
    }
}
