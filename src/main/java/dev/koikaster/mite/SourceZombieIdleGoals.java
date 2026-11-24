package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

/** pp/px/ql source decisions around approved native control transport. */
public final class SourceZombieIdleGoals {
 public static final class Swim extends Goal {
  private final PathfinderMob mob;public Swim(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.JUMP));((GroundPathNavigation)mob.getNavigation()).setCanFloat(true);}
  private boolean buoyant(){if(mob.noPhysics)return true;var slow=mob.getEffect(MobEffects.MOVEMENT_SLOWDOWN);var fast=mob.getEffect(MobEffects.MOVEMENT_SPEED);float modifier=(fast==null?0:(fast.getAmplifier()+1)*.2f)-(slow==null?0:(slow.getAmplifier()+1)*.2f);var data=mob.getPersistentData();if(data.contains("MiteSourceWebTick")&&mob.tickCount-data.getInt("MiteSourceWebTick")<=1)modifier-=.75f;if(modifier<0)modifier*=1-EnchantmentEffects.paralysisResistance(mob);return modifier>=-.5f;}
  @Override public boolean canUse(){if(!buoyant())return false;return OriginalSwimmingWater.gate(mob);}
  @Override public boolean canContinueToUse(){return buoyant()&&canUse();}
  @Override public void tick(){if(!buoyant())return;if(mob.level().getFluidState(BlockPos.containing(mob.getX(),mob.getEyeY()+.5,mob.getZ())).is(FluidTags.WATER)){var speed=mob.getDeltaMovement();mob.setDeltaMovement(speed.x,(double).1f,speed.z);}else if(mob.getRandom().nextFloat()<.8f)mob.getJumpControl().jump();}
  @Override public boolean requiresUpdateEveryTick(){return true;}
 }
 public static final class Watch extends Goal {
  private final PathfinderMob mob;private LivingEntity selected;private int remaining;public Watch(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.LOOK));}
  @Override public boolean canUse(){if(mob.getRandom().nextFloat()>=.02f)return false;selected=OriginalDespawn.closest(mob,8,false);return selected!=null;}
  @Override public boolean canContinueToUse(){return selected.isAlive()&&mob.distanceToSqr(selected)<=64&&remaining>0;}@Override public void start(){remaining=40+mob.getRandom().nextInt(40);}@Override public void stop(){selected=null;}@Override public void tick(){mob.getLookControl().setLookAt(selected.getX(),selected.getEyeY(),selected.getZ(),10,mob.getMaxHeadXRot());remaining--;}@Override public boolean requiresUpdateEveryTick(){return true;}
 }
 public static final class Look extends Goal {
  private final PathfinderMob mob;private double x,z;private int remaining;public Look(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}@Override public boolean canUse(){return mob.getRandom().nextFloat()<.02f;}@Override public boolean canContinueToUse(){return remaining>=0;}@Override public void start(){double yaw=Math.PI*2*mob.getRandom().nextDouble();x=Math.cos(yaw);z=Math.sin(yaw);remaining=20+mob.getRandom().nextInt(20);}@Override public void tick(){remaining--;mob.getLookControl().setLookAt(mob.getX()+x,mob.getEyeY(),mob.getZ()+z,10,mob.getMaxHeadXRot());}@Override public boolean requiresUpdateEveryTick(){return true;}
 }
 private SourceZombieIdleGoals(){}
}
