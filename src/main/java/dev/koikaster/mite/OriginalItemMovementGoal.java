package dev.koikaster.mite;

import java.util.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** EntityAIMovementTask. Connected through source food goals in0.26; runtime proof is tracked separately.
 * Native path water malus is the modern adapter for source navigator.avoidsWater.
 */
public abstract class OriginalItemMovementGoal extends Goal {
 protected final PathfinderMob mob;protected final float speed;protected final boolean swimIfNecessary;
 protected final int sourceRandomIndex;private int lastX,lastZ,notMoved,threshold;private float previousWaterMalus;
 protected OriginalItemMovementGoal(PathfinderMob mob,float speed,boolean swim){this.mob=mob;this.speed=speed;swimIfNecessary=swim;sourceRandomIndex=mob.getRandom().nextInt();setFlags(EnumSet.of(Flag.MOVE));}
 protected abstract Path movementPath();
 protected float movementSpeed(){return speed;}
 protected boolean stuck(){return notMoved>threshold;}
 private boolean invalid(Path path){if(path==null||path.isDone())return true;var end=path.getEndNode();return mob.position().distanceToSqr(new Vec3(end.x,end.y,end.z))<1;}
 public void start(){lastX=mob.getBlockX();lastZ=mob.getBlockZ();notMoved=0;threshold=30+mob.getRandom().nextInt(20);previousWaterMalus=mob.getPathfindingMalus(BlockPathTypes.WATER);var path=movementPath();if(invalid(path)&&previousWaterMalus>0&&swimIfNecessary){mob.setPathfindingMalus(BlockPathTypes.WATER,0);path=movementPath();}mob.getNavigation().moveTo(path,movementSpeed());}
 public void tick(){mob.getNavigation().setSpeedModifier(movementSpeed());int x=mob.getBlockX(),z=mob.getBlockZ();if(x==lastX&&z==lastZ)++notMoved;else{lastX=x;lastZ=z;notMoved=0;}}
 public boolean canContinueToUse(){return !stuck()&&!mob.getNavigation().isDone();}
 public void stop(){mob.getNavigation().stop();mob.setPathfindingMalus(BlockPathTypes.WATER,previousWaterMalus);}
 public boolean requiresUpdateEveryTick(){return true;}
}
