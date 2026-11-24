package dev.koikaster.mite;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;
/** EntityAIMoveToTree: burning revenant seeks source log role below raised players and boosts real fire spread. */
public final class OriginalBurningTreeGoal extends OriginalItemMovementGoal {
 public OriginalBurningTreeGoal(PathfinderMob mob,float speed){super(mob,speed,false);}
 public boolean canUse(){return mob.getRandom().nextInt(40)==0&&mob.isOnFire();}
 protected Path movementPath(){for(var candidate:OriginalLogCandidates.find(mob,16,4,8))for(var player:mob.level().players()){int dx=player.getBlockX()-candidate.getX(),dz=player.getBlockZ()-candidate.getZ();if(player.isSpectator()||player.isRemoved()||!player.isAlive()||player.getBlockY()<=candidate.getY()+2||player.getBlockY()>=candidate.getY()+9||dx*dx+dz*dz>32)continue;var path=mob.getNavigation().createPath(candidate,0,16);if(path==null)continue;var end=path.getEndNode();if(!(OriginalEnvironmentPaths.sq(end.x-candidate.getX(),end.y-candidate.getY(),end.z-candidate.getZ())<2))continue;((SourceFireMobState)mob).mite$fireSpreadTicks(100);return path;}return null;}
 public boolean canContinueToUse(){return !mob.getNavigation().isDone();}
 public void stop(){mob.getNavigation().stop();}
}
