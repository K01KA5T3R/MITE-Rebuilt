package dev.koikaster.mite;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/** Moves one existing archer to a second firing lane after its partner's real shot. */
final class GiantSkeletonArcherRotation {
    private static final int MAX_WAIT_TICKS = 35;
    private static final int MAX_WALK_TICKS = 45;
    private static final int MIN_ROOM_TO_MOVE = 3;

    record Plan(SourceSkeleton first, GiantSkeletonTactics.Position second) {}

    private final SourceGiantSkeleton boss;
    private final ServerPlayer target;
    private final Plan plan;
    private final long firstDeadline;
    private GiantSkeletonApproach goal;
    private UUID pendingArrow;
    private int arrowId;
    private long firedAt;
    private boolean registered;
    private boolean released;

    GiantSkeletonArcherRotation(SourceGiantSkeleton boss, ServerPlayer target, Plan plan, long now) {
        this.boss = boss;
        this.target = target;
        this.plan = plan;
        firstDeadline = now + MAX_WAIT_TICKS;
        goal = GiantSkeletonApproach.waitInCover(boss, plan.second().mob(), target, firstDeadline + 1);
        goal.install();
    }

    void observeArrow(AbstractArrow arrow, long now) {
        if (released || now >= firstDeadline || pendingArrow != null || arrow.getOwner() != plan.first()) return;
        pendingArrow = arrow.getUUID();
        arrowId = arrow.getId();
        firedAt = now;
    }

    boolean tick(ServerLevel level, long now) {
        var first = plan.first();
        var second = plan.second().mob();
        var end = plan.second().path().getTarget();
        if (!valid(first) || !valid(second) || goal.done() || !GiantSkeletonTactics.standable(level, second, end)) {
            return false;
        }
        if (released) return true;
        if (!first.hasLineOfSight(target)) return false;
        if (pendingArrow == null) return now < firstDeadline;
        if (now <= firedAt) return true;
        if (!registered) {
            var arrow = level.getEntity(arrowId);
            registered = arrow instanceof AbstractArrow shot && shot.getUUID().equals(pendingArrow)
                && shot.getOwner() == first;
            if (!registered) {
                pendingArrow = null;
                return now < firstDeadline;
            }
        }
        if (now - firedAt < MIN_ROOM_TO_MOVE) return true;

        var route = GiantSkeletonTactics.route(level, second, end);
        if (route == null || !fromEndpoint(level, second, target, end)) return false;
        goal.release();
        goal = new GiantSkeletonApproach(boss, second, target, route.path(), now + MAX_WALK_TICKS);
        goal.install();
        released = true;
        return true;
    }

    void clear() {
        goal.release();
        pendingArrow = null;
    }

    private boolean valid(PathfinderMob mob) {
        return mob.isAlive() && !mob.isNoAi() && mob.getTarget() == target
            && boss.includesWaveMember(mob.getUUID()) && GiantSkeletonContent.belongsTo(mob, boss)
            && boss.fighter(target) && GiantSkeletonTactics.bow(mob);
    }

    static Plan choose(ServerLevel level, ServerPlayer target, List<PathfinderMob> members) {
        var archers = members.stream().filter(mob -> mob instanceof SourceSkeleton && GiantSkeletonTactics.bow(mob)
            && mob.onGround() && !mob.isNoAi() && !mob.isInWaterOrBubble() && mob.getTarget() == target
            && mob.distanceToSqr(target) >= 25 && mob.distanceToSqr(target) <= 196).limit(4).toList();
        for (var candidate : archers) {
            var first = (SourceSkeleton) candidate;
            if (!first.isUsingItem() || !first.hasLineOfSight(target) || !SourceSkeletonAI.projectileLine(first, target)) continue;
            for (var second : archers) {
                if (first == second || second.hasLineOfSight(target)) continue;
                var route = secondLane(level, target, first, second);
                if (route != null) return new Plan(first, route);
            }
        }
        return null;
    }

    private static GiantSkeletonTactics.Position secondLane(ServerLevel level, ServerPlayer target,
                                                PathfinderMob first, PathfinderMob second) {
        Vec3 fromTarget = BossAttackGeometry.flatDirection(second.position().subtract(target.position()));
        Vec3 fromFirst = BossAttackGeometry.flatDirection(first.position().subtract(target.position()));
        Vec3 side = new Vec3(-fromTarget.z, 0, fromTarget.x);
        for (int sign : new int[]{-1, 1}) {
            var around = second.position().add(side.scale(3.1 * sign)).subtract(fromTarget.scale(.6));
            BlockPos end = BlockPos.containing(around);
            Vec3 newAngle = BossAttackGeometry.flatDirection(Vec3.atBottomCenterOf(end).subtract(target.position()));
            if (newAngle.dot(fromFirst) > .73 || !fromEndpoint(level, second, target, end)) continue;
            var route = GiantSkeletonTactics.route(level, second, end);
            if (route != null) return route;
        }
        return null;
    }

    private static boolean fromEndpoint(ServerLevel level, PathfinderMob second, ServerPlayer target, BlockPos end) {
        return level.hasChunkAt(end) && BossAttackGeometry.visible(level, second,
            Vec3.atBottomCenterOf(end).add(0, second.getEyeHeight(), 0), target.getEyePosition());
    }
}
