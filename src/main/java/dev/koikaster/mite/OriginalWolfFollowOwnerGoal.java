package dev.koikaster.mite;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/** pq: failed distant paths call to the owner; source does not teleport the wolf. */
public final class OriginalWolfFollowOwnerGoal extends Goal {
 private final Wolf wolf;private final double speed;private final float startDistance,stopDistance;private final Consumer<Wolf> callToOwner;
 private LivingEntity owner;private int repath;private float previousWaterMalus;
 public OriginalWolfFollowOwnerGoal(Wolf wolf,double speed,float startDistance,float stopDistance,Consumer<Wolf> callToOwner){this.wolf=wolf;this.speed=speed;this.startDistance=startDistance;this.stopDistance=stopDistance;this.callToOwner=Objects.requireNonNull(callToOwner);setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 public boolean canUse(){var candidate=wolf.getOwner();if(candidate==null||wolf.isInSittingPose()||wolf.distanceToSqr(candidate)<startDistance*startDistance)return false;owner=candidate;return true;}
 public boolean canContinueToUse(){return !wolf.getNavigation().isDone()&&wolf.distanceToSqr(owner)>stopDistance*stopDistance&&!wolf.isInSittingPose();}
 public void start(){repath=0;previousWaterMalus=wolf.getPathfindingMalus(BlockPathTypes.WATER);wolf.setPathfindingMalus(BlockPathTypes.WATER,0);}
 public void stop(){owner=null;wolf.getNavigation().stop();wolf.setPathfindingMalus(BlockPathTypes.WATER,previousWaterMalus);}
 public void tick(){wolf.getLookControl().setLookAt(owner,10,wolf.getMaxHeadXRot());if(!wolf.isInSittingPose()&&--repath<=0){repath=10;if(!wolf.getNavigation().moveTo(owner,speed)&&!wolf.isLeashed()&&wolf.distanceToSqr(owner)>=144&&(wolf.tickCount+wolf.getId()*47)%10==0&&Math.random()<(double).2f)callToOwner.accept(wolf);}}
 public boolean requiresUpdateEveryTick(){return true;}
}
