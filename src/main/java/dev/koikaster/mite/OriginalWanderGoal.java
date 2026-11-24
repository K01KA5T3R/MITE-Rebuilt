package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
/** qm's source speed cap and1/120 chance, without modern reduced-tick or idle-time gates. */
public final class OriginalWanderGoal extends Goal {
 private final PathfinderMob mob;private final double speed;private Vec3 target;
 public OriginalWanderGoal(PathfinderMob mob,double multiplier){this.mob=mob;double movement=mob.getAttributeValue(Attributes.MOVEMENT_SPEED);speed=multiplier*movement>.3?.3/movement:multiplier;setFlags(EnumSet.of(Flag.MOVE));}
 public boolean canUse(){long last=mob.getPersistentData().getLong("MiteLastCactusHarm");long elapsed=last<1?-1:mob.level().getGameTime()-last;boolean cactus=elapsed>=0&&elapsed<10&&mob.getNavigation().isDone();if(!cactus&&mob.getRandom().nextInt(120)!=0)return false;target=DefaultRandomPos.getPos(mob,10,7);return target!=null;}
 public boolean canContinueToUse(){return !mob.getNavigation().isDone();}public void start(){mob.getNavigation().moveTo(target.x,target.y,target.z,speed);}public boolean requiresUpdateEveryTick(){return true;}
}
