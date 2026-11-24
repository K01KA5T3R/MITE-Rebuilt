package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.pathfinder.Path;
/** Source EntityAIFleeAttackerOrPanic(1,0,true); the zero chance still consumes source random draws. */
public final class OriginalWolfPanicGoal extends OriginalItemMovementGoal {
 private final Wolf wolf;private final OriginalWolfFleeState state;private int panic;private boolean done;
 public OriginalWolfPanicGoal(Wolf wolf,OriginalWolfFleeState state){super(wolf,1,true);this.wolf=wolf;this.state=state;}
 private void panic(){panic=100+wolf.getRandom().nextInt(100);}
 private boolean panicking(){if(wolf.isOnFire()||state.spooked())panic();return panic>0;}
 public boolean canUse(){if(done&&wolf.level().random.nextInt(10000)==0)done=false;state.consider();return state.decided||panicking();}
 private Path randomPath(){var pos=OriginalWolfPaths.panicRandom(wolf);return pos==null?null:wolf.getNavigation().createPath(pos,0);}
 protected Path movementPath(){if(panicking())return randomPath();var attacker=state.harming();Path path=attacker==null?null:OriginalWolfPaths.away(wolf,BlockPos.containing(attacker.position()));if(path!=null){state.fleeing=true;OriginalWolfOwners.onFleeing(wolf);return path;}if(wolf.getRandom().nextFloat()<0f)panic();return randomPath();}
 public void start(){if(!done&&wolf.getRandom().nextFloat()<0f)panic();if(!panicking())done=true;super.start();}
 public void tick(){int before=panic;if(wolf.isOnFire())panic();else if(panic>0&&--panic==0)done=true;if(before==0&&panic>0||stuck()||wolf.getNavigation().isDone())super.start();}
 public boolean canContinueToUse(){state.stopConsidering();return state.decided||panicking();}
 protected float movementSpeed(){return super.movementSpeed()+(panicking()?.2f:0);}
 public void stop(){state.stopConsidering();super.stop();}
 public boolean panickingNow(){return panic>0;}
}
