package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** tq.bl after on.bl, using on's already ported decision kernel and the actual
 * native path. No arachnid state, web stock or second path is attached. */
public final class SourceSilverfishLife extends Goal implements SourceArachnidDecision.Host,OriginalParentHarmState.Flags {
 private final SourceSilverfish actor;private final SourceArachnidDecision decisions;
 public final OriginalParentHarmState harm;private final OriginalParentFleeTransition flee;
 private boolean fleeing,decided;private int alertTicks,attackCooldown;
 private Entity idleLookTarget;private int idleLookTicks;private float idleYawVelocity;
 public SourceSilverfishLife(SourceSilverfish actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);flee=new OriginalParentFleeTransition(actor,harm,OriginalParentFleeTransition.nativePath(actor,1));decisions=new SourceArachnidDecision(this);setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
 @Override public boolean canUse(){return true;}
 @Override public boolean canContinueToUse(){return true;}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 public void beforeTick(){harm.beforeTick();if(attackCooldown>0)--attackCooldown;}
 public void alert(){if(alertTicks<=0)alertTicks=20;}
 public int alertTicks(){return alertTicks;}
 public void save(net.minecraft.nbt.CompoundTag tag){harm.save(tag);tag.putShort("AttackTime",(short)attackCooldown);}
 public void load(net.minecraft.nbt.CompoundTag tag){harm.load(tag);attackCooldown=tag.getShort("AttackTime");}
 @Override public void tick(){
  decisions.tick();
  if(alertTicks>0&&--alertTicks==0)SourceInfestation.summon(actor);
  // tq: null path, rather than Path.isDone(); a target is cleared before a
  // later tick may stroll. This is separate from on's ordinary 1/150 stroll.
  if(path()==null){if(actor.getTarget()==null)decisions.strollSource();else actor.setTarget(null);}
 }
 public void afterDamage(DamageSource source,SourceParentHurtFrame.Outcome outcome){
  harm.afterDamage(source,outcome.negativelyAffected());var responsible=source.getEntity();
  if(!outcome.destroyed()&&outcome.negativelyAffected()&&responsible!=actor&&responsible!=actor.getVehicle()&&responsible!=actor.getFirstPassenger()&&(responsible==null||responsible instanceof LivingEntity))actor.setTarget((LivingEntity)responsible);
 }
 @Override public SourceSilverfish actor(){return actor;}
 @Override public boolean hasDecidedToFlee(){return decided;}
 @Override public void hasDecidedToFlee(boolean value){decided=value;}
 @Override public boolean fleeing(){return fleeing;}
 @Override public void fleeing(boolean value){fleeing=value;}
 @Override public boolean beforeFleeDecisions(){return flee.beforeDecisions();}
 @Override public Path path(){return actor.getNavigation().getPath();}
 @Override public void path(Path value){SourceArachnidPathBudget.commit(actor,value);}
 @Override public LivingEntity findPlayer(float range){return SourceArachnidAI.findPlayerToAttack(actor,8);}
 @Override public LivingEntity findChicken(float range){
  LivingEntity closest=null;float shortest=0;
  for(var target:actor.level().getEntitiesOfClass(LivingEntity.class,actor.getBoundingBox().inflate(range))){
   boolean animal=target instanceof Animal&&!(target instanceof Mobs.DireWolf wolf&&wolf.sourceHellhound());
   if(!target.isAlive()||!animal&&!(target instanceof Villager)||!actor.getSensing().hasLineOfSight(target))continue;
   var end=target.blockPosition();var path=SourceArachnidPathBudget.block(actor,end,(int)range);var last=path==null?null:path.getEndNode();
   if(last==null||last.x!=end.getX()||last.y!=end.getY()||last.z!=end.getZ())continue;
   float distance=actor.distanceTo(target);if(closest==null||distance<shortest){closest=target;shortest=distance;}
  }
  return closest;
 }
 @Override public float sourceRange(){return OriginalTargetGoal.range(actor);}
 @Override public boolean sourceLineOfStrike(LivingEntity target){var origin=actor.position().add(0,actor.getBbHeight()*.75f,0);for(float fraction:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(actor,origin,target.position().add(0,target.getBbHeight()*fraction,0)))return true;return false;}
 @Override public void sourceAttackOrLeap(LivingEntity target,float distance){if(attackCooldown<=0&&distance<1.2f&&target.getBoundingBox().maxY>actor.getBoundingBox().minY&&target.getBoundingBox().minY<actor.getBoundingBox().maxY){attackCooldown=20;actor.doHurtTarget(target);}}
 @Override public Path route(Entity target,float maximum){return SourceArachnidPathBudget.entity(actor,target,maximum);}
 @Override public Path route(BlockPos target,float maximum){return SourceArachnidPathBudget.block(actor,target,maximum);}
 @Override public float walkWeight(BlockPos pos){return actor.getWalkTargetValue(pos,actor.level());}
 @Override public long lastTickHarmedByCactus(){return SourceArachnidWorld.lastCactusTick(actor);}
 @Override public boolean insideLeaves(){return false;}
 @Override public double collisionTop(BlockPos pos){double value=top(pos);if(!Double.isNaN(value))return value;value=top(pos.below());return Double.isNaN(value)?pos.getY()-1:value;}
 private double top(BlockPos pos){var shape=actor.level().getBlockState(pos).getCollisionShape(actor.level(),pos,CollisionContext.of(actor));return shape.isEmpty()?Double.NaN:pos.getY()+shape.max(Direction.Axis.Y);}
 @Override public void idleParentTick(){
  var random=actor.getRandom();actor.setXxa(0);actor.setZza(0);
  if(random.nextFloat()<.02f){var player=OriginalDespawn.closest(actor,8,false);if(player!=null){idleLookTarget=player;idleLookTicks=10+random.nextInt(20);}else idleYawVelocity=(random.nextFloat()-.5f)*20;}
  if(idleLookTarget!=null){actor.lookAt(idleLookTarget,10,actor.getMaxHeadXRot());if(idleLookTicks--<=0||idleLookTarget.isRemoved()||idleLookTarget.distanceToSqr(actor)>64)idleLookTarget=null;}
  else{if(random.nextFloat()<.05f)idleYawVelocity=(random.nextFloat()-.5f)*20;actor.setYRot(actor.getYRot()+idleYawVelocity);actor.setXRot(0);}
  int speed=actor.hasEffect(MobEffects.MOVEMENT_SPEED)?actor.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1:0,slow=actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?actor.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1:0;float combined=(speed-slow)*.2f;if(combined<0)combined*=1-EnchantmentEffects.paralysisResistance(actor);
  if((actor.isInWater()||actor.isInLava())&&combined>=-.5f&&random.nextFloat()<.8f)actor.getJumpControl().jump();
 }
}
