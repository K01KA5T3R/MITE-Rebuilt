package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.pathfinder.Path;

/** EntityAIFleeAttackerOrPanic caller with the livestock decision overrides. */
final class SourceLivestockFleeGoal extends OriginalItemMovementGoal {
 private final Animal animal;private final SourceLivestockOwners.State state;private final float chance;int panic;boolean done;
 SourceLivestockFleeGoal(Animal animal,SourceLivestockOwners.State state){super(animal,animal instanceof Chicken?1.3f:animal instanceof Cow?1.6f:1.4f,true);this.animal=animal;this.state=state;chance=animal instanceof Chicken?.75f:.5f;}
 private void panic(){panic=100+animal.getRandom().nextInt(100);}
 private boolean panicking(){if(animal.isOnFire()||SourceLivestockSpook.spooked(animal))panic();return panic>0;}
 public boolean canUse(){if(done&&animal.level().random.nextInt(10000)==0)done=false;state.consider();return state.decided||panicking();}
 private Path randomPath(){
  // rh.c's conditional origin translation is a confirmed source quirk.
  int x=animal.getBlockX(),y=animal.getBlockY(),z=animal.getBlockZ();var home=animal.getRestrictCenter();double extent=animal.getRestrictRadius()+5;boolean restricted=animal.hasRestriction()&&home.distSqr(animal.blockPosition())+4<extent*extent;float best=-100;BlockPos selected=null;
  for(int attempt=0;attempt<10;attempt++){int dx=animal.getRandom().nextInt(11)-5,dy=animal.getRandom().nextInt(9)-4,dz=animal.getRandom().nextInt(11)-5;if(restricted){dx+=x;dy+=y;dz+=z;if(!animal.isWithinRestriction(new BlockPos(dx,dy,dz)))continue;}var at=new BlockPos(dx,dy,dz);float weight=animal.getWalkTargetValue(at,animal.level());if(weight>best){best=weight;selected=at;}}
  return selected==null?null:animal.getNavigation().createPath(selected,0);
 }
 protected Path movementPath(){
  if(panicking())return randomPath();
  var source=new SourceParentNativeWorld(animal);
  var world=new OriginalParentAwayPath.World(){public int nextWorldRandomInt(){return source.nextWorldRandomInt();}public boolean airOrPassable(int x,int y,int z,boolean liquid){return source.airOrPassable(x,y,z,liquid);}public Path graph(net.minecraft.world.entity.PathfinderMob actor,BlockPos target,int maximum,boolean navigator){return SourceArachnidPathBudget.block(actor,target,maximum);}};
  var path=state.harming==null?null:OriginalParentAwayPath.find(animal,BlockPos.containing(state.harming.position()),16,48,true,world);
  if(path!=null){state.fleeing=true;((SourceArachnidPathBudget)animal.getNavigation()).mite$sourceCommitPath(path);return path;}
  if(animal.getRandom().nextFloat()<chance)panic();return randomPath();
 }
 public void start(){if(!done&&animal.getRandom().nextFloat()<chance)panic();if(!panicking())done=true;super.start();}
 public void tick(){int before=panic;if(animal.isOnFire())panic();else if(panic>0&&--panic==0)done=true;if(before==0&&panic>0||stuck()||animal.getNavigation().isDone())super.start();}
 public boolean canContinueToUse(){state.stop();return state.decided||panicking();}
 protected float movementSpeed(){return super.movementSpeed()+(panicking()?.2f:0);}
 public void stop(){state.stop();super.stop();}
}
