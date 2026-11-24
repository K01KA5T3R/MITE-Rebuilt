package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Selects a short opening formation for the current living wave. */
final class GiantSkeletonTactics {
    private static final int PREPARE_TICKS = 64;
    private static final int ARRIVAL_WINDOW = 32;
    private static final int MAX_REACH = 10;
    private static final double HALF_DIAGONAL = 1.0 / Math.sqrt(2.0);

    enum Formation { FRONT_AND_BOW, PINCER, HIGH_BOW, PILLAR }

    record Position(PathfinderMob mob, Path path) {}

    private record Opening(Formation formation, Position first, Position second) {}

    private GiantSkeletonApproach first;
    private GiantSkeletonApproach second;
    private Formation formation;
    private int consideredWave;
    private int openingWave;
    private long openingStarted;

    boolean awaitingOpening(int wave, long now, int members, boolean reservesPending) {
        if (openingWave != wave) {
            openingWave = wave;
            openingStarted = now;
        }
        long elapsed = now - openingStarted;
        if (consideredWave == wave || members < 2 || elapsed < 4 || elapsed >= ARRIVAL_WINDOW) return false;
        return !reservesPending || elapsed >= 24;
    }

    void tick(SourceGiantSkeleton boss, ServerLevel level, ServerPlayer target, List<PathfinderMob> members,
              int wave, long now) {
        if (first != null && first.done()) {
            first.release();
            first = null;
            if (formation == Formation.FRONT_AND_BOW) releaseSecond();
        }
        if (second != null && second.done()) releaseSecond();
        if (members.size() < 2) return;
        consideredWave = wave;
        Opening chosen = choose(level, target, members);
        if (chosen == null) return;
        clear();
        formation = chosen.formation();
        first = install(boss, target, chosen.first(), now);
        if (first != null) second = install(boss, target, chosen.second(), now);
        if (first == null || chosen.second() != null && second == null) clear();
    }

    long waveAge(long now) {
        return now - openingStarted;
    }

    boolean active() {
        return first != null || second != null;
    }

    void clear() {
        if (first != null) {
            first.release();
            first = null;
        }
        releaseSecond();
        formation = null;
    }

    private void releaseSecond() {
        if (second != null) {
            second.release();
            second = null;
        }
    }

    private static GiantSkeletonApproach install(SourceGiantSkeleton boss, ServerPlayer target, Position position,
                                                 long now) {
        if (position == null) return null;
        position.mob().setTarget(target);
        if (position.mob().getTarget() != target) return null;
        var goal = new GiantSkeletonApproach(boss, position.mob(), target, position.path(), now + PREPARE_TICKS);
        goal.install();
        return goal;
    }

    private static Opening choose(ServerLevel level, ServerPlayer target, List<PathfinderMob> members) {
        var available = members.stream().filter(mob -> mob.onGround() && !mob.isInWaterOrBubble()
            && !mob.isNoAi() && mob.distanceToSqr(target) >= 9 && mob.distanceToSqr(target) <= 144).toList();
        var melee = available.stream().filter(mob -> !bow(mob)).limit(3).toList();
        var archers = available.stream().filter(GiantSkeletonTactics::bow).limit(2).toList();

        var lanes = lanes(level, target, melee, true);
        if (lanes != null) return lanes;
        if (!melee.isEmpty()) {
            for (var archer : archers) {
                var high = highBow(level, target, archer);
                if (high != null) return new Opening(Formation.HIGH_BOW, high, null);
            }
            for (var archer : archers) {
                for (var fighter : melee) {
                    var front = frontAndBow(level, target, fighter, archer);
                    if (front != null) return front;
                }
            }
        }
        return lanes(level, target, melee, false);
    }

    static boolean bow(Mob mob) {
        return mob instanceof RangedAttackMob && mob.getMainHandItem().getItem() instanceof BowItem;
    }

    private static Opening frontAndBow(ServerLevel level, ServerPlayer target, PathfinderMob fighter,
                                       PathfinderMob archer) {
        double frontDistance = fighter.distanceTo(target);
        double rearDistance = archer.distanceTo(target);
        Vec3 frontLine = directionFrom(target, fighter);
        Vec3 rearLine = directionFrom(target, archer);
        if (rearDistance <= frontDistance + 1.5 || frontLine.dot(rearLine) < .5
                || Math.abs(fighter.getY() - archer.getY()) >= 1.5 || !archer.hasLineOfSight(target)) return null;
        var at = target.position().add(frontLine.scale(Math.max(2.8, frontDistance - 2)));
        var left = at.add(sideOf(frontLine).scale(2));
        var right = at.subtract(sideOf(frontLine).scale(2));
        if (!(standable(level, fighter, BlockPos.containing(left))
                || standable(level, fighter, BlockPos.containing(right)))) return null;
        var forward = route(level, fighter, BlockPos.containing(at));
        if (forward == null) return null;
        return new Opening(Formation.FRONT_AND_BOW, forward, new Position(archer, null));
    }

    private static Position highBow(ServerLevel level, ServerPlayer target, PathfinderMob archer) {
        if (archer.hasLineOfSight(target)) return null;
        var toward = directionFrom(archer, target);
        var across = sideOf(toward);
        for (int side = -1; side <= 1; side += 2) {
            for (int dy = 1; dy <= 2; dy++) {
                var point = archer.position().add(toward.scale(1.25)).add(across.scale(side * 2.5));
                var at = BlockPos.containing(point.x, archer.getBlockY() + dy, point.z);
                if (at.getY() <= target.getY()) continue;
                var eye = Vec3.atBottomCenterOf(at).add(0, archer.getEyeHeight(), 0);
                if (!BossAttackGeometry.visible(level, archer, eye, target.getEyePosition())) continue;
                var route = route(level, archer, at);
                if (route != null) return route;
            }
        }
        return null;
    }

    private static Opening lanes(ServerLevel level, ServerPlayer target, List<PathfinderMob> melee, boolean pillar) {
        for (int i = 0; i + 1 < melee.size(); i++) {
            var first = melee.get(i);
            for (int j = i + 1; j < melee.size(); j++) {
                var second = melee.get(j);
                var average = first.position().lerp(second.position(), .5);
                var front = BossAttackGeometry.flatDirection(average.subtract(target.position()));
                if (front.lengthSqr() < .5 || Math.abs(average.y - target.getY()) > 2
                        || first.position().distanceToSqr(second.position()) < 4) continue;
                var side = sideOf(front);
                if (first.position().subtract(second.position()).dot(side) < 0) side = side.scale(-1);
                var center = target.position().add(front.scale(pillar ? 3.5 : 2.6));
                var firstLane = center.add(side.scale(pillar ? 3.4 : 2.9));
                var secondLane = center.subtract(side.scale(pillar ? 3.4 : 2.9));
                Vec3 eye = target.getEyePosition();
                Vec3 middleLane = eye.add(front.scale(5));
                if (!level.hasChunkAt(BlockPos.containing(middleLane))) continue;
                boolean blockedCenter = !BossAttackGeometry.visible(level, target, eye, middleLane);
                if (pillar != blockedCenter) continue;
                if (!visibleLane(level, target, firstLane) || !visibleLane(level, target, secondLane)) continue;
                var left = route(level, first, BlockPos.containing(firstLane));
                var right = route(level, second, BlockPos.containing(secondLane));
                if (left == null || right == null || !distinct(left.path(), right.path(), center, side)) continue;
                return new Opening(pillar ? Formation.PILLAR : Formation.PINCER, left, right);
            }
        }
        return null;
    }

    private static boolean visibleLane(ServerLevel level, ServerPlayer target, Vec3 position) {
        return level.hasChunkAt(BlockPos.containing(position))
            && BossAttackGeometry.visible(level, target, target.getEyePosition(), position.add(0, 1.3, 0));
    }

    private static boolean distinct(Path left, Path right, Vec3 center, Vec3 side) {
        if (left.getNodeCount() < 2 || right.getNodeCount() < 2
                || left.getTarget().distSqr(right.getTarget()) < 9) return false;
        for (int i = 1; i < left.getNodeCount(); i++) {
            var a = left.getNode(i).asBlockPos();
            for (int j = 1; j < right.getNodeCount(); j++) {
                if (a.equals(right.getNode(j).asBlockPos())) return false;
            }
        }
        return crossesSide(left, center, side) && crossesSide(right, center, side.scale(-1));
    }

    private static boolean crossesSide(Path path, Vec3 center, Vec3 side) {
        var previous = Vec3.atBottomCenterOf(path.getNode(path.getNodeCount() - 2).asBlockPos());
        return previous.subtract(center).dot(side) > HALF_DIAGONAL;
    }

    static Position route(ServerLevel level, PathfinderMob mob, BlockPos at) {
        if (!standable(level, mob, at) || mob.position().distanceToSqr(Vec3.atBottomCenterOf(at)) < 1.0) return null;
        var path = SourceArachnidPathBudget.block(mob, at, MAX_REACH);
        if (path == null || !path.canReach() || !at.equals(path.getTarget())
                || path.getNodeCount() < 2 || path.getNodeCount() > MAX_REACH + 1
                || !at.equals(path.getEndNode().asBlockPos())) return null;
        return new Position(mob, path);
    }

    static boolean standable(ServerLevel level, PathfinderMob mob, BlockPos at) {
        if (!level.hasChunkAt(at) || !level.hasChunkAt(at.below())
                || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) return false;
        var destination = Vec3.atBottomCenterOf(at);
        AABB body = mob.getBoundingBox().move(destination.subtract(mob.position()));
        var border = level.getWorldBorder();
        return level.hasChunksAt(BlockPos.containing(body.minX, body.minY, body.minZ),
                BlockPos.containing(body.maxX, body.maxY, body.maxZ))
            && body.minX >= border.getMinX() && body.maxX <= border.getMaxX()
            && body.minZ >= border.getMinZ() && body.maxZ <= border.getMaxZ()
            && !level.getBlockCollisions(mob, body).iterator().hasNext() && !level.containsAnyLiquid(body);
    }

    private static Vec3 directionFrom(Entity from, Entity to) {
        return BossAttackGeometry.flatDirection(to.position().subtract(from.position()));
    }

    private static Vec3 sideOf(Vec3 forward) {
        return new Vec3(-forward.z, 0, forward.x);
    }
}
