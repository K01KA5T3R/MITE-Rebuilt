package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
/** EntityAISeekLitTorch. Source constructor consumes an otherwise-unused index. */
public final class OriginalSeekLitTorchGoal extends Goal {
 private final PathfinderMob mob;private final float speed;private final int sourceRandomIndex;
 public OriginalSeekLitTorchGoal(PathfinderMob mob,float speed){this.mob=mob;this.speed=speed;sourceRandomIndex=mob.getRandom().nextInt();setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 public boolean canUse(){return mob.getRandom().nextInt(Mobs.name(mob).equals("shadow")?40:200)<=0;}
 protected Path path(){for(var candidate:OriginalTorchCandidates.find(mob)){var path=mob.getNavigation().createPath(candidate,0,16);if(path!=null&&OriginalTorchInteraction.near(mob,path.getEndNode().asBlockPos()))return path;}return null;}
 public boolean canContinueToUse(){return !mob.getNavigation().isDone();}
 public void start(){mob.getNavigation().moveTo(path(),speed);}
 public void stop(){mob.getNavigation().stop();}
}
