package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

/** R195 um/uj, native large-fireball identity and renderer. No native projectile tick/impact. */
public final class SourceLargeFireball extends LargeFireball {
 private int airTicks,groundTicks,raycastOffset;
 private BlockPos tile=BlockPos.ZERO;
 private net.minecraft.world.level.block.Block inTile=Blocks.AIR;
 private boolean embedded;private Entity sourceOwner;
 int sourceExplosionPower=1;
 public SourceLargeFireball(EntityType<? extends LargeFireball> type,Level level){super(type,level);}
 @Override public void setOwner(Entity owner){super.setOwner(owner);sourceOwner=owner;}
 @Override public Entity getOwner(){if(sourceOwner==null)sourceOwner=super.getOwner();return sourceOwner;}
 public SourceLargeFireball(Level level,LivingEntity owner,Vec3 target,float initialDistance,int power){
  this(EntityType.FIREBALL,level);setOwner(owner);var origin=owner.position().add(0,owner.getBbHeight()*.5f,0);var delta=target.subtract(origin);float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90;float pitch=(float)(-Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))*180/Math.PI);moveTo(origin.x,origin.y,origin.z,yaw,pitch);setDeltaMovement(Vec3.ZERO);sourceExplosionPower=power;setSourceTrajectory(target.subtract(position().add(0,getBbHeight()*.5f,0)),initialDistance,.1f);
 }
 public void setSourceTrajectory(Vec3 trajectory,float initialDistance,float wander){
  if(wander>0)trajectory=new Vec3(trajectory.x*(double)(1f+(random.nextFloat()-random.nextFloat())*wander),trajectory.y*(double)(1f+(random.nextFloat()-random.nextFloat())*wander),trajectory.z*(double)(1f+(random.nextFloat()-random.nextFloat())*wander));
  var unit=trajectory.normalize();xPower=unit.x*.1;yPower=unit.y*.1;zPower=unit.z*.1;setPos(position().add(unit.scale(initialDistance)));
 }
 int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}
 @Override public boolean isPickable(){return !isRemoved();}
 @Override public float getPickRadius(){return 1;}
 @Override public boolean isInvulnerableTo(DamageSource source){return isInvulnerable()||source.is(DamageTypeTags.IS_FIRE)||super.isInvulnerableTo(source);}
 @Override public boolean hurt(DamageSource source,float amount){
  if(isRemoved()||isInvulnerableTo(source)||!(source.getEntity() instanceof LivingEntity responsible))return false;
  applyReversal(responsible,responsible.getViewVector(1));markHurt();hasImpulse=true;hurtMarked=true;
  if(!level().isClientSide)SourceFireballReversal058.send(this,responsible,getDeltaMovement());return true;
 }
 void applyReversal(Entity owner,Vec3 motion){setDeltaMovement(motion);xPower=motion.x*.1;yPower=motion.y*.1;zPower=motion.z*.1;setOwner(owner);}
 @Override public void tick(){
  var owner=getOwner();if(!level().isClientSide&&(owner!=null&&owner.isRemoved()||!level().hasChunkAt(blockPosition()))){discard();return;}
  ((SourceFireballNativeParent)(Object)this).mite$fireballEntityTick();setSecondsOnFire(1);
  if(embedded){if(level().getBlockState(tile).is(inTile)){if(++groundTicks==600)discard();return;}embedded=false;var velocity=getDeltaMovement();setDeltaMovement(velocity.x*(double)(random.nextFloat()*.2f),velocity.y*(double)(random.nextFloat()*.2f),velocity.z*(double)(random.nextFloat()*.2f));groundTicks=0;airTicks=0;}else ++airTicks;
  var motion=getDeltaMovement();var from=position();var to=from.add(motion);var block=SourceLargeFireballRaycast.blocks(this,from,to);var limit=block==null?to:block.getLocation();HitResult hit=block;var entity=SourceLargeFireballRaycast.entities(this,from,limit,airTicks);if(entity!=null)hit=entity;
  if(hit!=null&&!ForgeEventFactory.onProjectileImpact(this,hit))onHit(hit);
  setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
  float horizontal=(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z);float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)+90,pitch=(float)(Math.atan2(horizontal,motion.y)*180/Math.PI)-90;
  while(pitch-xRotO< -180)xRotO-=360;while(pitch-xRotO>=180)xRotO+=360;while(yaw-yRotO< -180)yRotO-=360;while(yaw-yRotO>=180)yRotO+=360;
  setXRot(xRotO+(pitch-xRotO)*.2f);setYRot(yRotO+(yaw-yRotO)*.2f);float drag=.95f;
  if(isInWater()){for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,getX()-motion.x*(double).25f,getY()-motion.y*(double).25f,getZ()-motion.z*(double).25f,motion.x,motion.y,motion.z);drag=.8f;}
  setDeltaMovement((motion.x+xPower)*(double)drag,(motion.y+yPower)*(double)drag,(motion.z+zPower)*(double)drag);level().addParticle(ParticleTypes.SMOKE,getX(),getY()+.5,getZ(),0,0,0);setPos(getX(),getY(),getZ());
 }
 @Override protected void onHit(HitResult hit){
  if(!(level() instanceof ServerLevel world))return;
  if(hit instanceof EntityHitResult entity)entity.getEntity().hurt(damageSources().fireball(this,getOwner()),6);
  SourceExplosion.create(world,null,getX(),getY(),getZ(),(float)sourceExplosionPower,(float)sourceExplosionPower,true,world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));discard();
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ExplosionPower",sourceExplosionPower);tag.putShort("xTile",(short)tile.getX());tag.putShort("yTile",(short)tile.getY());tag.putShort("zTile",(short)tile.getZ());tag.putString("SourceInTile",net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(inTile).toString());tag.putByte("inGround",(byte)(embedded?1:0));var motion=getDeltaMovement();tag.put("direction",newDoubleList(motion.x,motion.y,motion.z));tag.put("acceleration",newDoubleList(xPower,yPower,zPower));}
 @Override public void readAdditionalSaveData(CompoundTag tag){sourceOwner=null;super.readAdditionalSaveData(tag);if(tag.contains("ExplosionPower"))sourceExplosionPower=tag.getInt("ExplosionPower");tile=new BlockPos(tag.getShort("xTile"),tag.getShort("yTile"),tag.getShort("zTile"));var id=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("SourceInTile"));inTile=id==null?Blocks.AIR:net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);if(inTile==null)inTile=Blocks.AIR;embedded=tag.getByte("inGround")==1;airTicks=groundTicks=0;var direction=tag.getList("direction",Tag.TAG_DOUBLE);var acceleration=tag.getList("acceleration",Tag.TAG_DOUBLE);if(direction.size()==3&&acceleration.size()==3){setDeltaMovement(direction.getDouble(0),direction.getDouble(1),direction.getDouble(2));xPower=acceleration.getDouble(0);yPower=acceleration.getDouble(1);zPower=acceleration.getDouble(2);}else if(!tag.contains("power",Tag.TAG_LIST))discard();}
}
