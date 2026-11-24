package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** tl -> EntityCubic -> og, retaining the native Magma registry, renderer and
 * synchronization. The human's three selected kill gates exclude Magma. */
public final class SourceMagmaCube extends MagmaCube implements OriginalMagmaLife.Host,OriginalParentHarmState.Flags {
 private final OriginalMagmaLife life=new OriginalMagmaLife();
 private final OriginalCubicMotion motion;
 private final OriginalParentHarmState harm;
 private boolean deferDespawn,decided,fleeing;
 public SourceMagmaCube(EntityType<? extends MagmaCube> type,Level level){
  super(type,level);getNavigation().setCanFloat(true);
  int size=1<<random.nextInt(3);motion=new OriginalCubicMotion(OriginalMagmaLife.jumpDelay(false,random::nextInt));
  moveControl=new MoveControl(this){@Override public void tick(){}};
  jumpControl=new JumpControl(this){@Override public void tick(){}};
  harm=new OriginalParentHarmState(this,this);setSize(size,true);
  getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);setPathfindingMalus(BlockPathTypes.LAVA,0);
 }
 @Override protected void registerGoals(){}
 @Override public void setSize(int requested,boolean resetHealth){super.setSize(requested,resetHealth);int size=getSize();getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).2f);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(size*2);getAttribute(Attributes.ARMOR).setBaseValue(0);xpReward=OriginalMagmaLife.experience(size);}
 @Override public EntityDimensions getDimensions(Pose pose){int size=entityData==null?1:getSize();return EntityDimensions.scalable(.5f*size,.5f*size);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 public float naturalDefense(DamageSource source){return OriginalMagmaLife.defense(getSize(),source!=null&&ClassicDefense.mundaneBypass(source));}
 @Override public int getExperienceReward(){return OriginalMagmaLife.experience(getSize());}
 @Override public boolean canBreatheUnderwater(){return true;}
 @Override public boolean isInLava(){return false;} // tl.J: parent movement never takes its lava branch.
 @Override public boolean canSwimInFluidType(net.minecraftforge.fluids.FluidType type){return type!=net.minecraftforge.common.ForgeMod.LAVA_TYPE.get()&&super.canSwimInFluidType(type);}
 @Override public void setSecondsOnFire(int seconds){}
 @Override public boolean isOnFire(){return false;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override protected void checkFallDamage(double motion,boolean grounded,BlockState state,BlockPos pos){}
 @Override public boolean canBeAffected(MobEffectInstance effect){var kind=effect.getEffect();return kind!=MobEffects.REGENERATION&&kind!=MobEffects.POISON&&kind!=MobEffects.WITHER&&super.canBeAffected(effect);}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){try(var scope=SourceSpawnInitialization.begin(this)){return ((SourceMagmaNativeParent)(Object)this).mite$magmaParentSpawn(world,difficulty,reason,data,tag);}}
 @Override public boolean checkSpawnObstruction(LevelReader world){return level().getDifficulty()!=Difficulty.PEACEFUL&&world.isUnobstructed(this)&&world.noCollision(this)&&!world.containsAnyLiquid(getBoundingBox());}
 boolean deferSourceDespawn(){return deferDespawn;}
 OriginalParentHarmState sourceHarm(){return harm;}
 @Override public void aiStep(){life.beforeParent(this);deferDespawn=!level().isClientSide;try{super.aiStep();}finally{deferDespawn=false;}}
 @Override protected void customServerAiStep(){setSpeed((float)getAttributeValue(Attributes.MOVEMENT_SPEED));motion.step(new Motion());}
 @Override public void tick(){
  if(!level().isClientSide&&level().getDifficulty()==Difficulty.PEACEFUL&&getSize()>0){discard();return;}
  if(!level().isClientSide&&harm!=null)harm.beforeTick();
  oSquish=squish;squish+=(targetSquish-squish)*.5f;boolean grounded=onGround();
  ((SourceMagmaNativeParent)(Object)this).mite$magmaParentTick();
  if(onGround()&&!grounded){for(int i=0;i<getSize()*8;i++){float angle=random.nextFloat()*(float)Math.PI*2,radius=random.nextFloat()*.5f+.5f;level().addParticle(ParticleTypes.FLAME,getX()+Mth.sin(angle)*getSize()*.5f*radius,getBoundingBox().minY,getZ()+Mth.cos(angle)*getSize()*.5f*radius,0,0,0);}if(getSize()>2)playSound(slimeSound(),getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)/.8f);targetSquish=-.5f;}else if(!onGround()&&grounded)targetSquish=1;
  targetSquish*=.6f; // The audited active virtual call is EntityCubic.alterSquishAmount.
 }
 @Override protected void jumpFromGround(){var velocity=getDeltaMovement();setDeltaMovement(velocity.x,OriginalMagmaLife.jumpVelocity(getSize()),velocity.z);hasImpulse=true;net.minecraftforge.common.ForgeHooks.onLivingJump(this);}
 private SoundEvent slimeSound(){return getSize()>1?SoundEvents.SLIME_SQUISH:SoundEvents.SLIME_SQUISH_SMALL;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return slimeSound();}
 @Override protected SoundEvent getDeathSound(){return slimeSound();}
 @Override protected SoundEvent getJumpSound(){return slimeSound();}
 @Override protected SoundEvent getSquishSound(){return slimeSound();}
 @Override protected float getSoundVolume(){return .2f*getSize();}
 /** Slime.push's modern iron-golem attack branch must never run. */
 @Override protected boolean isDealsDamage(){return false;}
 @Override protected void dealDamage(LivingEntity target){}
 public static boolean suppressPush(Object one,Object two){return one instanceof SourceMagmaCube cube&&cube.getSize()==1&&two instanceof Player||two instanceof SourceMagmaCube otherCube&&otherCube.getSize()==1&&one instanceof Player;}
 private float reachSquared(){float reach=getSize()*.6f;reach*=getSize()==1?1.1f:getSize()==2?.9f:.7f;return reach*reach;}
 @Override public void playerTouch(Player player){if(!level().isClientSide&&distanceToSqr(player)<=reachSquared()&&OriginalVision.see(this,player))doHurtTarget(player);}
 @Override public boolean doHurtTarget(Entity target){
  if(!(target instanceof Player player))return false;
  try(var frame=SourceParentHurtFrame.beginObserver(player,outcome->{if(outcome.negativelyAffected()&&!outcome.destroyed()&&random.nextInt(10)<getSize()*2)player.setSecondsOnFire(getSize()*3);})){boolean accepted=player.hurt(SourceMagmaWorld.attack(this),getSize()*2);frame.finish();return accepted;}
 }
 @Override public boolean hurt(DamageSource source,float amount){
  String name=ClassicDefense.name(source);if(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypes.LAVA)||source.is(DamageTypes.DROWN)||source.is(DamageTypes.CACTUS)||source.is(DamageTypes.WITHER)||name.equals("pepsin")||name.equals("acid"))return false;
  if(source.getDirectEntity() instanceof Snowball&&!SourceSnowballTraits.proper(source)){amount=2;if(!level().isClientSide)SourceChemicalFx.entity(this,SourceChemicalFx.ENTITY_STEAM);}
  if(level().isClientSide||harm==null)return super.hurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->harm.afterDamage(source,outcome.negativelyAffected()))){boolean accepted=super.hurt(source,amount);frame.finish();return accepted;}
 }
 @Override public void remove(RemovalReason reason){
  if(!isRemoved()&&!level().isClientSide&&getSize()>1&&getHealth()<=0){int count=2+random.nextInt(3);for(int index=0;index<count;index++){float dx=((float)(index%2)-.5f)*getSize()/4,dz=((float)(index/2)-.5f)*getSize()/4;var child=getType().create(level());if(child==null)continue;child.setSize(getSize()/2,true);child.moveTo(getX()+dx,getY()+.5,getZ()+dz,random.nextFloat()*360,0);level().addFreshEntity(child);}}
  ((SourceMagmaNativeParent)(Object)this).mite$magmaParentRemove(reason);
 }
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){int count=OriginalMagmaLife.drops(getSize(),looting,random::nextInt);for(int i=0;i<count;i++)spawnAtLocation(Items.MAGMA_CREAM);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.remove("wasOnGround");if(harm!=null)harm.save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setSize(tag.getInt("Size")+1,true);if(harm!=null)harm.load(tag);}
 @Override public boolean client(){return level().isClientSide;}@Override public boolean wet(){return isInWaterRainOrBubble();}@Override public boolean immersed(){return isInWater();}
 @Override public int nextInt(int bound){return random.nextInt(bound);}@Override public float nextFloat(){return random.nextFloat();}
 @Override public void steam(int count){if(client())SourceChemicalClientFx.particles(this,ParticleTypes.POOF,count);}
 @Override public void fizz(float volume,float pitch){playSound(SoundEvents.FIRE_EXTINGUISH,volume,pitch);}
 @Override public void waterHurt(float amount){hurt(OriginalElementalDamage.water(this),amount);}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
 private record Target(Player actor) implements OriginalCubicMotion.Target {public boolean removed(){return actor.isRemoved();}public float health(){return actor.getHealth();}public boolean player(){return true;}public boolean item(){return false;}}
 private Target closest(double range,boolean sight){Player closest=null;double best=Double.MAX_VALUE;for(var player:level().players()){if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;double limit=range;if(player.isCrouching())limit*=(double).8f;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));double distance=player.distanceToSqr(getX(),getY()+getBbHeight()/2,getZ());if(distance>=limit*limit||distance>=best||sight&&!OriginalVision.see(this,player))continue;closest=player;best=distance;}return closest==null?null:new Target(closest);}
 private final class Motion implements OriginalCubicMotion.Host {
  public boolean ooze(){return false;}public boolean gelatinous(){return false;}public boolean acidic(){return false;}public boolean magmaCube(){return true;}
  public int sourceTicksWithOffset(){return tickCount+getId()*47;}public int size(){return getSize();}public boolean onGround(){return SourceMagmaCube.this.onGround();}public boolean horizontalCollision(){return SourceMagmaCube.this.horizontalCollision;}
  public int blockFeedingCountdown(){return 0;}public boolean feeding(){return false;}public boolean seeksItems(){return false;}public void blockFeedingCountdown(int value){}public void itemFeedingCountdown(int value){}
  public void tryDespawn(){boolean prior=deferDespawn;deferDespawn=false;try{OriginalDespawn.tryDespawn(SourceMagmaCube.this);}finally{deferDespawn=prior;}}
  public Target findPlayerToAttack(float range){return closest(range,true);}public Target closestVulnerablePlayer(double range,boolean sight){return closest(range,sight);}
  public Target closestDissolvableItem(double range,boolean sight,boolean path){return null;}public Target closestPrey(double range,boolean sight,boolean path){return null;}
  public boolean intersectsTargetCenterScaledXZ(OriginalCubicMotion.Target target,double factor){throw new IllegalStateException("Magma never enters gelatinous item paths");}public boolean intersectsOwnCenterScaledXZ(OriginalCubicMotion.Target target,double factor){throw new IllegalStateException("Magma never enters gelatinous feeding paths");}
  public boolean intersectsTarget(OriginalCubicMotion.Target target){return getBoundingBox().intersects(((Target)target).actor().getBoundingBox());}public boolean collidesAfterMovingUp(double amount){return level().getBlockCollisions(SourceMagmaCube.this,getBoundingBox().move(0,amount,0)).iterator().hasNext();}
  public void lookAt(OriginalCubicMotion.Target target,float yaw,float pitch){SourceMagmaCube.this.lookAt(((Target)target).actor(),yaw,pitch);}
  public int jumpDelay(OriginalCubicMotion.Target target){return OriginalMagmaLife.jumpDelay(target!=null,random::nextInt);}public boolean makesSoundOnJump(){return getSize()>0;}public void playSourceJumpSound(){playSound(slimeSound(),getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);}public float nextFloat(){return random.nextFloat();}
  public void strafe(float value){xxa=value;}public void forward(float value){zza=value;}public void jumping(boolean value){setJumping(value);}public void contactSupportingBlocksWithAcid(){SourceMagmaWorld.acidSupport(SourceMagmaCube.this);}
 }
}
