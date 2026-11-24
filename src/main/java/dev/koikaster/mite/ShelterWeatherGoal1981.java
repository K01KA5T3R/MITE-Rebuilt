package dev.koikaster.mite;
import java.util.EnumSet;import net.minecraft.core.*;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.PathfinderMob;import net.minecraft.world.entity.ai.goal.Goal;import net.minecraft.world.level.pathfinder.Path;
/** Existing livestock seek nearby cover only during the selected regional weather. */
public final class ShelterWeatherGoal1981 extends Goal {
 private final PathfinderMob mob;private Path path;private long nextScan;private int holding;
 public ShelterWeatherGoal1981(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
 public boolean canUse(){if(!(mob.level() instanceof ServerLevel level)||mob.isPassenger()||mob.isVehicle()||mob.isLeashed()||mob.getTarget()!=null||level.getGameTime()<nextScan)return false;nextScan=level.getGameTime()+80;var origin=mob.blockPosition();if(LocalWeather1981.at(level,origin.above()).strength()<.45f)return false;for(int radius:new int[]{4,8,12})for(int direction=0;direction<8;direction++){double angle=direction*Math.PI/4;int dx=(int)Math.round(Math.cos(angle)*radius),dz=(int)Math.round(Math.sin(angle)*radius);for(int dy:new int[]{0,1,-1,2,-2}){var pos=origin.offset(dx,dy,dz);if(!level.hasChunkAt(pos)||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||!level.getFluidState(pos).isEmpty()||!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()||!level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()||LocalWeather1981.exposed(level,pos.above()))continue;var found=SourceArachnidPathBudget.block(mob,pos,16);var end=found==null?null:found.getEndNode();if(end!=null&&end.x==pos.getX()&&end.y==pos.getY()&&end.z==pos.getZ()){path=found;return true;}}}return false;}
 public boolean canContinueToUse(){return mob.level() instanceof ServerLevel level&&!mob.isPassenger()&&!mob.isVehicle()&&!mob.isLeashed()&&mob.getTarget()==null&&holding>0&&LocalWeather1981.regional(level,mob.blockPosition().above()).strength()>.2f;}
 public void start(){holding=200;((SourceArachnidPathBudget)mob.getNavigation()).mite$sourceCommitPath(path);double movement=mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);double speed=.9*movement>.3?.3/movement:.9;mob.getNavigation().moveTo(path,speed);}
 public void tick(){holding--;}
 public void stop(){path=null;mob.getNavigation().stop();}
 public boolean requiresUpdateEveryTick(){return true;}
}
