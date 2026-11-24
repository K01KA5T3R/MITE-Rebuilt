package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/** Coordinates two existing members after their wave has come into contact. */
final class GiantSkeletonLateTactics {
    private static final int FIRST_WAVE = 4;
    private static final int FIRST_TICK = 52;
    private static final int LAST_TICK = 112;
    private static final int FLANK_DELAY = 15;
    private static final int MOVE_TICKS = 38;

    enum Move { DELAYED_FLANK, FEIGNED_RETREAT }

    private record Plan(Move move, GiantSkeletonTactics.Position position, PathfinderMob partner) {}

    private GiantSkeletonApproach approach;
    private Plan plan;
    private ServerPlayer target;
    private long advancesAt;
    private long nextCheckAt;
    private int usedWave;
    private boolean waiting;
    private GiantSkeletonArcherRotation rotation;
    private GiantSkeletonCaptainAdvance captainAdvance;

    void observeArrow(AbstractArrow arrow, long now) {
        if (rotation != null) rotation.observeArrow(arrow, now);
    }

    boolean shouldCheck(int wave, long waveAge, long now, boolean openingActive) {
        return wave >= FIRST_WAVE && watchWindow(waveAge) && !openingActive
            && usedWave != wave && now >= nextCheckAt;
    }

    void select(SourceGiantSkeleton boss, ServerLevel level, ServerPlayer fighter, List<PathfinderMob> members,
                int wave, long now) {
        nextCheckAt = now + 10;
        var visible = members.stream().filter(mob -> mob.onGround() && !mob.isNoAi()
            && !mob.isInWaterOrBubble() && mob.getTarget() == fighter && mob.hasLineOfSight(fighter)).toList();
        var fronts = visible.stream().filter(GiantSkeletonLateTactics::front)
            .filter(mob -> mob.distanceToSqr(fighter) >= 2.25 && mob.distanceToSqr(fighter) <= 25).limit(2).toList();
        var flanks = visible.stream().filter(mob -> role(mob) == GiantSkeletonWavePlan.Role.FLANK)
            .filter(mob -> !GiantSkeletonTactics.bow(mob) && mob.distanceToSqr(fighter) >= 20.25
                && mob.distanceToSqr(fighter) <= 121).limit(2).toList();
        var bows = visible.stream().filter(mob -> GiantSkeletonTactics.bow(mob) && mob.isUsingItem())
            .filter(mob -> mob.distanceToSqr(fighter) >= 25 && mob.distanceToSqr(fighter) <= 121).limit(2).toList();
        // Only the later rosters routinely bring two bowmen.
        if (wave >= 7) {
            var order = GiantSkeletonArcherRotation.choose(level, fighter, members);
            if (order != null) {
                rotation = new GiantSkeletonArcherRotation(boss, fighter, order, now);
                usedWave = wave;
                boss.holdAttackPortals(level);
                return;
            }
        }
        if (wave == GiantSkeletonWavePlan.LAST_WAVE) {
            var order = GiantSkeletonCaptainAdvance.choose(level, fighter, members);
            if (order != null) {
                captainAdvance = new GiantSkeletonCaptainAdvance(boss, fighter, order, now);
                usedWave = wave;
                boss.holdAttackPortals(level);
                return;
            }
        }
        Plan chosen = null;
        for (var front : fronts) {
            chosen = delayedFlank(level, fighter, front, flanks);
            if (chosen == null) chosen = retreat(level, fighter, front, bows);
            if (chosen != null) break;
        }
        if (chosen == null) return;

        plan = chosen;
        target = fighter;
        usedWave = wave;
        waiting = plan.move() == Move.DELAYED_FLANK;
        advancesAt = now + FLANK_DELAY;
        var actor = plan.position().mob();
        var path = waiting ? null : plan.position().path();
        approach = new GiantSkeletonApproach(boss, actor, target, path, now + (waiting ? FLANK_DELAY + 2 : MOVE_TICKS));
        approach.install();
        boss.holdAttackPortals(level);
    }

    void tick(SourceGiantSkeleton boss, ServerLevel level, int wave, long now) {
        if (rotation != null) {
            if (usedWave != wave || !rotation.tick(level, now)) clear(level, boss);
            else boss.holdAttackPortals(level);
            return;
        }
        if (captainAdvance != null) {
            if (usedWave != wave || !captainAdvance.tick(level, now)) clear(level, boss);
            else boss.holdAttackPortals(level);
            return;
        }
        if (plan == null) return;
        var partner = plan.partner();
        var lastPoint = plan.position().path().getTarget();
        var actor = plan.position().mob();
        if (usedWave != wave || approach.done() || !boss.includesWaveMember(partner.getUUID())
                || !GiantSkeletonContent.belongsTo(partner, boss) || !partner.isAlive()
                || partner.getTarget() != target || !partner.hasLineOfSight(target)
                || plan.move() == Move.FEIGNED_RETREAT && !GiantSkeletonTactics.bow(partner)
                || !GiantSkeletonTactics.standable(level, actor, lastPoint)) {
            clear(level, boss);
            return;
        }
        if (waiting && now >= advancesAt) {
            var route = GiantSkeletonTactics.route(level, actor, lastPoint);
            if (route == null || !actor.hasLineOfSight(target)) {
                clear(level, boss);
                return;
            }
            approach.release();
            approach = new GiantSkeletonApproach(boss, actor, target, route.path(), now + MOVE_TICKS);
            approach.install();
            waiting = false;
        }
        boss.holdAttackPortals(level);
    }

    void clear(ServerLevel level, SourceGiantSkeleton boss) {
        if (rotation != null) {
            rotation.clear();
            rotation = null;
            BossArenaPortals.release(level, boss.getUUID());
        }
        if (captainAdvance != null) {
            captainAdvance.clear();
            captainAdvance = null;
            BossArenaPortals.release(level, boss.getUUID());
        }
        if (plan == null) return;
        approach.release();
        approach = null;
        plan = null;
        target = null;
        waiting = false;
        BossArenaPortals.release(level, boss.getUUID());
    }

    private static boolean watchWindow(long age) {
        return age >= FIRST_TICK && age < LAST_TICK;
    }

    private static GiantSkeletonWavePlan.Role role(PathfinderMob mob) {
        return GiantSkeletonWavePlan.Role.read(mob.getPersistentData().getString(GiantSkeletonWavePlan.ROLE_TAG));
    }

    private static boolean front(PathfinderMob mob) {
        var role = role(mob);
        return (role == GiantSkeletonWavePlan.Role.FRONT || role == GiantSkeletonWavePlan.Role.CAPTAIN)
            && !GiantSkeletonTactics.bow(mob);
    }

    private static Plan delayedFlank(ServerLevel level, ServerPlayer target, PathfinderMob front,
                                     List<PathfinderMob> candidates) {
        var frontDirection = direction(target, front);
        for (var flank : candidates) {
            var fromTarget = direction(target, flank);
            double separation = frontDirection.dot(fromTarget);
            if (separation > .55 || separation < -.65 || Math.abs(flank.getY() - target.getY()) > 1) continue;
            var at = BlockPos.containing(target.position().add(fromTarget.scale(3.0)));
            var route = GiantSkeletonTactics.route(level, flank, at);
            if (route != null && seenFrom(level, target, at)) return new Plan(Move.DELAYED_FLANK, route, front);
        }
        return null;
    }

    private static Plan retreat(ServerLevel level, ServerPlayer target, PathfinderMob front,
                                List<PathfinderMob> archers) {
        var fromTarget = direction(target, front);
        var side = new Vec3(-fromTarget.z, 0, fromTarget.x);
        for (var archer : archers) {
            var archerDirection = direction(target, archer);
            double towardBow = archerDirection.dot(side);
            if (Math.abs(towardBow) < .55 || archerDirection.dot(fromTarget) < .1
                    || front.distanceToSqr(archer) < 9 || Math.abs(front.getY() - target.getY()) > 1) continue;
            if (archer instanceof SourceSkeleton skeleton && !SourceSkeletonAI.projectileLine(skeleton, target)) continue;
            var point = front.position().add(fromTarget.scale(2.8)).subtract(side.scale(Math.copySign(1.8, towardBow)));
            var at = BlockPos.containing(point);
            if (Vec3.atBottomCenterOf(at).distanceToSqr(target.position()) < front.distanceToSqr(target) + 8) continue;
            var route = GiantSkeletonTactics.route(level, front, at);
            if (route != null && seenFrom(level, target, at)) return new Plan(Move.FEIGNED_RETREAT, route, archer);
        }
        return null;
    }

    private static boolean seenFrom(ServerLevel level, ServerPlayer target, BlockPos at) {
        return BossAttackGeometry.visible(level, target, target.getEyePosition(), Vec3.atBottomCenterOf(at).add(0, 1.3, 0));
    }

    private static Vec3 direction(ServerPlayer target, PathfinderMob member) {
        return BossAttackGeometry.flatDirection(member.position().subtract(target.position()));
    }
}

