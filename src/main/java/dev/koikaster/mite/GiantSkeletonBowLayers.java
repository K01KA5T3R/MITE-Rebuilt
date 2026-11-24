package dev.koikaster.mite;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Two separately supported lanes for the giant's alternating bow shots. */
record GiantSkeletonBowLayers(GiantSkeletonBowLayers.Lane lower, GiantSkeletonBowLayers.Lane upper) {
    private record Step(BlockPos feet, int distance) {}

    record Lane(UUID player, Vec3 feet) {
        GiantArrowTrajectory.Flight flight(ServerLevel level, SourceGiantSkeleton boss,
                                           List<ServerPlayer> fighters, boolean lower) {
            for (var fighter : fighters) {
                if (!player.equals(fighter.getUUID())) continue;
                if (!standable(level, fighter, feet)) return null;
                var offset = feet.subtract(boss.position());
                double distance = offset.horizontalDistance();
                if (distance < 5 || distance > 14 || Math.abs(offset.y) > 6) return null;
                var direction = BossAttackGeometry.flatDirection(offset);
                Vec3 start = lower
                    ? boss.position().add(direction.scale(.95)).add(0, 1.15, 0)
                    : boss.getEyePosition().add(0, -.1, 0);
                return GiantArrowTrajectory.layer(level, boss, start, feet.add(0, lower ? .6 : 1.05, 0));
            }
            return null;
        }
    }

    static GiantSkeletonBowLayers find(ServerLevel level, SourceGiantSkeleton boss, List<ServerPlayer> fighters) {
        for (var first : fighters) {
            if (!first.onGround() || !clearSide(level, first, boss, first.position())) continue;
            for (var second : fighters) {
                if (first == second || !second.onGround() || Math.abs(first.getY() - second.getY()) < 2
                        || !clearSide(level, second, boss, second.position())) continue;
                var pair = plan(level, boss, fighters, new Lane(first.getUUID(), first.position()),
                    new Lane(second.getUUID(), second.position()));
                if (pair != null) return pair;
            }
        }
        if (fighters.size() != 1) return null;

        var player = fighters.get(0);
        if (!player.onGround() || !clearSide(level, player, boss, player.position())) return null;
        return findNearbyLayer(level, boss, player, fighters);
    }

    private static GiantSkeletonBowLayers findNearbyLayer(ServerLevel level, SourceGiantSkeleton boss,
                                                         ServerPlayer player, List<ServerPlayer> fighters) {
        var start = player.blockPosition();
        if (!standable(level, player, Vec3.atBottomCenterOf(start))) return null;
        var seen = new HashSet<BlockPos>();
        var steps = new ArrayDeque<Step>();
        steps.add(new Step(start, 0));
        seen.add(start);
        var current = new Lane(player.getUUID(), player.position());
        while (!steps.isEmpty()) {
            var step = steps.removeFirst();
            Vec3 feet = Vec3.atBottomCenterOf(step.feet());
            if (Math.abs(feet.y - current.feet().y) >= 2 && clearSide(level, player, boss, feet)) {
                var pair = plan(level, boss, fighters, current, new Lane(player.getUUID(), feet));
                if (pair != null) return pair;
            }
            if (step.distance() >= 5) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int rise = -1; rise <= 1; ++rise) {
                    var next = step.feet().relative(direction).offset(0, rise, 0);
                    if (seen.contains(next)) continue;
                    Vec3 destination = Vec3.atBottomCenterOf(next);
                    if (!standable(level, player, destination) || !canStep(level, player, feet, destination)) continue;
                    seen.add(next);
                    steps.addLast(new Step(next, step.distance() + 1));
                }
            }
        }
        return null;
    }

    private static GiantSkeletonBowLayers plan(ServerLevel level, SourceGiantSkeleton boss, List<ServerPlayer> fighters,
                                             Lane first, Lane second) {
        if (first.feet().subtract(second.feet()).horizontalDistanceSqr() > 8 * 8) return null;
        Lane lower = first.feet().y < second.feet().y ? first : second;
        Lane upper = lower == first ? second : first;
        var firstFlight = lower.flight(level, boss, fighters, true);
        var secondFlight = upper.flight(level, boss, fighters, false);
        if (firstFlight == null || secondFlight == null) return null;
        return new GiantSkeletonBowLayers(lower, upper);
    }

    private static boolean canStep(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to) {
        double topY = Math.max(from.y, to.y);
        return clearBody(level, player, new Vec3(from.x, topY, from.z))
            && clearBody(level, player, new Vec3(to.x, topY, to.z))
            && clearBody(level, player, new Vec3((from.x + to.x) * .5, topY, (from.z + to.z) * .5));
    }

    private static boolean clearSide(ServerLevel level, ServerPlayer player, SourceGiantSkeleton boss, Vec3 feet) {
        var toward = BossAttackGeometry.flatDirection(feet.subtract(boss.position()));
        var across = new Vec3(-toward.z, 0, toward.x).scale(1.1);
        return flatStep(level, player, feet, feet.add(across)) || flatStep(level, player, feet, feet.subtract(across));
    }

    private static boolean flatStep(ServerLevel level, ServerPlayer player, Vec3 start, Vec3 end) {
        return standable(level, player, start.lerp(end, .5)) && standable(level, player, end);
    }

    private static boolean standable(ServerLevel level, ServerPlayer player, Vec3 feet) {
        BlockPos below = BlockPos.containing(feet.add(0, -.01, 0));
        return level.hasChunkAt(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
            && clearBody(level, player, feet);
    }

    private static boolean clearBody(ServerLevel level, ServerPlayer player, Vec3 feet) {
        double half = Math.max(.3, player.getBbWidth() * .5);
        double height = Math.max(1.8, player.getBbHeight());
        AABB box = new AABB(feet.x - half, feet.y, feet.z - half, feet.x + half, feet.y + height, feet.z + half);
        return level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))
            && box.minX >= level.getWorldBorder().getMinX() && box.maxX <= level.getWorldBorder().getMaxX()
            && box.minZ >= level.getWorldBorder().getMinZ() && box.maxZ <= level.getWorldBorder().getMaxZ()
            && !level.getBlockCollisions(player, box).iterator().hasNext() && !level.containsAnyLiquid(box);
    }
}
