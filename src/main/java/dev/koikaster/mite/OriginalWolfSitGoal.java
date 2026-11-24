package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
/** qr: requested sitting is distinct from effective synchronized sitting. */
public final class OriginalWolfSitGoal extends Goal {
 private final Wolf wolf;
 public OriginalWolfSitGoal(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
 public boolean canUse(){if(!wolf.isTame()||wolf.isInWater()||!wolf.onGround())return false;var owner=wolf.getOwner();return owner==null||(!(wolf.distanceToSqr(owner)<144&&owner.getLastHurtByMob()!=null)&&wolf.isOrderedToSit());}
 public void start(){wolf.getNavigation().stop();wolf.setInSittingPose(true);}public void stop(){wolf.setInSittingPose(false);}
 public boolean requiresUpdateEveryTick(){return true;}
}
