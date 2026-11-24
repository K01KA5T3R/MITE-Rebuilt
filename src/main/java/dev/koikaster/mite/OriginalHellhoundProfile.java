package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/** R195 EntityHellhound constructor/profile; the hellhound owner installs these in registerGoals. */
public final class OriginalHellhoundProfile {
 public static final float WIDTH=.6f,HEIGHT=.8f;
 public static final int EXPERIENCE=15;
 // Source pp/pw/px/ql leaf factories; their confirmed control flow remains unchanged.
 public static Goal swimGoal(PathfinderMob mob){return new Swim(mob);}
 public static Goal leapGoal(PathfinderMob mob){return new Leap(mob,.4f);}
 public static Goal watchGoal(PathfinderMob mob){return new Watch(mob,8);}
 public static Goal idleGoal(PathfinderMob mob){return new Idle(mob);}

 public static AttributeSupplier.Builder attributes(){return Wolf.createAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.4f).add(Attributes.FOLLOW_RANGE,16);}
 /** Call in the hellhound branch of registerGoals, after native selectors exist. No tame/breed/food tasks survive. */
 public static void install(PathfinderMob mob){
  var access=(SourceMobAi)mob;var goals=access.mite$goals();var targets=access.mite$targets();
  goals.removeAllGoals(goal->true);targets.removeAllGoals(goal->true);
  mob.setPathfindingMalus(BlockPathTypes.WATER,8); // source rf.avoidsWater=true on modern path malus
  goals.addGoal(1,new Swim(mob));goals.addGoal(2,new Leap(mob,.4f));
  goals.addGoal(3,new OriginalMeleeGoal(mob,null,1,true,null));
  goals.addGoal(6,new OriginalWanderGoal(mob,1));
  goals.addGoal(7,new Watch(mob,8));goals.addGoal(7,new Idle(mob));
  targets.addGoal(1,new OriginalHurtByGoal(mob,true));
  targets.addGoal(2,new OriginalNearestTargetGoal(mob,Player.class,0,true));
  targets.addGoal(4,new OriginalNearestTargetGoal(mob,Animal.class,20,true));
  goals.addGoal(4,new OriginalLeaveWaterGoal(mob,1));goals.addGoal(4,new OriginalLeaveLavaGoal(mob,1));
 }
 public static float walkValue(Mob mob,BlockPos pos){return SourceSpecialSpawns.sourceDay(mob.level())&&mob.level().canSeeSky(pos)?-.5f:10;}
 /** Source biological classification is independent of modern MobType; do not label this wolf UNDEAD. */
 public static boolean biologicallyAlive(){return false;}

 /** pp source, specialized to this non-earth owner. Fluid surfaces and cobweb occupancy use modern world APIs. */
 private static final class Swim extends Goal {
  private final PathfinderMob mob;
  private Swim(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.JUMP));((GroundPathNavigation)mob.getNavigation()).setCanFloat(true);}
  private boolean buoyant(){
   if(mob.horizontalCollision)return true;
   var slow=mob.getEffect(MobEffects.MOVEMENT_SLOWDOWN);var haste=mob.getEffect(MobEffects.MOVEMENT_SPEED);
   float combined=(haste==null?0:(haste.getAmplifier()+1)*.2f)-(slow==null?0:(slow.getAmplifier()+1)*.2f);
   if(mob.level().getBlockStates(mob.getBoundingBox()).anyMatch(state->state.is(Blocks.COBWEB)))combined-=.75f;
   if(combined<0)combined*=1-EnchantmentEffects.paralysisResistance(mob);
   return combined>=-.5f;
  }
  public boolean canUse(){
   if(!buoyant())return false;
   var at=mob.blockPosition();var fluid=mob.level().getFluidState(at);
   boolean footInWater=fluid.is(FluidTags.WATER)&&mob.getY()<at.getY()+fluid.getHeight(mob.level(),at);
   if(mob.isInWater()&&!footInWater&&!(fluid.is(FluidTags.WATER)&&fluid.isSource()))return mob.isInLava();
   return mob.isInWater()||mob.isInLava();
  }
  public boolean canContinueToUse(){return canUse();}
  public void tick(){
   if(!buoyant())return;
   if(mob.level().getFluidState(BlockPos.containing(mob.getX(),mob.getEyeY()+.5,mob.getZ())).is(FluidTags.WATER)){var motion=mob.getDeltaMovement();mob.setDeltaMovement(motion.x,.1f,motion.z);return;}
   if(mob.getRandom().nextFloat()<.8f)mob.getJumpControl().jump();
  }
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** pw retains its additive horizontal momentum, 1/5 chance and yaw assignment. */
 private static final class Leap extends Goal {
  private final PathfinderMob mob;private final float height;private LivingEntity target;
  private Leap(PathfinderMob mob,float height){this.mob=mob;this.height=height;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
  public boolean canUse(){target=mob.getTarget();if(target==null)return false;double distance=mob.distanceToSqr(target);return distance>=4&&distance<=16&&mob.onGround()&&mob.getRandom().nextInt(5)==0;}
  public boolean canContinueToUse(){return !mob.onGround();}
  public void start(){double dx=target.getX()-mob.getX(),dz=target.getZ()-mob.getZ();float length=(float)Math.sqrt(dx*dx+dz*dz);var motion=mob.getDeltaMovement();mob.setDeltaMovement(motion.x+dx/length*.5*(double).8f+motion.x*(double).2f,height,motion.z+dz/length*.5*(double).8f+motion.z*(double).2f);double yaw=Math.atan2(dz,dx)*180/Math.PI-90;mob.setYRot((float)(yaw<0?yaw+360:yaw));}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** px: native watch goals use reduced delays; this one keeps 40..79 full ticks. */
 private static final class Watch extends Goal {
  private final PathfinderMob mob;private final float range;private Player target;private int time;
  private Watch(PathfinderMob mob,float range){this.mob=mob;this.range=range;setFlags(EnumSet.of(Flag.LOOK));}
  public boolean canUse(){if(mob.getRandom().nextFloat()>=.02f)return false;target=OriginalDespawn.closest(mob,range,false);return target!=null;}
  public boolean canContinueToUse(){return target!=null&&target.isAlive()&&mob.distanceToSqr(target)<=range*range&&time>0;}
  public void start(){time=40+mob.getRandom().nextInt(40);}public void stop(){target=null;}
  public void tick(){mob.getLookControl().setLookAt(target.getX(),target.getEyeY(),target.getZ(),10,mob.getMaxHeadXRot());--time;}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 /** ql is unpatched vanilla 1.6.4: MOVE+LOOK flags and >=0 continuation are deliberate. */
 private static final class Idle extends Goal {
  private final PathfinderMob mob;private double dx,dz;private int time;
  private Idle(PathfinderMob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){return mob.getRandom().nextFloat()<.02f;}public boolean canContinueToUse(){return time>=0;}
  public void start(){double angle=Math.PI*2*mob.getRandom().nextDouble();dx=Math.cos(angle);dz=Math.sin(angle);time=20+mob.getRandom().nextInt(20);}
  public void tick(){--time;mob.getLookControl().setLookAt(mob.getX()+dx,mob.getEyeY(),mob.getZ()+dz,10,mob.getMaxHeadXRot());}
  public boolean requiresUpdateEveryTick(){return true;}
 }
 private OriginalHellhoundProfile(){}
}
