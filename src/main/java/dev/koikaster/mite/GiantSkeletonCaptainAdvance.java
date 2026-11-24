package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The last sealed wave places a guard on one side before the captain advances down the middle. */
final class GiantSkeletonCaptainAdvance {
    private static final int GUARD_TICKS = 30;
    private static final int HANDOFF_TICKS = 5;
    private static final int CAPTAIN_TICKS = 28;
    private static final double ARRIVAL_RADIUS = .72;

    record Plan(GiantSkeletonTactics.Position guard, GiantSkeletonTactics.Position captain,
                Vec3 anchor, Vec3 side) {}

    private final SourceGiantSkeleton boss;
    private final ServerPlayer target;
    private final Plan plan;
    private final long guardDeadline;
    private GiantSkeletonApproach guardGoal;
    private GiantSkeletonApproach captainGoal;
    private long guardReachedAt = -1;
    private long captainStartedAt = -1;

    GiantSkeletonCaptainAdvance(SourceGiantSkeleton boss, ServerPlayer target, Plan plan, long now) {
        this.boss = boss;
        this.target = target;
        this.plan = plan;
        guardDeadline = now + GUARD_TICKS;
        guardGoal = new GiantSkeletonApproach(boss, plan.guard().mob(), target, plan.guard().path(), guardDeadline);
        captainGoal = new GiantSkeletonApproach(boss, plan.captain().mob(), target,
            null, guardDeadline + HANDOFF_TICKS + 1);
        guardGoal.install();
        captainGoal.install();
    }

    boolean tick(ServerLevel level, long now) {
        PathfinderMob guard = plan.guard().mob();
        PathfinderMob captain = plan.captain().mob();
        BlockPos guardSite = plan.guard().path().getTarget();
        BlockPos captainSite = plan.captain().path().getTarget();
        if (!canFight(level, guard) || !canFight(level, captain)
                || target.position().distanceToSqr(plan.anchor()) > 9
                || !GiantSkeletonTactics.standable(level, guard, guardSite)
                || !GiantSkeletonTactics.standable(level, captain, captainSite)) {
            return false;
        }
        if (captainStartedAt >= 0) {
            return !guardGoal.done() && reached(guard, guardSite) && !captainGoal.done()
                && !reached(captain, captainSite) && now < captainStartedAt + CAPTAIN_TICKS;
        }
        if (captainGoal.done() || now >= guardDeadline && guardReachedAt < 0) return false;
        if (guardReachedAt < 0) {
            if (!reached(guard, guardSite)) return !guardGoal.done();
            guardReachedAt = now;
            guardGoal.release();
            guardGoal = new GiantSkeletonApproach(boss, guard, target, null, now + HANDOFF_TICKS + CAPTAIN_TICKS + 1);
            guardGoal.install();
            return true;
        }
        if (guardGoal.done() || !reached(guard, guardSite)) return false;
        if (now - guardReachedAt < HANDOFF_TICKS) return true;

        var route = GiantSkeletonTactics.route(level, captain, captainSite);
        if (route == null || !inLane(route, plan.anchor(), plan.side(), false)
                || !escapeSide(level, target, plan.side().reverse())) return false;
        captainGoal.release();
        captainGoal = new GiantSkeletonApproach(boss, captain, target, route.path(), now + CAPTAIN_TICKS);
        captainGoal.install();
        captainStartedAt = now;
        return true;
    }

    void clear() {
        guardGoal.release();
        captainGoal.release();
    }

    private boolean canFight(ServerLevel level, PathfinderMob member) {
        return boss.fighter(target) && member.level() == level && member.isAlive() && !member.isNoAi()
            && !member.isInWaterOrBubble() && member.getTarget() == target && member.hasLineOfSight(target)
            && boss.includesWaveMember(member.getUUID()) && GiantSkeletonContent.belongsTo(member, boss);
    }

    static Plan choose(ServerLevel level, ServerPlayer target, List<PathfinderMob> members) {
        var captains = members.stream().filter(member -> eligible(member, target, GiantSkeletonWavePlan.Role.CAPTAIN)
            && member.distanceToSqr(target) >= 25 && member.distanceToSqr(target) <= 121).limit(1).toList();
        if (captains.isEmpty()) return null;
        PathfinderMob captain = captains.get(0);
        Vec3 anchor = target.position();
        Vec3 facing = BossAttackGeometry.flatDirection(captain.position().subtract(anchor));
        Vec3 side = new Vec3(-facing.z, 0, facing.x);
        var captainRoute = GiantSkeletonTactics.route(level, captain, BlockPos.containing(anchor.add(facing.scale(3.2))));
        if (captainRoute == null) return null;

        for (var guard : members) {
            if (!eligible(guard, target, GiantSkeletonWavePlan.Role.FLANK)) continue;
            Vec3 relative = guard.position().subtract(anchor);
            double outward = relative.dot(side);
            if (relative.dot(facing) < 2.8 || Math.abs(outward) < 2.1 || relative.lengthSqr() > 100) continue;
            Vec3 guardSide = side.scale(Math.copySign(1, outward));
            Vec3 guardFeet = anchor.add(facing.scale(2.1)).add(guardSide.scale(3.1));
            var guardRoute = GiantSkeletonTactics.route(level, guard, BlockPos.containing(guardFeet));
            if (guardRoute != null && inLane(guardRoute, anchor, guardSide, true)
                    && inLane(captainRoute, anchor, guardSide, false)
                    && escapeSide(level, target, guardSide.reverse())) {
                return new Plan(guardRoute, captainRoute, anchor, guardSide);
            }
        }
        return null;
    }

    private static boolean eligible(PathfinderMob member, ServerPlayer target, GiantSkeletonWavePlan.Role role) {
        return member instanceof SourceMobAi && member.isAlive() && !member.isNoAi() && member.onGround()
            && member.getTarget() == target && member.hasLineOfSight(target) && !member.isInWaterOrBubble()
            && !GiantSkeletonTactics.bow(member) && Math.abs(member.getY() - target.getY()) < .8
            && GiantSkeletonWavePlan.Role.read(member.getPersistentData().getString(GiantSkeletonWavePlan.ROLE_TAG)) == role;
    }

    private static boolean reached(PathfinderMob mob, BlockPos site) {
        Vec3 delta = Vec3.atBottomCenterOf(site).subtract(mob.position());
        return mob.onGround() && Math.abs(delta.y) < .24 && delta.horizontalDistanceSqr() < ARRIVAL_RADIUS * ARRIVAL_RADIUS;
    }

    private static boolean inLane(GiantSkeletonTactics.Position route, Vec3 anchor, Vec3 side, boolean guard) {
        var path = route.path();
        for (int node = 0; node < path.getNodeCount(); node++) {
            double offset = Vec3.atBottomCenterOf(path.getNode(node).asBlockPos()).subtract(anchor).dot(side);
            if (guard ? offset < 1.95 : Math.abs(offset) > .95) return false;
        }
        return true;
    }

    private static boolean escapeSide(ServerLevel level, ServerPlayer player, Vec3 direction) {
        if (!player.onGround() || !DragonCombat.escape(level, player, direction, 2)) return false;
        for (int step = 1; step <= 4; ++step) {
            AABB at = player.getBoundingBox().move(direction.scale(step * .5));
            if (!SourceBiomePortal.fitsBorder(level, at) || level.containsAnyLiquid(at)) return false;
        }
        return true;
    }
}
