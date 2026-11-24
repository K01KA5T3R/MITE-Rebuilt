package dev.koikaster.mite;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Phase-three boundary, local to the ritual arena and reconstructed safely after a reload. */
final class DragonArenaPressure {
    private static final String SAVED_RING = "MiteRitualSafeRing";
    private static final double[] RADII = {58, 44, 32, 24};
    private static final int WARNING_TICKS = 66;
    private static final int BETWEEN_RINGS = 300;
    private static final int MIN_RETURN_TICKS = 52;
    private static final int PULSE_INTERVAL = 32;
    private static final int ROUTE_BUDGET = 640;
    private static final SimpleParticleType WARNING = ParticleTypes.GLOW;
    private static final SimpleParticleType ACTIVE = ParticleTypes.DRAGON_BREATH;
    private static final Direction[] CARDINAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final int[] STEPS = {0, 1, -1};

    private final Map<UUID, Long> outsideSince = new HashMap<>();
    private Vec3 center;
    private int ring = -1;
    private int restore = -1;
    private int pending = -1;
    private int warning;
    private int rest;
    private long lastPulse = Long.MIN_VALUE;

    /** True while a pending boundary owns the quiet interval between the dragon's attacks. */
    boolean tick(ServerLevel level, List<ServerPlayer> players, boolean idle, boolean cloudFading, boolean cloudActive) {
        if (center == null) {
            BlockPos altar = DragonRitual.altar(level);
            if (altar == null) return false;
            center = Vec3.atBottomCenterOf(altar);
        }

        if (pending < 0 && idle && !cloudFading && wantsWarning()) {
            pending = restore >= 0 ? restore : ring + 1;
            warning = WARNING_TICKS;
            for (var player : players) {
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, .8f, 1.35f);
            }
        }
        if (pending >= 0) {
            if (warning % 6 == 0) mark(level, players, RADII[pending], true);
            if (--warning == 0) {
                ring = pending;
                pending = restore = -1;
                rest = BETWEEN_RINGS;
                outsideSince.clear();
                mark(level, players, RADII[ring], false);
            }
            return true;
        }

        if (rest > 0) --rest;
        if (ring >= 0) {
            if (level.getGameTime() % 7 == 0) mark(level, players, RADII[ring], false);
            if (cloudActive) outsideSince.clear();
            else pressure(level, players);
        }
        return false;
    }

    boolean attacking(long now) {
        return pending >= 0 || lastPulse == now;
    }

    private boolean wantsWarning() {
        return rest == 0 && ring < RADII.length - 1;
    }

    boolean boundaryDue() { return pending >= 0 || wantsWarning(); }

    boolean allows(Vec3 at, double margin) {
        return ring < 0 || center == null || inside(at, RADII[ring] - margin);
    }

    void suspend() {
        pending = -1;
        warning = 0;
        lastPulse = Long.MIN_VALUE;
        outsideSince.clear();
    }

    void save(CompoundTag tag) {
        int committed = restore >= 0 ? restore : ring;
        if (committed >= 0) tag.putInt(SAVED_RING, committed);
    }

    void load(CompoundTag tag) {
        suspend();
        ring = -1;
        rest = 0;
        restore = tag.contains(SAVED_RING) ? Mth.clamp(tag.getInt(SAVED_RING), 0, RADII.length - 1) : -1;
    }

    private void pressure(ServerLevel level, List<ServerPlayer> players) {
        outsideSince.keySet().removeIf(id -> !(level.getPlayerByUUID(id) instanceof ServerPlayer player)
            || !players.contains(player) || allows(player.position(), 0));
        long now = level.getGameTime();
        for (var player : players) {
            if (allows(player.position(), 0)) continue;
            long age = now - outsideSince.computeIfAbsent(player.getUUID(), id -> now);
            if (age < MIN_RETURN_TICKS || Math.floorMod(now - player.getId(), PULSE_INTERVAL) != 0) continue;
            int route = routeLength(level, player, RADII[ring] - .6);
            if (route >= 0 && age >= route * 7L) {
                lastPulse = now;
                player.hurt(level.damageSources().magic(), BossCombatTuning.playerDamage(4));
            }
        }
    }

    private boolean inside(Vec3 at, double radius) {
        return at.subtract(center).horizontalDistanceSqr() <= radius * radius;
    }

    private int routeLength(ServerLevel level, ServerPlayer player, double radius) {
        var relativeBox = player.getBoundingBox().move(-player.getX(), -player.getY(), -player.getZ()).deflate(.012);
        BlockPos start = player.blockPosition();
        for (int i = 0; i < 2 && !standing(level, player, relativeBox, start); i++) start = start.below();
        if (!standing(level, player, relativeBox, start)) return -1;

        var queue = new PriorityQueue<>(Comparator.comparingDouble(RouteNode::score));
        var seen = new HashSet<BlockPos>();
        queue.add(new RouteNode(start, 0, 0));
        seen.add(start);
        for (int count = 0; count < ROUTE_BUDGET && !queue.isEmpty(); count++) {
            var current = queue.remove();
            Vec3 from = Vec3.atBottomCenterOf(current.pos);
            if (inside(from, radius)) return current.length;
            for (var direction : CARDINAL) {
                for (int rise : STEPS) {
                    BlockPos next = current.pos.relative(direction).offset(0, rise, 0);
                    if (seen.contains(next) || !standing(level, player, relativeBox, next)) continue;
                    Vec3 to = Vec3.atBottomCenterOf(next);
                    double y = Math.max(from.y, to.y);
                    if (!clear(level, player, relativeBox, new Vec3(from.x, y, from.z))
                            || !clear(level, player, relativeBox, from.lerp(to, .5).add(0, Math.abs(rise) * .5, 0))
                            || !clear(level, player, relativeBox, new Vec3(to.x, y, to.z))) continue;
                    int length = current.length + 1;
                    double remaining = Math.sqrt(to.subtract(center).horizontalDistanceSqr()) - radius;
                    queue.add(new RouteNode(next, length, length + remaining * 1.75));
                    seen.add(next);
                }
            }
        }
        return -1;
    }

    private static boolean standing(ServerLevel level, ServerPlayer player, AABB box, BlockPos feet) {
        return level.hasChunkAt(feet) && level.getFluidState(feet).isEmpty()
            && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
            && clear(level, player, box, Vec3.atBottomCenterOf(feet));
    }

    private static boolean clear(ServerLevel level, ServerPlayer player, AABB box, Vec3 feet) {
        return !level.getBlockCollisions(player, box.move(feet)).iterator().hasNext();
    }

    private void mark(ServerLevel level, List<ServerPlayer> players, double radius, boolean preview) {
        ParticleOptions particle = preview ? WARNING : ACTIVE;
        ParticleOptions marker = preview ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.REVERSE_PORTAL;
        for (var player : players) {
            Vec3 offset = player.position().subtract(center);
            double distance = offset.horizontalDistance();
            if (distance < radius - 25) continue;
            if (distance > radius + 26) {
                Vec3 inward = offset.multiply(-1, 0, -1).normalize();
                for (int i = 2; i <= 5; i++) {
                    Vec3 point = player.position().add(inward.scale(i * 2)).add(0, .55, 0);
                    particle(level, player, point, particle);
                    if (i % 2 == 0) particle(level, player, point.add(0, .8, 0), marker);
                }
                continue;
            }
            double angle = Math.atan2(offset.z, offset.x);
            for (int i = -16; i <= 16; i++) {
                double sample = angle + i * 1.2 / radius;
                double x = center.x + Math.cos(sample) * radius;
                double z = center.z + Math.sin(sample) * radius;
                BlockPos column = BlockPos.containing(x, player.getY(), z);
                if (!level.hasChunkAt(column)) continue;
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                double floor = Math.abs(surface - player.getY()) <= 6 ? surface : player.getY();
                particle(level, player, new Vec3(x, floor + .22, z), particle);
                if (i % 2 == 0) {
                    particle(level, player, new Vec3(x, floor + .95, z), particle);
                    particle(level, player, new Vec3(x, floor + 1.65, z), marker);
                }
            }
        }
    }

    private static void particle(ServerLevel level, ServerPlayer player, Vec3 point, ParticleOptions particle) {
        level.sendParticles(player, particle, true, point.x, point.y, point.z, 1, 0, 0, 0, 0);
    }

    private record RouteNode(BlockPos pos, int length, double score) {}
}
