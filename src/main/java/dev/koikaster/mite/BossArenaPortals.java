package dev.koikaster.mite;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Transient server-side entrance locks owned by active boss attacks. */
public final class BossArenaPortals {
    private record Lock(AABB bounds, double radiusSquared, double halfHeight,
                        WeakReference<LivingEntity> follower, long through) {
        boolean expired(ServerLevel level, long now) {
            if (through < now) return true;
            if (follower == null) return false;
            LivingEntity boss = follower.get();
            return boss == null || boss.level() != level || !boss.isAlive();
        }

        boolean covers(BlockPos entrance) {
            Vec3 at = Vec3.atCenterOf(entrance);
            if (follower != null) {
                LivingEntity boss = follower.get();
                return boss != null && withinCylinder(boss.position(), at, radiusSquared, halfHeight);
            }
            if (!bounds.contains(at)) return false;
            if (radiusSquared == 0) return true;
            double dx = at.x - (bounds.minX + bounds.maxX) * .5;
            double dz = at.z - (bounds.minZ + bounds.maxZ) * .5;
            return dx * dx + dz * dz <= radiusSquared;
        }
    }

    private static final Map<ServerLevel, Map<UUID, Lock>> LOCKS = new WeakHashMap<>();

    private BossArenaPortals() {}

    static void hold(ServerLevel level, UUID boss, double x, double z, double radius, double bottom, double top) {
        hold(level, boss, new AABB(x - radius, bottom, z - radius, x + radius, top, z + radius), radius * radius);
    }

    static void hold(ServerLevel level, UUID boss, AABB bounds) {
        hold(level, boss, bounds, 0);
    }

    static boolean withinCylinder(Vec3 center, Vec3 at, double radiusSquared, double halfHeight) {
        double dx = at.x - center.x;
        double dz = at.z - center.z;
        return dx * dx + dz * dz <= radiusSquared && Math.abs(at.y - center.y) <= halfHeight;
    }

    static void holdFollowing(ServerLevel level, LivingEntity boss, double radius, double halfHeight) {
        long now = level.getGameTime();
        var active = activeLocks(level, now);
        active.put(boss.getUUID(), new Lock(null, radius * radius, halfHeight, new WeakReference<>(boss), now + 1));
        cancelLockedRoutes(level);
    }

    private static void hold(ServerLevel level, UUID boss, AABB bounds, double radiusSquared) {
        long now = level.getGameTime();
        // Keep the preceding tick valid when a portal user ticks before the boss.
        activeLocks(level, now).put(boss, new Lock(bounds, radiusSquared, 0, null, now + 1));
        cancelLockedRoutes(level);
    }

    private static void cancelLockedRoutes(ServerLevel level) {
        SourcePortalTravel.cancelLocked(level);
        SourcePortalTransition.cancelLocked(level);
        SourceBiomePortal.cancelLocked(level);
        SkyEndPortal.cancelLocked(level);
        DragonArrivalReturn.cancelLocked(level);
    }

    private static Map<UUID, Lock> activeLocks(ServerLevel level, long now) {
        var active = LOCKS.computeIfAbsent(level, ignored -> new HashMap<>());
        active.values().removeIf(lock -> lock.expired(level, now));
        return active;
    }

    static void release(ServerLevel level, UUID boss) {
        var active = LOCKS.get(level);
        if (active == null) return;
        active.remove(boss);
        if (active.isEmpty()) LOCKS.remove(level);
    }

    private static Map<UUID, Lock> currentLocks(Level level) {
        if (!(level instanceof ServerLevel server)) return Map.of();
        var active = LOCKS.get(server);
        if (active == null) return Map.of();
        long now = server.getGameTime();
        active.values().removeIf(lock -> lock.expired(server, now));
        if (active.isEmpty()) LOCKS.remove(server);
        return active;
    }

    static boolean hasActiveLocks(Level level) {
        return !currentLocks(level).isEmpty();
    }

    public static boolean blocked(Level level, BlockPos entrance) {
        return entrance != null && currentLocks(level).values().stream().anyMatch(lock -> lock.covers(entrance));
    }

    static boolean blockedNetherEntrance(Level level, BlockPos entrance) {
        if (entrance == null || !hasActiveLocks(level)) return false;
        if (blocked(level, entrance)) return true;
        return level.hasChunkAt(entrance) && SourcePortalFrames.complete(level, entrance)
            .map(frame -> blockedPortal(level, frame.rectangle(), frame.axis())).orElse(false);
    }

    static boolean blockedPortal(Level level, BlockUtil.FoundRectangle portal, Direction.Axis axis) {
        var locks = currentLocks(level).values();
        return !locks.isEmpty() && anyPortalCell(portal, axis, cell -> locks.stream().anyMatch(lock -> lock.covers(cell)));
    }

    static boolean anyPortalCell(BlockUtil.FoundRectangle portal, Direction.Axis axis, Predicate<BlockPos> check) {
        for (int across = 0; across < portal.axis1Size; ++across) {
            for (int up = 0; up < portal.axis2Size; ++up) {
                BlockPos cell = axis == Direction.Axis.X ? portal.minCorner.offset(across, up, 0)
                    : portal.minCorner.offset(0, up, across);
                if (check.test(cell)) return true;
            }
        }
        return false;
    }
}
