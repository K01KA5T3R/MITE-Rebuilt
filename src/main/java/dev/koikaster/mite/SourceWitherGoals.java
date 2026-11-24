package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** pp/qn/qm/px/ql/qx/qy decisions; modern flying navigation is transport. */
final class SourceWitherGoals {
 static void register(SourceWitherBoss actor){
  var ai=(SourceMobAi)(Object)actor;var goals=ai.mite$goals();var targets=ai.mite$targets();
  goals.addGoal(0,new Warmup(actor,new Swim(actor)));goals.addGoal(2,new Warmup(actor,new Ranged(actor)));goals.addGoal(5,new Warmup(actor,new OriginalWanderGoal(actor,1)));goals.addGoal(6,new Warmup(actor,new SourceZombieIdleGoals.Watch(actor)));goals.addGoal(7,new Warmup(actor,new SourceZombieIdleGoals.Look(actor)));
  targets.addGoal(1,new Warmup(actor,new OriginalHurtByGoal(actor,false)));targets.addGoal(2,new Warmup(actor,new Targets(actor)));
 }
 private static final class Warmup extends Goal {
  final SourceWitherBoss mob;final Goal delegate;Warmup(SourceWitherBoss mob,Goal delegate){this.mob=mob;this.delegate=delegate;setFlags(delegate.getFlags());}
  private boolean enabled(){return mob.ritual()==null&&mob.getInvulnerableTicks()<=0;}
  public boolean canUse(){return enabled()&&delegate.canUse();}public boolean canContinueToUse(){return enabled()&&delegate.canContinueToUse();}public void start(){delegate.start();}public void stop(){delegate.stop();}public void tick(){if(enabled())delegate.tick();}public boolean requiresUpdateEveryTick(){return delegate.requiresUpdateEveryTick();}
 }
 static final class Ranged extends Goal implements SourceRangedAttackRules.Host {
  final SourceWitherBoss mob;LivingEntity target;final SourceRangedAttackRules.State state=new SourceRangedAttackRules.State();Ranged(SourceWitherBoss mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
  public boolean canUse(){var next=mob.getTarget();if(next==null)return false;target=next;return true;}public boolean canContinueToUse(){return canUse()||!mob.getNavigation().isDone();}public void stop(){target=null;state.cooldown=-1;state.visible=0;}public void tick(){if(target!=null)SourceRangedAttackRules.tick(this);}public boolean requiresUpdateEveryTick(){return true;}
  public SourceRangedAttackRules.State state(){return state;}public double distanceSquared(){return mob.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ());}public boolean piercingLine(){return SourceBlazeRaycast.blocks(mob,mob.getEyePosition(),target.getEyePosition(),null)==null||SourceBlazeRaycast.blocks(mob,mob.getEyePosition(),target.position().add(0,target.getBbHeight()*.25f,0),null)==null;}public float range(){return 20;}public int cooldown(){return 40;}public void stopNavigation(){mob.getNavigation().stop();}public void pursue(){mob.getNavigation().moveTo(target,1);}public void look(){mob.getLookControl().setLookAt(target,30,30);}public boolean eyePassable(){return OriginalDigging.passable(mob.level(),BlockPos.containing(mob.getEyePosition()),true);}public void shoot(float power){mob.performRangedAttack(target,power);}public boolean frenzied(){return SourceMobFrenzy.active(mob);}
 }
 static final class Targets extends OriginalTargetGoal {
  LivingEntity selected;Targets(SourceWitherBoss mob){super(mob,false,false);}
  private LivingEntity mount(LivingEntity target){return target!=null&&target.getVehicle() instanceof LivingEntity rider&&!rider.isInvulnerable()?rider:target;}
  private LivingEntity nearest(){double range=range(mob);var values=new ArrayList<Mob>();for(var target:mob.level().getEntitiesOfClass(Mob.class,mob.getBoundingBox().inflate(range,6,range)))if(target.getMobType()!=MobType.UNDEAD&&suitable(target,false))values.add(target);values.sort(Comparator.comparingDouble(mob::distanceToSqr));return values.isEmpty()?null:mount(values.get(0));}
  public boolean canUse(){selected=nearest();return selected!=null;}public void tick(){if(mob.getRandom().nextInt(40)==0){var next=nearest();if(next!=null&&next!=selected&&sees(next,false)){stop();selected=next;start();return;}}selected=mount(selected);}public void start(){selected=mount(selected);mob.setTarget(selected);super.start();}
 }
 static final class Swim extends Goal {
  final SourceWitherBoss mob;Swim(SourceWitherBoss mob){this.mob=mob;setFlags(EnumSet.of(Flag.JUMP));mob.getNavigation().setCanFloat(true);}
  private boolean buoyant(){if(mob.noPhysics)return true;var slow=mob.getEffect(MobEffects.MOVEMENT_SLOWDOWN);var fast=mob.getEffect(MobEffects.MOVEMENT_SPEED);float modifier=(fast==null?0:(fast.getAmplifier()+1)*.2f)-(slow==null?0:(slow.getAmplifier()+1)*.2f);var data=mob.getPersistentData();if(data.contains("MiteSourceWebTick")&&mob.tickCount-data.getInt("MiteSourceWebTick")<=1)modifier-=.75f;if(modifier<0)modifier*=1-EnchantmentEffects.paralysisResistance(mob);return modifier>=-.5f;}
  public boolean canUse(){if(!buoyant())return false;var fluid=mob.level().getFluidState(mob.blockPosition());if(mob.isInWater()&&!fluid.is(FluidTags.WATER)&&fluid.getAmount()!=8)return mob.isInLava();return mob.isInWater()||mob.isInLava();}public boolean canContinueToUse(){return buoyant()&&canUse();}public void tick(){if(!buoyant())return;if(mob.level().getFluidState(BlockPos.containing(mob.getX(),mob.getEyeY()+.5,mob.getZ())).is(FluidTags.WATER)){var velocity=mob.getDeltaMovement();mob.setDeltaMovement(velocity.x,(double).1f,velocity.z);}else if(mob.getRandom().nextFloat()<.8f)mob.getJumpControl().jump();}public boolean requiresUpdateEveryTick(){return true;}
 }
 private SourceWitherGoals(){}
}
