package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** td with its on/tm caller. Native identity, renderer, data sync and the
 * questionnaire's modern navigation remain. No magical-only damage gate. */
public final class SourceBlaze extends Blaze implements OriginalBlazeLife.Host {
 private final OriginalBlazeLife sourceLife=new OriginalBlazeLife();
 private final SourceBlazeAI sourceAi;
 public SourceBlaze(EntityType<? extends Blaze> type,Level level){
  super(type,level);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).1f);
  getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);
  setPathfindingMalus(BlockPathTypes.LAVA,0);sourceAi=new SourceBlazeAI(this);goalSelector.addGoal(5,sourceAi);xpReward=20;
 }
 @Override protected void registerGoals(){}
 public SourceBlazeAI sourceAi(){return sourceAi;}
 @Override public void tick(){if(!level().isClientSide&&sourceAi!=null)sourceAi.harm.beforeTick();super.tick();}
 @Override public void aiStep(){sourceLife.beforeParent(this);if(sourceAi!=null)sourceAi.beforeParent();((SourceBlazeNativeParent)(Object)this).mite$blazeParentAiStep();}
 @Override protected void customServerAiStep(){((SourceBlazeNativeParent)(Object)this).mite$blazeParentCustomAiStep();}
 /** The source leaf already applies water damage before the parent. */
 @Override public boolean isSensitiveToWater(){return false;}
 @Override public void setSpeed(float requested){super.setSpeed(.1f);setZza(requested);}
 @Override public void setTarget(LivingEntity target){super.setTarget(target!=null&&target.isRemoved()?null:target);}
 @Override public LivingEntity getTarget(){var target=super.getTarget();if(target!=null&&target.isRemoved()){super.setTarget(null);return null;}return target;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override protected void checkFallDamage(double motion,boolean ground,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos){}
 @Override public int getExperienceReward(){return 20;}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){if(((SourceMobAi)(Object)this).mite$recentPlayerHitTicks()>0){int count=random.nextInt(2+looting);for(int i=0;i<count;i++)spawnAtLocation(Items.BLAZE_ROD);}}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.getDirectEntity() instanceof Snowball&&!SourceSnowballTraits.proper(source)){amount=3;if(!level().isClientSide)SourceChemicalFx.entity(this,SourceChemicalFx.ENTITY_STEAM);}
  if(level().isClientSide||sourceAi==null)return super.hurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->sourceAi.afterDamage(source,outcome))){boolean result=super.hurt(source,amount);frame.finish();return result;}
 }
 @Override public float getWalkTargetValue(net.minecraft.core.BlockPos pos,LevelReader world){return (SourceArachnidWorld.cameFromSpawner(this)?.7f:.5f)-SourceArachnidWorld.pointBrightness(level(),pos);}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(sourceAi!=null)sourceAi.save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(sourceAi!=null)sourceAi.load(tag);}
 // Mandatory td.c host. World-side FX routing deliberately avoids broadcasting
 // duplicate smoke/sound; server still consumes the original random draws.
 @Override public boolean client(){return level().isClientSide;}
 @Override public boolean wet(){return isInWaterRainOrBubble();}
 // Keep the Host name distinct: Forge remaps Entity.onGround to its SRG name.
 @Override public boolean blazeOnGround(){return onGround();}
 @Override public double x(){return getX();}@Override public double y(){return getY();}@Override public double z(){return getZ();}
 @Override public float width(){return getBbWidth();}@Override public float height(){return getBbHeight();}@Override public float eyeHeight(){return getEyeHeight();}
 @Override public Double targetEyeY(){var target=getTarget();return target==null?null:target.getEyeY();}
 @Override public double motionY(){return getDeltaMovement().y;}@Override public void motionY(double value){var motion=getDeltaMovement();setDeltaMovement(motion.x,value,motion.z);}
 @Override public void waterHurt(float value){hurt(OriginalElementalDamage.water(this),value);}
 @Override public int nextInt(int bound){return random.nextInt(bound);}@Override public float nextFloat(){return random.nextFloat();}@Override public double nextGaussian(){return random.nextGaussian();}@Override public double nextDouble(){return random.nextDouble();}
 @Override public void fireSound(double x,double y,double z,float volume,float pitch){if(client()&&!isSilent())level().playLocalSound(x,y,z,SoundEvents.BLAZE_BURN,getSoundSource(),volume,pitch,false);}
 @Override public void smoke(double x,double y,double z){level().addParticle(ParticleTypes.LARGE_SMOKE,x,y,z,0,0,0);}
}
