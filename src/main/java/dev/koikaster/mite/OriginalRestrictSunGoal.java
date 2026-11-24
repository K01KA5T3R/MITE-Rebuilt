package dev.koikaster.mite;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
/** qp, installed on Shadow: native navigation's avoidSun is the source navigator flag. */
public final class OriginalRestrictSunGoal extends Goal {
 private final PathfinderMob mob;
 public OriginalRestrictSunGoal(PathfinderMob mob){this.mob=mob;}
 public boolean canUse(){return SourceSpecialSpawns.sourceDay(mob.level());}
 public void start(){((GroundPathNavigation)mob.getNavigation()).setAvoidSun(true);}
 public void stop(){((GroundPathNavigation)mob.getNavigation()).setAvoidSun(false);}
}
