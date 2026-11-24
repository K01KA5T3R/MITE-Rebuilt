package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Items;
/** EntityArachnid's web stock, post-tick ranged branch and final dropFewItems method. */
public final class SourceArachnidState {
 public enum Species {NORMAL,CAVE,WOOD,DEMON,PHASE,BLACK_WIDOW}
 private final Mob actor;private final Species species;public int numWebs,attackCooldown;public boolean fleeing,hasDecidedToFlee,installed;public final SourceParentArachnidLife parentLife;
 public SourceArachnidState(Mob actor,Species species){
  this.actor=actor;this.species=species;
  if(!actor.level().isClientSide&&species!=Species.PHASE){numWebs=actor.getRandom().nextInt(4);if(numWebs>0&&species!=Species.CAVE&&species!=Species.DEMON)--numWebs;}
  parentLife=new SourceParentArachnidLife((PathfinderMob)actor,this);
 }
 public Species species(){return species;}
 public void save(CompoundTag tag){tag.putByte("num_webs",(byte)numWebs);tag.putShort("AttackTime",(short)attackCooldown);parentLife.save(tag);}
 public void load(CompoundTag tag){numWebs=tag.getByte("num_webs");attackCooldown=tag.getShort("AttackTime");parentLife.load(tag);}
 public int period(){return species==Species.CAVE||species==Species.DEMON?200:500;}
 /** Invoke after the actual owner's tick(), once per game tick. Peaceful/climb parent branches remain separate. */
 public void afterTick(){
  if(actor.level().isClientSide||numWebs<=0||(actor.tickCount+actor.getId()*47)%period()!=0)return;
  var target=actor.getTarget();if(target==null||actor.distanceTo(target)>8)return;
  var origin=actor.getEyePosition();var end=target.getEyePosition();var block=SourceWebRaycast.blocks(actor,origin,end);
  if(block!=null){end=target.position().add(0,target.getBbHeight()*.25f,0);block=SourceWebRaycast.blocks(actor,origin,end);}
  if(block!=null)return;var entity=SourceWebRaycast.entities(actor,origin,end,hit->true);
  if(entity!=null&&entity.getEntity()==target)shoot(target);
 }
 public SourceWebProjectile shoot(LivingEntity target){
  var web=new SourceWebProjectile(actor.level(),actor);var predicted=SourceArachnidPrediction.at(target,10);
  double dx=predicted.x-actor.getX(),dy=target.getY()+(double)target.getEyeHeight()-(double)1.1f-web.getY(),dz=predicted.z-actor.getZ();
  float arc=(float)Math.sqrt(dx*dx+dz*dz)*.2f;web.shootSource(dx,dy+(double)arc,dz,.8f,0);
  actor.playSound(SoundEvents.ARROW_SHOOT,1,1f/(actor.getRandom().nextFloat()*.4f+.8f));actor.level().addFreshEntity(web);
  if(species==Species.DEMON||actor.isOnFire())web.setSecondsOnFire(10);--numWebs;return web;
 }
 /** Replace native Spider loot, then leave contained/equipment death callers to their own source owners. */
 public void dropFewItems(boolean recentlyHitByPlayer,int butcheringModifier){
  if(recentlyHitByPlayer)while(numWebs-->0)actor.spawnAtLocation(Items.STRING);
  if(recentlyHitByPlayer&&(actor.getRandom().nextInt(3)==0||actor.getRandom().nextInt(1+butcheringModifier)>0))actor.spawnAtLocation(Items.SPIDER_EYE);
 }
}
