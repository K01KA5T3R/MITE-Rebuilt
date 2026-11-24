package dev.koikaster.mite;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** qc's nearest unvisited legacy village door, not a modern POI target. */
public final class SourceZombieVillageGoal extends Goal {
 private final PathfinderMob mob;private final List<SourceVillageDoors.Door> visited=new ArrayList<>();private SourceVillageDoors.Door selected;private Path path;
 public SourceZombieVillageGoal(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE));}
 @Override public boolean canUse(){
  if(visited.size()>15)visited.remove(0);if(!(mob.level() instanceof ServerLevel level))return false;var v=SourceVillageDoors.get(level).closest(mob.blockPosition(),0);if(v==null)return false;
  selected=null;int nearest=Integer.MAX_VALUE;for(var d:v.doors){int distance=d.distance(mob.blockPosition());if(distance>=nearest||visited.stream().anyMatch(old->old.pos.equals(d.pos)))continue;nearest=distance;selected=d;}if(selected==null)return false;
  var nav=(GroundPathNavigation)mob.getNavigation();boolean old=nav.canOpenDoors();nav.setCanOpenDoors(false);try{path=nav.createPath(selected.pos,0);}finally{nav.setCanOpenDoors(old);}if(path!=null)return true;
  var fallback=DefaultRandomPos.getPosTowards(mob,10,7,Vec3.atCenterOf(selected.pos),Math.PI/2);if(fallback==null)return false;
  nav.setCanOpenDoors(false);try{path=nav.createPath(fallback.x,fallback.y,fallback.z,0);}finally{nav.setCanOpenDoors(old);}return path!=null;
 }
 @Override public boolean canContinueToUse(){if(mob.getNavigation().isDone())return false;float range=mob.getBbWidth()+4;return mob.distanceToSqr(selected.pos.getX(),selected.pos.getY(),selected.pos.getZ())>range*range;}
 @Override public void start(){mob.getNavigation().moveTo(path,1);}@Override public void stop(){if(mob.getNavigation().isDone()||mob.distanceToSqr(selected.pos.getX(),selected.pos.getY(),selected.pos.getZ())<16)visited.add(selected);}@Override public boolean requiresUpdateEveryTick(){return true;}
}
