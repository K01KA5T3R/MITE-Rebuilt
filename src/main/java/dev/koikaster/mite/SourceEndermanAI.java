package dev.koikaster.mite;
import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
/** on.bl/tm.a for tg: single target/path carrier, every-tick original stare acquisition. */
public final class SourceEndermanAI extends Goal implements OriginalParentHarmState.Flags {
 private final SourceEnderman actor;public final OriginalParentHarmState harm;private final OriginalParentFleeTransition flee;
 private boolean decided,fleeing,includeLast;private int cooldown;private ItemEntity targetItem;
 private Entity idleTarget;private int idleTicks;private float idleYaw;
 SourceEndermanAI(SourceEnderman actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);flee=new OriginalParentFleeTransition(actor,harm,OriginalParentFleeTransition.nativePath(actor,1));setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
 @Override public boolean canUse(){return !actor.sourceDecoy();}@Override public boolean canContinueToUse(){return canUse();}@Override public boolean requiresUpdateEveryTick(){return true;}
 void beforeParent(){if(cooldown>0)--cooldown;}
 public int attackCooldown(){return cooldown;}
 private Path path(){return actor.getNavigation().getPath();}
 private void path(Path route){includeLast=false;SourceArachnidPathBudget.commit(actor,route);}
 private float budget(Entity target,float range){return Math.max(4,Math.min(actor.distanceTo(target)*2,range));}
 @Override public void tick(){
  var random=actor.getRandom();float range=Math.min(OriginalTargetGoal.range(actor),24);boolean escaping=flee.beforeDecisions();int offset=actor.tickCount+actor.getId()*47;
  if(targetItem!=null&&targetItem.isRemoved())targetItem=null;
  if(!escaping){var target=actor.getTarget();if(target!=null&&!target.isAlive()){actor.setTarget(null);target=null;}
   if(target==null){target=actor.findPlayer();actor.setTarget(target);if(target!=null)path(SourceArachnidPathBudget.entity(actor,target,range));else if(random.nextFloat()<.01f){/* og.findNonPlayerToAttack returns null. */}
    if(target==null&&(targetItem==null||targetItem.isRemoved()||path()==null||path().isDone())&&actor.valuables().pickup.cooldown==0&&offset%20==0){targetItem=actor.valuables().first(range*.75f);if(targetItem!=null)path(SourceArachnidPathBudget.entity(actor,targetItem,offset%100==0?range*.75f:budget(targetItem,range*.75f)));}
   }else if(!target.isRemoved()&&lineOfStrike(target)&&cooldown<=0&&actor.distanceTo(target)<1.75f&&target.getBoundingBox().maxY>actor.getBoundingBox().minY&&target.getBoundingBox().minY<actor.getBoundingBox().maxY){if(!actor.getMainHandItem().isEmpty())actor.swing(InteractionHand.MAIN_HAND);cooldown=20;actor.doHurtTarget(target);}
  }
  var target=actor.getTarget();
  if(target!=null&&offset%8==0){var route=SourceArachnidPathBudget.entity(actor,target,offset%40==0?range:budget(target,range));if(route!=null)path(route);}
  else if(!fleeing&&path()==null&&target==null&&random.nextInt(150)==0)stroll();
  else{long stamp=SourceArachnidWorld.lastCactusTick(actor),age=stamp<1?-1:actor.level().getGameTime()-stamp;if(age>=0&&age<10&&(path()==null||path().isDone()))stroll();}
  actor.setXRot(0);actor.setJumping(false);var route=path();
  if(route!=null&&random.nextInt(fleeing?1000:100)>0&&!route.isDone())follow(route,range);
  else{if(route!=null){target=actor.findPlayer();actor.setTarget(target);if(target!=null){path(SourceArachnidPathBudget.entity(actor,target,range));idle();return;}}idle();path(null);}
 }
 private boolean lineOfStrike(Entity target){var origin=actor.position().add(0,actor.getBbHeight()*.75f,0);for(float f:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(actor,origin,target.position().add(0,target.getBbHeight()*f,0)))return true;return false;}
 private void stroll(){var r=actor.getRandom();var origin=actor.blockPosition();var selected=origin;float weight=-100;for(int i=0;i<10;i++){selected=origin.offset(r.nextInt(13)-6,r.nextInt(7)-3,r.nextInt(13)-6);weight=actor.getWalkTargetValue(selected,actor.level());}if(weight>-100)path(SourceArachnidPathBudget.block(actor,selected,10));}
 private void follow(Path route,float range){
  Vec3 point=route.getNextEntityPos(actor);double radius=actor.getBbWidth()*2f;
  while(point!=null&&horizontal(point,actor.position())<radius*radius){route.advance();if(route.isDone()){if(!includeLast)point=null;path(null);var target=actor.getTarget();if(target!=null){path(SourceArachnidPathBudget.entity(actor,target,budget(target,range)));break;}if(targetItem==null||targetItem.isRemoved()||actor.distanceTo(targetItem)>4||actor.getBoundingBox().inflate(1,.25,1).intersects(targetItem.getBoundingBox())&&actor.valuables().ray(targetItem))break;path(SourceArachnidPathBudget.entity(actor,targetItem,4));if(path()!=null)includeLast=true;break;}point=route.getNextEntityPos(actor);}
  if(point!=null){double dx=point.x-actor.getX(),dz=point.z-actor.getZ();actor.setYRot((float)(Math.atan2(dz,dx)*180/Math.PI)-90);var pos=BlockPos.containing(point);double top=SourceEndermanGeometry.collisionTop(actor,pos);if(actor.horizontalCollision&&top-actor.getBoundingBox().minY>actor.maxUpStep())actor.getJumpControl().jump();if(path()==null)actor.getMoveControl().setWantedPosition(point.x,point.y,point.z,1);}
  if(actor.horizontalCollision&&path()==null)actor.getJumpControl().jump();if(actor.getRandom().nextFloat()<.8f&&(actor.isInWater()||actor.isInLava()))actor.getJumpControl().jump();
 }
 private static double horizontal(Vec3 a,Vec3 b){double x=a.x-b.x,z=a.z-b.z;return x*x+z*z;}
 private void idle(){
  var r=actor.getRandom();actor.setXxa(0);actor.setZza(0);
  if(r.nextFloat()<.02f){var player=OriginalDespawn.closest(actor,8,false);if(player!=null){idleTarget=player;idleTicks=10+r.nextInt(20);}else idleYaw=(r.nextFloat()-.5f)*20;}
  if(idleTarget!=null){actor.lookAt(idleTarget,10,actor.getMaxHeadXRot());if(idleTicks--<=0||idleTarget.isRemoved()||idleTarget.distanceToSqr(actor)>64)idleTarget=null;}
  else{if(r.nextFloat()<.05f)idleYaw=(r.nextFloat()-.5f)*20;actor.setYRot(actor.getYRot()+idleYaw);actor.setXRot(0);}
  int speed=actor.hasEffect(MobEffects.MOVEMENT_SPEED)?actor.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1:0,slow=actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?actor.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1:0;float combined=(speed-slow)*.2f;if(combined<0)combined*=1-EnchantmentEffects.paralysisResistance(actor);if((actor.isInWater()||actor.isInLava())&&combined>=-.5f&&r.nextFloat()<.8f)actor.getJumpControl().jump();
 }
 void afterDamage(DamageSource source,SourceParentHurtFrame.Outcome outcome){harm.afterDamage(source,outcome.negativelyAffected());var responsible=source.getEntity();if(!outcome.destroyed()&&outcome.negativelyAffected()&&responsible!=actor&&responsible!=actor.getVehicle()&&responsible!=actor.getFirstPassenger()&&(responsible==null||responsible instanceof LivingEntity))actor.setTarget((LivingEntity)responsible);}
 void save(CompoundTag tag){harm.save(tag);tag.putShort("AttackTime",(short)cooldown);}
 void load(CompoundTag tag){harm.load(tag);cooldown=tag.getShort("AttackTime");}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
}
