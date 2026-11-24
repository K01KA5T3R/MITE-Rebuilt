package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

/** uj flying/embedded state and uo impact, through the native small-fireball
 * identity/renderer/network packet. Forge cancellation remains an explicit
 * platform boundary. Dispenser's direct vanilla constructor is separate. */
public final class SourceSmallFireball extends SmallFireball implements SourceBlazeRaycast.Seed {
 private int airTicks,groundTicks,raycastOffset;private BlockPos tile=BlockPos.ZERO;private net.minecraft.world.level.block.Block inTile=Blocks.AIR;private boolean embedded;private Entity sourceOwner;
 public SourceSmallFireball(EntityType<? extends SmallFireball> type,Level level){super(type,level);}
 @Override public void setOwner(Entity owner){super.setOwner(owner);sourceOwner=owner;}
 /** uj retains the removed shooter reference; native UUID lookup forgets it. */
 @Override public Entity getOwner(){if(sourceOwner==null)sourceOwner=super.getOwner();return sourceOwner;}
 public SourceSmallFireball(Level level,LivingEntity owner,double dx,double dy,double dz){
  this(EntityType.SMALL_FIREBALL,level);setOwner(owner);moveTo(owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot());setDeltaMovement(Vec3.ZERO);
  dx+=random.nextGaussian()*.4;dy+=random.nextGaussian()*.4;dz+=random.nextGaussian()*.4;
  double length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);xPower=dx/length*.1;yPower=dy/length*.1;zPower=dz/length*.1;
 }
 @Override public int nextRaycastSeed(){return ++raycastOffset+getId()&32767;}
 @Override public void tick(){
  var owner=getOwner();if(!level().isClientSide&&(owner!=null&&owner.isRemoved()||!level().hasChunkAt(blockPosition()))){discard();return;}
  ((SourceFireballNativeParent)(Object)this).mite$fireballEntityTick();setSecondsOnFire(1);
  if(embedded){if(level().getBlockState(tile).is(inTile)){if(++groundTicks==600)discard();return;}embedded=false;var velocity=getDeltaMovement();setDeltaMovement(velocity.x*(double)(random.nextFloat()*.2f),velocity.y*(double)(random.nextFloat()*.2f),velocity.z*(double)(random.nextFloat()*.2f));groundTicks=0;airTicks=0;}else ++airTicks;
  var motion=getDeltaMovement();var from=position();var to=from.add(motion);var block=SourceBlazeRaycast.blocks(this,from,to,this);var limit=block==null?to:block.getLocation();HitResult hit=block;var entity=SourceBlazeRaycast.entities(this,from,limit,airTicks);
  if(entity!=null)hit=entity;
  if(hit!=null&&!ForgeEventFactory.onProjectileImpact(this,hit))impact(hit);
  setPos(getX()+motion.x,getY()+motion.y,getZ()+motion.z);
  float horizontal=(float)Math.sqrt(motion.x*motion.x+motion.z*motion.z);float yaw=(float)(Math.atan2(motion.z,motion.x)*180/Math.PI)+90,pitch=(float)(Math.atan2(horizontal,motion.y)*180/Math.PI)-90;
  while(pitch-xRotO< -180)xRotO-=360;while(pitch-xRotO>=180)xRotO+=360;while(yaw-yRotO< -180)yRotO-=360;while(yaw-yRotO>=180)yRotO+=360;
  setXRot(xRotO+(pitch-xRotO)*.2f);setYRot(yRotO+(yaw-yRotO)*.2f);
  float drag=.95f;if(isInWater()){for(int i=0;i<4;i++)level().addParticle(ParticleTypes.BUBBLE,getX()-motion.x*(double).25f,getY()-motion.y*(double).25f,getZ()-motion.z*(double).25f,motion.x,motion.y,motion.z);drag=.8f;}
  setDeltaMovement((motion.x+xPower)*(double)drag,(motion.y+yPower)*(double)drag,(motion.z+zPower)*(double)drag);level().addParticle(ParticleTypes.SMOKE,getX(),getY()+.5,getZ(),0,0,0);setPos(getX(),getY(),getZ());
 }
 private void impact(HitResult hit){
  if(level().isClientSide)return;
  if(hit instanceof EntityHitResult entity){entity.getEntity().hurt(damageSources().fireball(this,getOwner()),2);entity.getEntity().setSecondsOnFire(5);}
  else if(hit instanceof BlockHitResult block){var pos=block.getBlockPos().relative(block.getDirection());if(level().isEmptyBlock(pos)&&(!(getOwner() instanceof Mob)||ForgeEventFactory.getMobGriefingEvent(level(),getOwner())))level().setBlockAndUpdate(pos,Blocks.FIRE.defaultBlockState());}
  discard();
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putShort("xTile",(short)tile.getX());tag.putShort("yTile",(short)tile.getY());tag.putShort("zTile",(short)tile.getZ());tag.putString("SourceInTile",net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(inTile).toString());tag.putByte("inGround",(byte)(embedded?1:0));var motion=getDeltaMovement();tag.put("direction",newDoubleList(motion.x,motion.y,motion.z));tag.put("acceleration",newDoubleList(xPower,yPower,zPower));}
 @Override public void readAdditionalSaveData(CompoundTag tag){sourceOwner=null;super.readAdditionalSaveData(tag);tile=new BlockPos(tag.getShort("xTile"),tag.getShort("yTile"),tag.getShort("zTile"));var id=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("SourceInTile"));inTile=id==null?Blocks.AIR:net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);if(inTile==null)inTile=Blocks.AIR;embedded=tag.getByte("inGround")==1;airTicks=groundTicks=0;var direction=tag.getList("direction",Tag.TAG_DOUBLE);var acceleration=tag.getList("acceleration",Tag.TAG_DOUBLE);if(direction.size()==3&&acceleration.size()==3){setDeltaMovement(direction.getDouble(0),direction.getDouble(1),direction.getDouble(2));xPower=acceleration.getDouble(0);yPower=acceleration.getDouble(1);zPower=acceleration.getDouble(2);}else if(!tag.contains("power",Tag.TAG_LIST))discard();}
}
