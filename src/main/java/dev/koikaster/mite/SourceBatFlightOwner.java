package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;

/** Native ro flight and hanging. Native runtime verification remains required. */
public abstract class SourceBatFlightOwner extends Bat {
 private static final EntityDataAccessor<Integer> HANG_Y=SynchedEntityData.defineId(SourceBatFlightOwner.class,EntityDataSerializers.INT);
 private static final int NO_ANCHOR=Integer.MIN_VALUE;
 private final OriginalBatFlight sourceFlight=new OriginalBatFlight();
 private int soundCooldown,preventHangCountdown;
 protected SourceBatFlightOwner(EntityType<? extends Bat> type,Level level){super(type,level);super.setResting(false);refreshDimensions();}
 protected float sourceScale(){return 1;}
 protected boolean sourcePredatory(){return false;}
 protected abstract void sourceParentAi();
 protected abstract void sourceCollision(Entity other);
 /** Must bypass native Bat.hurt's early wake and wake after a non-null source parent result. */
 @Override public abstract boolean hurt(DamageSource source,float amount);
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(HANG_Y,NO_ANCHOR);}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.5f*sourceScale(),.9f*sourceScale());}
 protected final Integer hangingAnchor(){int y=entityData.get(HANG_Y);return y==NO_ANCHOR?null:y;}
 private void hangingAnchor(Integer y){if(!level().isClientSide)entityData.set(HANG_Y,y==null?NO_ANCHOR:y);}
 @Override public void setResting(boolean resting){boolean previous=isResting();super.setResting(resting);if(!resting&&previous)preventHangCountdown=60;else if(resting)snapToAnchor();}
 private void snapToAnchor(){Integer y=hangingAnchor();if(y==null||!isResting())return;setDeltaMovement(Vec3.ZERO);setPos(getX(),(float)y-getBbHeight()-(sourceScale()>1?(double).1175f:(double).00625f),getZ());}
 protected final void sourceCollisionSound(){if(soundCooldown<=0){level().levelEvent(null,LevelEvent.SOUND_BAT_LIFTOFF,BlockPos.containing(getX(),getY(),getZ()),0);soundCooldown=random.nextInt(31)+30;}}
 @Override protected void pushEntities(){if(!sourcePredatory()||level().isClientSide)return;for(Entity other:level().getEntities(this,getBoundingBox().inflate((double).2f,0,(double).2f),Entity::isPushable))sourceCollision(other);}
 @Override protected void doPush(Entity other){} // ro.n has a sound/attack consumer, not native physical pushing.
 @Override protected void customServerAiStep(){sourceFlight.step(new FlightWorld());}
 @Override public void tick(){
  ((SourceBatNativeParent)this).mite$batParentTick();
  if(soundCooldown>0)--soundCooldown;if(preventHangCountdown>0)--preventHangCountdown;
  Integer y=hangingAnchor();
  if(!level().isClientSide&&hurtTime>0&&isResting()&&y!=null){setResting(false);level().levelEvent(null,LevelEvent.SOUND_BAT_LIFTOFF,BlockPos.containing(getX(),getY(),getZ()),0);setPos(getX(),(float)y-getBbHeight()-.01f,getZ());}
  // Correct native Bat's floor-based hanging snap to the original synchronized anchor.
  if(isResting())snapToAnchor();else setDeltaMovement(getDeltaMovement().multiply(1,(double).6f,1));
 }
 private Integer newAnchor(){int y=net.minecraft.util.Mth.floor(getY()+(double).12f+getBbHeight());var pos=new BlockPos(blockPosition().getX(),y,blockPosition().getZ());var state=level().getBlockState(pos);
  if(state.isAir()||state.is(Blocks.GLASS)||sourceScale()>1&&!level().isEmptyBlock(pos.below(2)))return null;
  if(!(state.isCollisionShapeFullBlock(level(),pos)||state.isFaceSturdy(level(),pos,Direction.DOWN))||!level().isEmptyBlock(pos.below()))return null;
  return y;
 }
 private boolean preventHanging(){return preventHangCountdown>0||getTarget()!=null||hurtTime>0||level().getNearestPlayer(getX(),getY(),getZ(),4,entity->entity instanceof Player player&&!player.isCreative()&&!player.isSpectator())!=null;}
 /** aci's initial upward search. The spawning scheduler decides once per pass
  * whether to call this; it does not consume another per-bat chance roll. */
 public final float sourceInitialHang(int x,int y,int z,float posX,float posY,float posZ){
  for(int dy=1;dy<16;dy++){var at=new BlockPos(x,y+dy,z);if(level().isEmptyBlock(at))continue;var state=level().getBlockState(at);
   if(state.is(Blocks.GLASS)||sourceScale()>1&&!level().isEmptyBlock(at.below(2))||!(state.isCollisionShapeFullBlock(level(),at)||state.isFaceSturdy(level(),at,Direction.DOWN))||!level().isEmptyBlock(at.below()))break;
   setPos(posX,(float)(y+dy)-getBbHeight()-.01f,posZ);hangingAnchor(y+dy);setResting(true);sourceFlight.initialHangAttempted(true);
   // aci writes its initial .01 position after a(true); ro snaps its distinct
   // normal/giant hanging offset on the following real tick.
   setPos(posX,(float)(y+dy)-getBbHeight()-.01f,posZ);return (float)getY();
  }return posY;
 }
 @Override protected float getSoundVolume(){return super.getSoundVolume()*sourceScale();}
 @Override public float getVoicePitch(){return super.getVoicePitch()/sourceScale();}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("initial_hang_attempted",sourceFlight.initialHangAttempted());Integer y=hangingAnchor();if(y!=null)tag.putInt("mite_hanging_anchor_y",y);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);sourceFlight.initialHangAttempted(tag.getBoolean("initial_hang_attempted"));hangingAnchor(tag.contains("mite_hanging_anchor_y")?tag.getInt("mite_hanging_anchor_y"):null);}
 private final class FlightWorld implements OriginalBatFlight.World {
  public void parentAi(){sourceParentAi();}public boolean resting(){return isResting();}public void resting(boolean value){setResting(value);}
  public Integer newHangingY(){return newAnchor();}public void hangingY(Integer y){hangingAnchor(y);}public void wakeSound(){level().levelEvent(null,LevelEvent.SOUND_BAT_LIFTOFF,BlockPos.containing(getX(),getY(),getZ()),0);}
  public boolean preventedFromHanging(){return preventHanging();}public void unhangPosition(int y){setPos(getX(),(float)y-getBbHeight()-.01f,getZ());}
  public double x(){return getX();}public double y(){return getY();}public double z(){return getZ();}public int blockX(){return blockPosition().getX();}public int blockZ(){return blockPosition().getZ();}
  public int minimumFlightY(){return level().getMinBuildHeight()+1;}public boolean air(int x,int y,int z){return level().isEmptyBlock(new BlockPos(x,y,z));}public float scale(){return sourceScale();}
  public OriginalBatFlight.Point targetEyes(){LivingEntity target=getTarget();return target==null?null:new OriginalBatFlight.Point(target.getX(),target.getEyeY(),target.getZ());}
  public int ticksWithOffset(){return tickCount+getId()*47;}public int nextInt(int bound){return random.nextInt(bound);}public float nextFloat(){return random.nextFloat();}
  public double motionX(){return getDeltaMovement().x;}public double motionY(){return getDeltaMovement().y;}public double motionZ(){return getDeltaMovement().z;}public void motion(double x,double y,double z){setDeltaMovement(x,y,z);}
  public float yaw(){return getYRot();}public void yaw(float value){setYRot(value);}public void headYaw(float value){setYHeadRot(value);}public void forward(float value){zza=value;}
 }
}
