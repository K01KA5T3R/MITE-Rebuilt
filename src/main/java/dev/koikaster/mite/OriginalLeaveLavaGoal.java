package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
/** EntityAIGetOutOfLava direct control flow, installed by the source hellhound constructor. */
public final class OriginalLeaveLavaGoal extends OriginalItemMovementGoal {
 private final int maximum=16;private int index;
 public OriginalLeaveLavaGoal(PathfinderMob mob,float speed){super(mob,speed,true);index=sourceRandomIndex;OriginalEnvironmentPaths.initialize(mob);}
 public boolean canUse(){if(!OriginalRandomTables.chance_in_16[++index&32767])return false;return mob.isInLava();}
 protected Path movementPath(){int x=mob.getBlockX(),y=mob.getBlockY(),z=mob.getBlockZ();double best=0;Path selected=null;for(int attempt=0;attempt<16;attempt++){int dx=OriginalRandomTables.int_max[++index&32767]%33-maximum;int dy=OriginalRandomTables.int_7_minus_3[++index&32767];int dz=OriginalRandomTables.int_max[++index&32767]%33-maximum;var trial=OriginalEnvironmentPaths.settle(mob,new BlockPos(x+dx,y+dy,z+dz));if(!OriginalEnvironmentPaths.passable(mob,trial,false)||OriginalEnvironmentPaths.passable(mob,trial.below(),true)||OriginalEnvironmentPaths.occupied(mob,trial))continue;double distance=OriginalEnvironmentPaths.sq(dx,dy,dz);if(selected!=null&&!(distance<best))continue;var path=mob.getNavigation().createPath(trial,0,maximum);if(path==null)continue;var end=path.getEndNode();var at=new BlockPos(end.x,end.y,end.z);if(OriginalEnvironmentPaths.passable(mob,at.below(),true)||OriginalEnvironmentPaths.occupied(mob,at))continue;distance=OriginalEnvironmentPaths.sq(end.x-x,end.y-y,end.z-z);if(selected!=null&&!(distance<best))continue;best=distance;selected=path;}return selected==null?OriginalEnvironmentPaths.toward(mob,OriginalOrigin.position(mob),maximum):selected;}
}
