package dev.koikaster.mite;

import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** on.bl/bK and og.bl around human73A's native path/movement transport.
 * The actor owns one Entity target; Navigation owns one committed Path. */
public final class SourcePigmanAI extends Goal implements OriginalParentHarmState.Flags {
 private final SourcePigman actor;public final OriginalParentHarmState harm;
 private final OriginalParentFleeTransition flee;
 private boolean decided,fleeing,includeLast;private int cooldown;
 private ItemEntity targetItem;private Entity idleTarget;private int idleTicks;private float idleYaw;
 SourcePigmanAI(SourcePigman actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);flee=new OriginalParentFleeTransition(actor,harm,new OriginalParentFleeTransition.PathOwner(){public Path path(){return SourcePigmanAI.this.path();}public void path(Path value){SourcePigmanAI.this.path(value);}});setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.TARGET));}
 @Override public boolean canUse(){return true;}@Override public boolean canContinueToUse(){return true;}@Override public boolean requiresUpdateEveryTick(){return true;}
 void beforeParent(){if(cooldown>0)--cooldown;}
 public int attackCooldown(){return cooldown;}void loadCooldown(short value){cooldown=value;}
 private Path path(){return actor.getNavigation().getPath();}
 private void path(Path value){includeLast=false;SourceArachnidPathBudget.commit(actor,value);if(value!=null)actor.getNavigation().setSpeedModifier(SourceMobFrenzy.blood(actor)?1.2:1);}
 private Path route(Entity value,float range){return SourceArachnidPathBudget.entity(actor,value,range);}
 private float budget(Entity target,float range){return Math.max(4,Math.min(actor.distanceTo(target)*2,range));}
 private Entity find(float range){return (Entity)actor.sourceLife().findPlayer(actor,range);}
 public Player closestPlayer(float range){
  Player nearest=null;double shortest=-1;
  for(var player:actor.level().players()){
   if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;
   double limit=range;if(player.isCrouching())limit*=(double).8f;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));
   double distance=player.distanceToSqr(actor.getX(),actor.getY()+(double)(actor.getBbHeight()/2f),actor.getZ());
   if(range>=0&&distance>=limit*limit||shortest!=-1&&distance>=shortest||!actor.hasLineOfSight(player))continue;nearest=player;shortest=distance;
  }return nearest;
 }
 private boolean strikeLine(Entity target){var origin=actor.position().add(0,actor.getBbHeight()*.75f,0);for(float f:new float[]{.5f,.9f,.1f})if(OriginalRaycast.physicalLine(actor,origin,target.position().add(0,target.getBbHeight()*f,0)))return true;return false;}
 @Override public void tick(){
  var random=actor.getRandom();float range=Math.min(OriginalTargetGoal.range(actor),24);boolean escaping=flee.beforeDecisions();int offset=actor.tickCount+actor.getId()*47;
  if(targetItem!=null&&targetItem.isRemoved())targetItem=null;
  if(!escaping){
   var target=actor.sourceRawTarget();if(target!=null&&(target.isRemoved()||target instanceof LivingEntity living&&living.getHealth()<=0)){actor.sourceRawTarget(null);target=null;}
   if(target==null){
    if(offset%10==0){target=find(range);actor.sourceRawTarget(target);}
    if(target!=null)path(route(target,range));else if(random.nextFloat()<.01f){/* og.findNonPlayerToAttack returns null. */}
    if(target==null&&(targetItem==null||targetItem.isRemoved()||path()==null||path().isDone())&&actor.sourcePickup().state.cooldown==0&&offset%20==0){targetItem=actor.sourcePickup().first(range*.75f);if(targetItem!=null)path(route(targetItem,offset%100==0?range*.75f:budget(targetItem,range*.75f)));}
   }else if(target.isAlive()){
    if(strikeLine(target)&&cooldown<=0&&actor.distanceTo(target)<1.75f&&target.getBoundingBox().maxY>actor.getBoundingBox().minY&&target.getBoundingBox().minY<actor.getBoundingBox().maxY){if(!actor.getMainHandItem().isEmpty())actor.swing(InteractionHand.MAIN_HAND);cooldown=20;actor.doHurtTarget(target);}
   }else actor.sourceRawTarget(null);
  }
  var target=actor.sourceRawTarget();
  if(target!=null&&offset%8==0){var value=route(target,offset%40==0?range:budget(target,range));if(value!=null)path(value);}
  else if(!fleeing&&path()==null&&target==null&&random.nextInt(150)==0)stroll();
  else{long stamp=SourceArachnidWorld.lastCactusTick(actor),age=stamp<1?-1:actor.level().getGameTime()-stamp;if(age>=0&&age<10&&(path()==null||path().isDone()))stroll();}
  actor.setXRot(0);actor.setJumping(false);var value=path();
  if(value!=null&&random.nextInt(fleeing?1000:100)>0&&!value.isDone())follow(value,range);
  else{
   if(value!=null){target=find(range);actor.sourceRawTarget(target);if(target!=null){path(route(target,range));idle();return;}}
   idle();path(null);
  }
 }
 private void stroll(){var random=actor.getRandom();var origin=actor.blockPosition();var selected=origin;float weight=-100;for(int n=0;n<10;n++){selected=origin.offset(random.nextInt(13)-6,random.nextInt(7)-3,random.nextInt(13)-6);weight=actor.getWalkTargetValue(selected,actor.level());}if(weight> -100)path(SourceArachnidPathBudget.block(actor,selected,10));}
 private void follow(Path value,float range){
  var point=value.getNextEntityPos(actor);double radius=actor.getBbWidth()*2f;
  while(point!=null&&horizontal(point,actor.position())<radius*radius){
   value.advance();
   if(value.isDone()){
    if(!includeLast)point=null;path(null);var target=actor.sourceRawTarget();
    if(target!=null){path(route(target,budget(target,range)));break;}
    if(targetItem==null||targetItem.isRemoved()||actor.distanceTo(targetItem)>4||actor.getBoundingBox().inflate(1,.25,1).intersects(targetItem.getBoundingBox())&&actor.sourcePickup().ray(targetItem))break;
    path(route(targetItem,4));if(path()!=null)includeLast=true;break;
   }point=value.getNextEntityPos(actor);
  }
  if(point!=null){actor.setYRot((float)(Math.atan2(point.z-actor.getZ(),point.x-actor.getX())*180/Math.PI)-90);double rise=collisionTop(BlockPos.containing(point))-actor.getBoundingBox().minY;if(actor.horizontalCollision&&rise>actor.maxUpStep())actor.getJumpControl().jump();}
  if(actor.horizontalCollision&&path()==null)actor.getJumpControl().jump();if(actor.getRandom().nextFloat()<.8f&&(actor.isInWater()||actor.isInLava()))actor.getJumpControl().jump();
 }
 private double collisionTop(BlockPos pos){var shape=actor.level().getBlockState(pos).getCollisionShape(actor.level(),pos,CollisionContext.of(actor));if(!shape.isEmpty())return pos.getY()+shape.max(Direction.Axis.Y);var below=pos.below();shape=actor.level().getBlockState(below).getCollisionShape(actor.level(),below,CollisionContext.of(actor));return shape.isEmpty()?below.getY():below.getY()+shape.max(Direction.Axis.Y);}
 private static double horizontal(Vec3 a,Vec3 b){double x=a.x-b.x,z=a.z-b.z;return x*x+z*z;}
 private void idle(){
  var random=actor.getRandom();actor.setXxa(0);actor.setZza(0);
  if(random.nextFloat()<.02f){var player=OriginalDespawn.closest(actor,8,false);if(player!=null){idleTarget=player;idleTicks=10+random.nextInt(20);}else idleYaw=(random.nextFloat()-.5f)*20;}
  if(idleTarget!=null){actor.lookAt(idleTarget,10,actor.getMaxHeadXRot());if(idleTicks--<=0||idleTarget.isRemoved()||idleTarget.distanceToSqr(actor)>64)idleTarget=null;}
  else{if(random.nextFloat()<.05f)idleYaw=(random.nextFloat()-.5f)*20;actor.setYRot(actor.getYRot()+idleYaw);actor.setXRot(0);}
  int speed=actor.hasEffect(MobEffects.MOVEMENT_SPEED)?actor.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1:0,slow=actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?actor.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1:0;float combined=slow*-.2f+speed*.2f;if(combined<0)combined*=1-EnchantmentEffects.paralysisResistance(actor);
  if((actor.isInWater()||actor.isInLava())&&combined>=-.5f){if(random.nextFloat()<.8f)actor.getJumpControl().jump();}
 }
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
}
