package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** on.bl for td: bn suppresses repathing after td's ranged branch. No spider
 * prey/leap/look branch, modern goal restarts, or second target/path carrier. */
public final class SourceBlazeAI extends Goal implements OriginalParentHarmState.Flags,OriginalBlazeAttack.Host {
 private final SourceBlaze actor;private final OriginalBlazeAttack attack=new OriginalBlazeAttack();
 public final OriginalParentHarmState harm;private final OriginalParentFleeTransition flee;
 private boolean decided,fleeing,attacked;private int cooldown;
 private Entity idleTarget;private int idleTicks;private float idleYaw;
 public SourceBlazeAI(SourceBlaze actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);flee=new OriginalParentFleeTransition(actor,harm,OriginalParentFleeTransition.nativePath(actor,1));setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
 @Override public boolean canUse(){return true;}@Override public boolean canContinueToUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
 public void beforeParent(){if(cooldown>0)--cooldown;}
 private Path path(){return actor.getNavigation().getPath();}private void path(Path path){SourceArachnidPathBudget.commit(actor,path);}
 private float budget(Entity target,float range){return Math.max(4,Math.min(actor.distanceTo(target)*2,range));}
 private LivingEntity findPlayer(float range){LivingEntity closest=null;double shortest=-1;for(var player:actor.level().players()){if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;double limit=player.isCrouching()?range*(double).8f:range;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));double distance=player.distanceToSqr(actor.getX(),actor.getY()+(double)(actor.getBbHeight()/2f),actor.getZ());if(range>=0&&distance>=limit*limit||shortest!=-1&&distance>=shortest||!actor.getSensing().hasLineOfSight(player))continue;closest=player;shortest=distance;}return closest;}
 @Override public void tick(){
  attacked=false;var random=actor.getRandom();float range=Math.min(OriginalTargetGoal.range(actor),24);boolean escaping=flee.beforeDecisions();int offset=actor.tickCount+actor.getId()*47;
  if(!escaping){
   var target=actor.getTarget();if(target!=null&&!target.isAlive()){actor.setTarget(null);target=null;}
   if(target==null){if(offset%10==0){target=findPlayer(range);actor.setTarget(target);}if(target!=null)path(SourceArachnidPathBudget.entity(actor,target,range));else if(random.nextFloat()<.01f){/* og.findNonPlayerToAttack is null */}}
   else if(!target.isRemoved()&&lineOfStrike(target))attack(target,actor.distanceTo(target));
  }
  var target=actor.getTarget();
  if(!attacked&&target!=null&&offset%8==0){var route=SourceArachnidPathBudget.entity(actor,target,offset%40==0?range:budget(target,range));if(route!=null)path(route);}
  else if(!fleeing&&!attacked&&path()==null&&target==null&&random.nextInt(150)==0)stroll();
  else{long stamp=SourceArachnidWorld.lastCactusTick(actor),age=stamp<1?-1:actor.level().getGameTime()-stamp;if(age>=0&&age<10&&(path()==null||path().isDone()))stroll();}
  actor.setXRot(0);
  var path=path();if(path!=null&&random.nextInt(fleeing?1000:100)>0&&!path.isDone())follow(path,range);
  else{if(path!=null){target=findPlayer(range);actor.setTarget(target);if(target!=null){path(SourceArachnidPathBudget.entity(actor,target,range));idle();return;}}idle();path(null);}
 }
 private boolean lineOfStrike(LivingEntity target){
  var origin=actor.position().add(0,(double)(actor.getBbHeight()*.75f),0);
  if(SourceBlazeRaycast.blocks(actor,origin,target.position().add(0,(double)(target.getBbHeight()*.5f),0),null)==null)return true;
  for(float fraction:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(actor,origin,target.position().add(0,(double)(target.getBbHeight()*fraction),0)))return true;return false;
 }
 private void stroll(){var random=actor.getRandom();var origin=actor.blockPosition();var selected=origin;float weight=-100;for(int i=0;i<10;i++){selected=origin.offset(random.nextInt(13)-6,random.nextInt(7)-3,random.nextInt(13)-6);weight=actor.getWalkTargetValue(selected,actor.level());}if(weight> -100)path(SourceArachnidPathBudget.block(actor,selected,10));}
 private void follow(Path path,float range){
  Vec3 point=path.getNextEntityPos(actor);double radius=actor.getBbWidth()*2f;
  while(point!=null&&horizontal(point,actor.position())<radius*radius){path.advance();if(path.isDone()){point=null;path(null);var target=actor.getTarget();if(target!=null)path(SourceArachnidPathBudget.entity(actor,target,budget(target,range)));break;}point=path.getNextEntityPos(actor);}
  if(point!=null){double dx=point.x-actor.getX(),dz=point.z-actor.getZ();actor.setYRot((float)(Math.atan2(dz,dx)*180/Math.PI)-90);var pos=BlockPos.containing(point);var shape=actor.level().getBlockState(pos).getCollisionShape(actor.level(),pos,CollisionContext.of(actor));if(shape.isEmpty()){pos=pos.below();shape=actor.level().getBlockState(pos).getCollisionShape(actor.level(),pos,CollisionContext.of(actor));}double top=shape.isEmpty()?pos.getY():pos.getY()+shape.max(Direction.Axis.Y);if(actor.horizontalCollision&&top-actor.getBoundingBox().minY>actor.maxUpStep())actor.getJumpControl().jump();}
  if(actor.horizontalCollision&&path()==null)actor.getJumpControl().jump();if(actor.getRandom().nextFloat()<.8f&&(actor.isInWater()||actor.isInLava()))actor.getJumpControl().jump();
 }
 private static double horizontal(Vec3 a,Vec3 b){double dx=a.x-b.x,dz=a.z-b.z;return dx*dx+dz*dz;}
 private void idle(){
  var random=actor.getRandom();actor.setXxa(0);actor.setZza(0);
  if(random.nextFloat()<.02f){var player=OriginalDespawn.closest(actor,8,false);if(player!=null){idleTarget=player;idleTicks=10+random.nextInt(20);}else idleYaw=(random.nextFloat()-.5f)*20;}
  if(idleTarget!=null){actor.lookAt(idleTarget,10,actor.getMaxHeadXRot());if(idleTicks--<=0||idleTarget.isRemoved()||idleTarget.distanceToSqr(actor)>64)idleTarget=null;}
  else{if(random.nextFloat()<.05f)idleYaw=(random.nextFloat()-.5f)*20;actor.setYRot(actor.getYRot()+idleYaw);actor.setXRot(0);}
  int speed=actor.hasEffect(MobEffects.MOVEMENT_SPEED)?actor.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1:0,slow=actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?actor.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1:0;float combined=(speed-slow)*.2f;if(combined<0)combined*=1-EnchantmentEffects.paralysisResistance(actor);
  if((actor.isInWater()||actor.isInLava())&&combined>=-.5f&&random.nextFloat()<.8f)actor.getJumpControl().jump();
 }
 public void afterDamage(DamageSource source,SourceParentHurtFrame.Outcome outcome){harm.afterDamage(source,outcome.negativelyAffected());var responsible=source.getEntity();if(!outcome.destroyed()&&outcome.negativelyAffected()&&responsible!=actor&&responsible!=actor.getVehicle()&&responsible!=actor.getFirstPassenger()&&(responsible==null||responsible instanceof LivingEntity))actor.setTarget((LivingEntity)responsible);}
 public void save(CompoundTag tag){harm.save(tag);tag.putShort("AttackTime",(short)cooldown);}public void load(CompoundTag tag){harm.load(tag);cooldown=tag.getShort("AttackTime");}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
 public void attack(LivingEntity target,float distance){attack.attack(this,new Target(target),distance);}
 private record Target(LivingEntity actor) implements OriginalBlazeAttack.Target {public double x(){return actor.getX();}public double z(){return actor.getZ();}public double minY(){return actor.getBoundingBox().minY;}public double maxY(){return actor.getBoundingBox().maxY;}public float height(){return actor.getBbHeight();}public double predictedX(float lead){return SourceArachnidPrediction.at(actor,lead).x;}public double predictedZ(float lead){return SourceArachnidPrediction.at(actor,lead).z;}}
 @Override public double x(){return actor.getX();}@Override public double y(){return actor.getY();}@Override public double z(){return actor.getZ();}@Override public double minY(){return actor.getBoundingBox().minY;}@Override public double maxY(){return actor.getBoundingBox().maxY;}@Override public float height(){return actor.getBbHeight();}
 @Override public int cooldown(){return cooldown;}@Override public void cooldown(int value){cooldown=value;}@Override public float nextFloat(){return actor.getRandom().nextFloat();}@Override public double nextGaussian(){return actor.getRandom().nextGaussian();}
 @Override public void melee(OriginalBlazeAttack.Target target){actor.doHurtTarget(((Target)target).actor());}@Override public void charged(boolean value){((SourceBlazeNativeParent)(Object)actor).mite$blazeCharged(value);}
 @Override public void effect(int id,int x,int y,int z){actor.level().levelEvent(null,1018,new BlockPos(x,y,z),0);/* source1009 is modern1018: same Blaze shoot event */}
 @Override public void fireball(double dx,double dy,double dz,double spawnY){var ball=new SourceSmallFireball(actor.level(),actor,dx,dy,dz);ball.setPos(ball.getX(),spawnY,ball.getZ());actor.level().addFreshEntity(ball);}
 @Override public void yaw(float yaw){actor.setYRot(yaw);}@Override public void attacked(){attacked=true;}
}
