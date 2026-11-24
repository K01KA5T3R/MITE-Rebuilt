package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.*;

/** EntityCubic/EntityGelatinousCube native owner. It inherits the source og
 * branch, represented by PathfinderMob, never modern Slime or Monster AI.
 * Hard kill predicates of Ooze/Pudding are intentionally outside user scope. */
public final class SourceGelatinousCube extends PathfinderMob implements Enemy {
 private static final EntityDataAccessor<Byte> SIZE=SynchedEntityData.defineId(SourceGelatinousCube.class,EntityDataSerializers.BYTE);
 private static final EntityDataAccessor<Boolean> FEEDING=SynchedEntityData.defineId(SourceGelatinousCube.class,EntityDataSerializers.BOOLEAN),CLIMBING=SynchedEntityData.defineId(SourceGelatinousCube.class,EntityDataSerializers.BOOLEAN);
 public float squishAmount,squishFactor,previousSquishFactor;
 private OriginalCubicMotion motion;
 private OriginalGelatinousProgress progress;
 private SourceGelatinousWorld world;
 private int nextFizz;
 private boolean deferDespawn;
 public SourceGelatinousCube(EntityType<? extends SourceGelatinousCube> type,Level level){
  super(type,level);((GroundPathNavigation)getNavigation()).setCanFloat(true);
  int size=1<<random.nextInt(3);motion=new OriginalCubicMotion(profile().jumpDelay(false,random::nextInt));
  moveControl=new MoveControl(this){@Override public void tick(){}};
  jumpControl=new JumpControl(this){@Override public void tick(){}};
  setSourceSize(size);
 }
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.FOLLOW_RANGE,16).add(Attributes.MOVEMENT_SPEED,(double).1f).add(Attributes.ATTACK_DAMAGE,2);}
 public OriginalGelatinousProfile profile(){return switch(Mobs.name(this)){case "ochre_jelly"->OriginalGelatinousProfile.JELLY;case "crimson_blob"->OriginalGelatinousProfile.BLOB;case "gray_ooze"->OriginalGelatinousProfile.OOZE;case "black_pudding"->OriginalGelatinousProfile.PUDDING;default->throw new IllegalStateException("Non-source gelatinous type "+getType());};}
 public int sourceSize(){return entityData.get(SIZE);}
 public void setSourceSize(int requested){int size=profile().admittedSize(requested);entityData.set(SIZE,(byte)size);refreshDimensions();setPos(getX(),getY(),getZ());getAttribute(Attributes.MAX_HEALTH).setBaseValue(size*size);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(profile().attackStrength(size));setHealth(getMaxHealth());}
 OriginalGelatinousProgress progress(){if(progress==null)progress=new OriginalGelatinousProgress(value->entityData.set(FEEDING,value));return progress;}
 private SourceGelatinousWorld world(){if(world==null)world=new SourceGelatinousWorld(this);return world;}
 public boolean sourceFeeding(){return level().isClientSide?entityData.get(FEEDING):progress().feeding();}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SIZE,(byte)1);entityData.define(FEEDING,false);entityData.define(CLIMBING,false);}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){if(SIZE.equals(key))refreshDimensions();super.onSyncedDataUpdated(key);}
 @Override public EntityDimensions getDimensions(Pose pose){int size=entityData==null?1:sourceSize();return EntityDimensions.scalable(.5f*size,.5f*size);}
 @Override protected void registerGoals(){}
 @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor world,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){if(reason==MobSpawnType.NATURAL||reason==MobSpawnType.CHUNK_GENERATION){double y=SourceTroopPlacement.surfaceY(world.getLevel(),blockPosition());if(!Double.isNaN(y))setPos(getX(),y,getZ());}return super.finalizeSpawn(world,difficulty,reason,data,tag);}
 @Override public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader reader){
  if(!reader.isUnobstructed(this)||!reader.noCollision(this)||reader.getBlockState(blockPosition().below()).getBlock() instanceof LeavesBlock)return false;
  var above=reader.getBlockState(blockPosition().above());if(above.getFluidState().is(net.minecraft.tags.FluidTags.WATER)||above.is(Blocks.LILY_PAD))return false;
  if(reader.getFluidState(blockPosition()).is(net.minecraft.tags.FluidTags.WATER)&&SourcePhaseNativeWorld.solid(above))return false;
  var box=getBoundingBox();for(var pos:BlockPos.betweenClosed(Mth.floor(box.minX),Mth.floor(box.minY),Mth.floor(box.minZ),Mth.ceil(box.maxX)-1,Mth.ceil(box.maxY)-1,Mth.ceil(box.maxZ)-1))if(reader.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA))return false;return true;
 }
 @Override public boolean canBreatheUnderwater(){return true;}
 @Override public int getExperienceReward(){return profile().experience(sourceSize());}
 @Override public int getMaxHeadXRot(){return 0;} // Source bp, not bv (cluster size inherits4).
 boolean deferSourceDespawn(){return deferDespawn;}
 void chemicalArrowFx(Entity arrow){SourceChemicalFx.entity(arrow,SourceChemicalFx.ENTITY_STEAM);}
 @Override public boolean onClimbable(){return entityData.get(CLIMBING);}
 @Override public void setSecondsOnFire(int seconds){} // canCatchFire=false; lava damage remains possible.
 @Override public boolean isOnFire(){return false;}
 @Override protected void customServerAiStep(){world().beforeMotion();setSpeed((float)getAttributeValue(Attributes.MOVEMENT_SPEED));motion.step(new Motion());}
 @Override public void aiStep(){
  if(!level().isClientSide){progress().beforeAi();if((tickCount+getId()*47)%20==0){progress().everySourceSecond(world(),world().occupied(),sourceSize());feedItems();}}
  deferDespawn=!level().isClientSide;try{super.aiStep();}finally{deferDespawn=false;}
 }
 @Override public void tick(){
  if(!level().isClientSide&&level().getDifficulty()==Difficulty.PEACEFUL&&sourceSize()>0){discard();return;}
  previousSquishFactor=squishFactor;squishFactor+=(squishAmount-squishFactor)*.5f;boolean grounded=onGround();super.tick();
  if(!level().isClientSide)entityData.set(CLIMBING,profile()==OriginalGelatinousProfile.OOZE&&horizontalCollision);
  if(isInLava()){if(level().isClientSide)SourceChemicalClientFx.lava(this);else if(--nextFizz<=0){playSound(SoundEvents.FIRE_EXTINGUISH,.7f,1.6f+(random.nextFloat()-random.nextFloat())*.4f);nextFizz=random.nextInt(7)+2;}}
  if(profile()==OriginalGelatinousProfile.OOZE&&grounded){squishAmount=(float)Math.sin((float)(tickCount+getId()*47)/3)*.1f;return;}
  if(sourceFeeding()){squishAmount=(float)Math.sin((float)(tickCount+getId()*47)/5)*.1f;return;}
  if(onGround()&&!grounded){for(int i=0;i<sourceSize()*8;i++){float angle=random.nextFloat()*(float)Math.PI*2,radius=random.nextFloat()*.5f+.5f;level().addParticle(new ItemParticleOption(ParticleTypes.ITEM,SourceGelatinousContent.sphere(profile().dropSubtype())),getX()+Mth.sin(angle)*sourceSize()*.5f*radius,getBoundingBox().minY,getZ()+Mth.cos(angle)*sourceSize()*.5f*radius,0,0,0);}if(sourceSize()>2)playSound(slimeSound(),getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)/.8f);squishAmount=-.5f;}else if(!onGround()&&grounded)squishAmount=1;
  squishAmount*=.6f;
 }
 @Override protected void jumpFromGround(){super.jumpFromGround();double height=(double).421f;var boost=getEffect(MobEffects.JUMP);if(boost!=null)height+=(float)(boost.getAmplifier()+1)*.1f;setDeltaMovement(getDeltaMovement().x,height,getDeltaMovement().z);}
 private SoundEvent slimeSound(){return sourceSize()>1?SoundEvents.SLIME_SQUISH:SoundEvents.SLIME_SQUISH_SMALL;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return slimeSound();}
 @Override protected SoundEvent getDeathSound(){return slimeSound();}
 @Override protected float getSoundVolume(){return .2f*sourceSize();}
 private boolean prey(Entity entity){return entity instanceof Animal&&!Mobs.name(entity).equals("hellhound")||entity instanceof Villager;}
 private float reachSquared(){float reach=sourceSize()*.6f;reach*=sourceSize()==1?1.1f:sourceSize()==2?.9f:.7f;return reach*reach;}
 @Override public void playerTouch(Player player){if(distanceToSqr(player)<=reachSquared()&&OriginalVision.see(this,player)){SourceGelatinousContact.touch(player);if(!level().isClientSide)doHurtTarget(player);}}
 @Override public void push(Entity entity){if(!level().isClientSide&&prey(entity)&&distanceToSqr(entity)<=reachSquared()&&OriginalVision.see(this,entity))doHurtTarget(entity);super.push(entity);}
 @Override public boolean doHurtTarget(Entity target){
  if(!(target instanceof Player)&&!prey(target))return false;
  float before=target instanceof LivingEntity living?living.getHealth():0;boolean affected=target.hurt(damageSources().mobAttack(this),profile().attackStrength(sourceSize()));
  if(affected&&target.isAlive()){
   if(target instanceof Player player&&SourceChemicalItems.inventory(player,profile().acidic(),.05f*sourceSize(),profile().attackStrength(sourceSize()),true))SourceChemicalFx.entity(player,SourceChemicalFx.ENTITY_STEAM);
   if(profile()==OriginalGelatinousProfile.BLOB&&target instanceof LivingEntity living&&living.getHealth()<before)living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,5));
  }
  return affected;
 }
 @Override public boolean hurt(DamageSource source,float amount){
  String name=ClassicDefense.name(source);if(source.is(DamageTypes.CACTUS)||name.equals("pepsin")||name.equals("acid")&&profile()==OriginalGelatinousProfile.PUDDING)return false;
  boolean affected=super.hurt(source,amount);
  if(!level().isClientSide&&source.getDirectEntity() instanceof Player player&&!player.getMainHandItem().isEmpty()&&SourceChemicalItems.inventoryStack(player,player.getMainHandItem(),profile().acidic(),profile().attackMultiplier()))SourceChemicalFx.entity(player,SourceChemicalFx.ENTITY_STEAM);
  return affected;
 }
 @Override public void remove(RemovalReason reason){if(!isRemoved()&&!level().isClientSide&&reason==RemovalReason.KILLED&&sourceSize()>1&&getHealth()<=0){int count=2+random.nextInt(3);for(int index=0;index<count;index++){float dx=((float)(index%2)-.5f)*sourceSize()/4,dz=((float)(index/2)-.5f)*sourceSize()/4;var child=(SourceGelatinousCube)getType().create(level());if(child==null)continue;child.setSourceSize(sourceSize()/2);child.moveTo(getX()+dx,getY()+.5,getZ()+dz,random.nextFloat()*360,0);level().addFreshEntity(child);}}super.remove(reason);}
 @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){if(sourceSize()!=1)return;int count=random.nextInt(3);if(looting>0)count+=random.nextInt(looting+1);if(count>0)spawnAtLocation(SourceGelatinousContent.sphere(profile().dropSubtype()).copyWithCount(count));}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Size",sourceSize()-1);tag.putIntArray("dissolving_blocks_info",progress().savedActive());int[] extra=progress().savedExtended();if(extra.length>0)tag.putIntArray("extended_dissolving_blocks_info",extra);world().save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setSourceSize(tag.getInt("Size")+1);world().load(tag);progress().load(tag.getIntArray("dissolving_blocks_info"),tag.getIntArray("extended_dissolving_blocks_info"));}
 private void feedItems(){boolean feeding=false;for(var item:level().getEntitiesOfClass(ItemEntity.class,getBoundingBox())){if(item.isRemoved())continue;if(item.getAge()<60)((SourceItemAge)item).mite$itemAge(60);var stack=item.getItem();if(ClassicVessels.handled(stack)){String fluid=ClassicVessels.fluid(stack);if(fluid.equals("lava")){hurt(damageSources().lava(),5);world().fx(item.blockPosition(),true,true);SourceChemicalItems.vessel(item,"stone");}else if(fluid.equals("water"))SourceChemicalItems.vessel(item,"");}var result=SourceChemicalItems.dropped(this,item,profile().acidic());if(result.affected()&&!result.destroyed())feeding=true;}progress().itemFeeding(feeding?20:0);}
 private record Target(Entity entity) implements OriginalCubicMotion.Target {
  public boolean removed(){return entity.isRemoved();}public boolean player(){return entity instanceof Player;}public boolean item(){return entity instanceof ItemEntity;}
  public float health(){if(entity instanceof LivingEntity living)return living.getHealth();if(entity instanceof ItemEntity item){var tag=new CompoundTag();item.addAdditionalSaveData(tag);return tag.getShort("Health");}return entity.isAlive()?1:0;}
 }
 private Entity entity(OriginalCubicMotion.Target target){return target==null?null:((Target)target).entity();}
 private Target wrap(Entity entity){return entity==null?null:new Target(entity);}
 private Player closestPlayer(double range,boolean sight){Player closest=null;double best=Double.MAX_VALUE;for(var player:level().players()){if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;double limit=range;if(player.isCrouching())limit*=(double).8f;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));double distance=player.distanceToSqr(getX(),getY()+getBbHeight()/2,getZ());if(distance>=limit*limit||distance>=best||sight&&!OriginalVision.see(this,player))continue;closest=player;best=distance;}return closest;}
 private final class Motion implements OriginalCubicMotion.Host {
  public boolean ooze(){return profile()==OriginalGelatinousProfile.OOZE;}public boolean gelatinous(){return true;}public boolean acidic(){return profile().acidic();}public boolean magmaCube(){return false;}
  public int sourceTicksWithOffset(){return tickCount+getId()*47;}public int size(){return sourceSize();}public boolean onGround(){return SourceGelatinousCube.this.onGround();}public boolean horizontalCollision(){return SourceGelatinousCube.this.horizontalCollision;}
  public int blockFeedingCountdown(){return progress().blockFeeding();}public boolean feeding(){return sourceFeeding();}public boolean seeksItems(){return true;}public void blockFeedingCountdown(int value){progress().blockFeeding(value);}public void itemFeedingCountdown(int value){progress().itemFeeding(value);}
  public void tryDespawn(){boolean prior=deferDespawn;deferDespawn=false;try{OriginalDespawn.tryDespawn(SourceGelatinousCube.this);}finally{deferDespawn=prior;}}
  public Target findPlayerToAttack(float range){return wrap(closestPlayer(range,true));}public Target closestVulnerablePlayer(double range,boolean sight){return wrap(closestPlayer(range,sight));}
  public Target closestDissolvableItem(double range,boolean sight,boolean path){ItemEntity best=null;double distance=Double.MAX_VALUE;for(var item:level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(range))){if(item.isRemoved()||!SourceChemicalItems.susceptible(item.getItem(),profile().acidic()))continue;var state=level().getBlockState(item.blockPosition());if(SourcePhaseNativeWorld.solid(state)&&SourceChemicalMaterials.blockTicks(state,profile().acidic())<0||sight&&!OriginalVision.see(SourceGelatinousCube.this,item))continue;double squared=distanceToSqr(item);if(squared<distance){best=item;distance=squared;}}return wrap(best);}
  public Target closestPrey(double range,boolean sight,boolean path){LivingEntity best=null;double distance=Double.MAX_VALUE;for(var candidate:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(range))){if(!candidate.isAlive()||!prey(candidate)||sight&&!OriginalVision.see(SourceGelatinousCube.this,candidate))continue;double squared=distanceToSqr(candidate);if(squared<distance){best=candidate;distance=squared;}}return wrap(best);}
  public boolean intersectsTargetCenterScaledXZ(OriginalCubicMotion.Target target,double factor){var box=entity(target).getBoundingBox();var center=box.getCenter();var scaled=new AABB(center.x-box.getXsize()*factor/2,box.minY,center.z-box.getZsize()*factor/2,center.x+box.getXsize()*factor/2,box.maxY,center.z+box.getZsize()*factor/2);return getBoundingBox().intersects(scaled);}
  public boolean intersectsOwnCenterScaledXZ(OriginalCubicMotion.Target target,double factor){var box=getBoundingBox();var center=box.getCenter();return new AABB(center.x-box.getXsize()*factor/2,box.minY,center.z-box.getZsize()*factor/2,center.x+box.getXsize()*factor/2,box.maxY,center.z+box.getZsize()*factor/2).intersects(entity(target).getBoundingBox());}
  public boolean intersectsTarget(OriginalCubicMotion.Target target){return getBoundingBox().intersects(entity(target).getBoundingBox());}
  public boolean collidesAfterMovingUp(double amount){return level().getBlockCollisions(SourceGelatinousCube.this,getBoundingBox().move(0,amount,0)).iterator().hasNext();}
  public void lookAt(OriginalCubicMotion.Target target,float yaw,float pitch){SourceGelatinousCube.this.lookAt(entity(target),yaw,pitch);}
  public int jumpDelay(OriginalCubicMotion.Target target){return profile().jumpDelay(target!=null,random::nextInt);}public boolean makesSoundOnJump(){return sourceSize()>0;}public void playSourceJumpSound(){playSound(slimeSound(),getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);}public float nextFloat(){return random.nextFloat();}
  public void strafe(float value){xxa=value;}public void forward(float value){zza=value;}public void jumping(boolean value){setJumping(value);}public void contactSupportingBlocksWithAcid(){world().acidSupport();}
 }
}
