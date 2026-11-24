package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
/** EntityAISeekShelterFromRain exact sixteen indexed trials and final point revalidation; FireElemental owner. */
public final class OriginalRainShelterGoal extends OriginalItemMovementGoal {
 private int index;
 public OriginalRainShelterGoal(PathfinderMob mob,float speed,boolean swim){super(mob,speed,swim);index=sourceRandomIndex;OriginalEnvironmentPaths.initialize(mob);}
 public boolean canUse(){if(!OriginalRandomTables.chance_in_16[++index&32767])return false;return OriginalWeather.rain(mob.level(),mob.blockPosition());}
 protected Path movementPath(){int x=mob.getBlockX(),y=mob.getBlockY(),z=mob.getBlockZ();double best=0;Path selected=null;for(int attempt=0;attempt<16;attempt++){int dx=OriginalRandomTables.int_max[++index&32767]%33-16;int dy=OriginalRandomTables.int_7_minus_3[++index&32767];int dz=OriginalRandomTables.int_max[++index&32767]%33-16;var trial=OriginalEnvironmentPaths.settle(mob,new BlockPos(x+dx,y+dy,z+dz));if(!OriginalEnvironmentPaths.passable(mob,trial,false)||OriginalWeather.rain(mob.level(),trial)||OriginalEnvironmentPaths.occupied(mob,trial))continue;double distance=OriginalEnvironmentPaths.sq(dx,dy,dz);if(selected!=null&&!(distance<best))continue;var path=mob.getNavigation().createPath(trial,0,16);if(path==null)continue;var end=path.getEndNode();var at=new BlockPos(end.x,end.y,end.z);if(OriginalWeather.rain(mob.level(),at)||OriginalEnvironmentPaths.occupied(mob,at))continue;distance=OriginalEnvironmentPaths.sq(end.x-x,end.y-y,end.z-z);if(selected!=null&&!(distance<best))continue;best=distance;selected=path;}return selected;}
}
