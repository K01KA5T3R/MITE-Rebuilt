package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** A short, exclusive native navigation goal for one summoned wave member. */
final class GiantSkeletonApproach extends Goal {
    private final SourceGiantSkeleton boss;
    private final PathfinderMob mob;
    private final ServerPlayer target;
    private final Path path;
    private final Vec3 startTarget;
    private final long until;
    private final boolean holdRequiresSight;
    private Vec3 startingPosition;
    private boolean finished;

    GiantSkeletonApproach(SourceGiantSkeleton boss, PathfinderMob mob, ServerPlayer target,
                         Path path, long until) {
        this(boss, mob, target, path, until, true);
    }

    static GiantSkeletonApproach waitInCover(SourceGiantSkeleton boss, PathfinderMob mob,
                                             ServerPlayer target, long until) {
        return new GiantSkeletonApproach(boss, mob, target, null, until, false);
    }

    private GiantSkeletonApproach(SourceGiantSkeleton boss, PathfinderMob mob, ServerPlayer target,
                                  Path path, long until, boolean holdRequiresSight) {
        this.boss = boss;
        this.mob = mob;
        this.target = target;
        this.path = path;
        this.until = until;
        this.holdRequiresSight = holdRequiresSight;
        startTarget = target.position();
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    void install() {
        goals().addGoal(1, this);
    }

    void release() {
        finished = true;
        goals().removeGoal(this);
    }

    boolean done() {
        return finished || !eligible();
    }

    private net.minecraft.world.entity.ai.goal.GoalSelector goals() {
        return ((SourceMobAi) mob).mite$goals();
    }

    private boolean eligible() {
        return mob.isAlive() && !mob.isNoAi() && !mob.isInWaterOrBubble()
            && boss.isAlive() && !boss.isNoAi() && boss.sealed()
            && boss.includesWaveMember(mob.getUUID()) && GiantSkeletonContent.belongsTo(mob, boss)
            && boss.level() == mob.level() && target.serverLevel() == mob.level()
            && mob.getTarget() == target && boss.fighter(target)
            && mob.level().getGameTime() < until && target.position().distanceToSqr(startTarget) <= 16;
    }

    @Override
    public boolean canUse() {
        return !finished && eligible();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if (path == null) {
            startingPosition = mob.position();
            mob.getNavigation().stop();
        } else {
            SourceArachnidPathBudget.commit(mob, path);
        }
    }

    @Override
    public void tick() {
        mob.getLookControl().setLookAt(target, 30, 30);
        if (path == null) {
            if (mob.position().distanceToSqr(startingPosition) > 1.5 * 1.5
                    || (holdRequiresSight && !mob.hasLineOfSight(target))) finished = true;
            return;
        }
        BlockPos feet = path.getTarget();
        var level = (ServerLevel) mob.level();
        if (!GiantSkeletonTactics.standable(level, mob, feet)) {
            finished = true;
            return;
        }
        Vec3 error = Vec3.atBottomCenterOf(feet).subtract(mob.position());
        boolean arrived = mob.onGround() && Math.abs(error.y) < .2 && error.horizontalDistanceSqr() < .6 * .6;
        if (arrived || mob.getNavigation().isDone()) finished = true;
    }

    @Override
    public void stop() {
        var navigation = mob.getNavigation();
        var current = navigation.getPath();
        if (path != null && current != null && current.getTarget().equals(path.getTarget())) navigation.stop();
        finished = true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
