package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
/** R195 modified po, not the unchanged native FleeSunGoal; concrete owner is Shadow. */
public final class OriginalFleeSunGoal extends Goal {
 private final PathfinderMob mob;private final double speed;private Vec3 destination;
 public OriginalFleeSunGoal(PathfinderMob mob,double speed){this.mob=mob;this.speed=speed;setFlags(EnumSet.of(Flag.MOVE));}
 public boolean canUse(){if(!SourceSpecialSpawns.sourceDay(mob.level())||!mob.isOnFire()||!mob.level().canSeeSky(new BlockPos(mob.getBlockX(),Mth.floor(mob.getBoundingBox().minY),mob.getBlockZ())))return false;destination=destination();return destination!=null;}
 protected Vec3 destination(){var level=mob.level();var random=mob.getRandom();int ox=mob.getBlockX(),oy=Mth.floor(mob.getBoundingBox().minY),oz=mob.getBlockZ();for(int attempt=0;attempt<10;attempt++){
  int x=ox+random.nextInt(3+attempt*2)-(1+attempt),y=oy+random.nextInt(7)-3,z=oz+random.nextInt(3+attempt*2)-(1+attempt);var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
  if(state.isCollisionShapeFullBlock(level,pos))continue;
  if(state.is(Blocks.WATER)&&OriginalDigging.passable(level,pos.above(),true))return new Vec3(x,y,z);
  if((state.is(Blocks.LAVA)||state.is(Blocks.FIRE))&&!mob.fireImmune())continue;
  if(!state.getCollisionShape(level,pos).isEmpty()&&!OriginalDigging.passable(level,pos,false)||level.canSeeSky(pos.above()))continue;
  var below=level.getBlockState(pos.below());if((below.is(Blocks.LAVA)||below.is(Blocks.FIRE))&&!mob.fireImmune())continue;
  if(mob.getBbHeight()<=1)return new Vec3(x,y,z);var head=level.getBlockState(pos.above());if(!head.isCollisionShapeFullBlock(level,pos.above())&&(head.getCollisionShape(level,pos.above()).isEmpty()||OriginalDigging.passable(level,pos.above(),false)))return new Vec3(x,y,z);
 }return null;}
 public boolean canContinueToUse(){return !mob.getNavigation().isDone();}
 public void start(){if(destination!=null)mob.getNavigation().moveTo(destination.x,destination.y,destination.z,speed);}
}
