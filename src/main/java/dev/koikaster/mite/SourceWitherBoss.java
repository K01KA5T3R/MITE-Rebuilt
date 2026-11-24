package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.entity.PartEntity;

/** sm combat owner. Native Wither aiStep remains flight/pose/particles only;
 * its custom server combat leaf and damage/loot branches never run. */
public final class SourceWitherBoss extends WitherBoss {
 private static final EntityDataAccessor<Integer> RITUAL_POSTURE=SynchedEntityData.defineId(SourceWitherBoss.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Long> RITUAL_MOVE_START=SynchedEntityData.defineId(SourceWitherBoss.class,EntityDataSerializers.LONG);
 private static final EntityDataAccessor<Integer> RITUAL_HEADS=SynchedEntityData.defineId(SourceWitherBoss.class,EntityDataSerializers.INT);
 private static final int RITUAL_FLAG=4;
 private static final int EXPOSED_FLAG=8;
 private final SourceWitherBrain source;
 private final WitherSideHead[] heads;
 private WitherRitual ritual;
 private WitherRitual inactiveRitual;
 private float jaw, previousJaw;
 public SourceWitherBoss(EntityType<? extends WitherBoss> type,Level level){super(type,level);source=new SourceWitherBrain(this);heads=new WitherSideHead[]{new WitherSideHead(this,1),new WitherSideHead(this,2)};setId(getId());SourceWitherGoals.register(this);refreshDimensions();}
 public SourceWitherBrain sourceBrain(){return source;}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(RITUAL_POSTURE,WitherCombat.IDLE);entityData.define(RITUAL_MOVE_START,0L);entityData.define(RITUAL_HEADS,0);}
 int ritualPosture(){return entityData.get(RITUAL_POSTURE);}
 void setRitualPosture(int posture){entityData.set(RITUAL_MOVE_START,level().getGameTime());entityData.set(RITUAL_POSTURE,posture);}
 long ritualMoveAge(){return Math.max(0,level().getGameTime()-entityData.get(RITUAL_MOVE_START));}
 void syncRitualHeads(int broken,boolean exposed){entityData.set(RITUAL_HEADS,ritual==null?0:RITUAL_FLAG|broken|(exposed?EXPOSED_FLAG:0));}
 boolean ritualHeadBroken(int head){return (entityData.get(RITUAL_HEADS)&1<<(head-1))!=0;}
 boolean ritualHeadTargetable(int head){return (entityData.get(RITUAL_HEADS)&EXPOSED_FLAG)!=0&&isAlive()
     &&getInvulnerableTicks()==0&&!ritualHeadBroken(head);}
 @Override public boolean isPowered(){
  if(entityData==null||!entityData.hasItem(RITUAL_HEADS))return super.isPowered();
  int status=entityData.get(RITUAL_HEADS);
  return (status&RITUAL_FLAG)==0?super.isPowered():(status&EXPOSED_FLAG)!=0&&(status&3)!=3;
 }
 boolean hurtSideHead(int head,DamageSource damage,float amount){
  return ritual!=null&&ritualHeadTargetable(head)&&source.hurt(damage,amount,head);
 }
 @Override public boolean isMultipartEntity(){return true;}
 @Override public PartEntity<?>[] getParts(){return heads;}
 @Override public void setId(int id){super.setId(id);if(heads!=null)for(int i=0;i<heads.length;i++)heads[i].setId(id+i+1);}
 WitherRitual ritual(){return ritual;}
 boolean hasInactiveRitual(){return inactiveRitual!=null;}
 @Override protected void registerGoals(){}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.9f,4);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height/8f;}
 @Override public void tick(){
  if(level().isClientSide){
   previousJaw=jaw;
   jaw=Mth.approach(jaw,WitherCombat.jawOpening(ritualPosture(),ritualMoveAge()),.16f);
  }else if(source!=null)source.harm.beforeTick();
  super.tick();
  if(heads!=null)for(int i=0;i<heads.length;i++)heads[i].align(source.headPosition(i+1));
 }
 float ritualJaw(float partial){return Mth.lerp(Mth.clamp(partial,0,1),previousJaw,jaw);}
 @Override protected void customServerAiStep(){
  if(source==null)return;
  if(level() instanceof ServerLevel server&&!ritualTick(server))return;
  int invulnerable=getInvulnerableTicks();
  source.step();
  if(ritual!=null&&level() instanceof ServerLevel server){
   if(invulnerable>0)setHealth(getMaxHealth()*(1-getInvulnerableTicks()/330f));
   boolean openingAttack=invulnerable>0&&invulnerable<=WitherRitual.WARNING_TICKS;
   ritual.attackLock(server,this,openingAttack||ritual.combat.attacking(ritual,this));
  }
 }
 void beginRitual(WitherRitual value){ritual=value;setNoGravity(true);ritual.applyHealth(this);syncRitualHeads(0,false);}
 boolean ritualTick(ServerLevel server){return ritual==null||ritual.tick(server,this);}
 @Override public void die(DamageSource damage){
  super.die(damage);
  if(ritual!=null&&!isAlive())ritual.stop(this);
 }
 @Override public void onRemovedFromWorld(){
  // Forge can stop tracking an entity before its chunk is saved.
  if(ritual!=null)ritual.stop(this,true);
  super.onRemovedFromWorld();
 }
 @Override public void makeInvulnerable(){SourceWitherRules.start(source);}
 @Override public boolean hurt(DamageSource damage,float amount){return source!=null&&source.hurt(damage,amount);}
 @Override public void performRangedAttack(LivingEntity target,float distance){if(ritual==null)source.shoot(0,target);}
 @Override public boolean canBreatheUnderwater(){return true;}
 @Override public boolean canDrownInFluidType(net.minecraftforge.fluids.FluidType type){return false;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){
  spawnAtLocation(Items.NETHER_STAR);
  if(ritual!=null&&recent&&level() instanceof ServerLevel server){
   spawnAtLocation(new ItemStack(Items.BLAZE_POWDER,2));
   BossTrophies.first(server,this,BossMusicTrack.WITHER,WitherRitual.TROPHY);
  }
 }
 @Override public int getExperienceReward(){return 50;}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof net.minecraft.world.entity.player.Player)OriginalDespawn.refresh(this,-1200);return seen;}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(source!=null)source.harm.save(tag);if(ritual!=null)ritual.save(tag);else if(inactiveRitual!=null)inactiveRitual.save(tag);tag.putBoolean("R195WitherOwner",true);}
 @Override public void readAdditionalSaveData(CompoundTag tag){
  super.readAdditionalSaveData(tag);
  if(source!=null)source.harm.load(tag);
  if(ritual!=null)ritual.stop(this);
  var settings=WorldContentMode.settings(level());
  var saved=level().isClientSide||!settings.rebuiltBosses()?null:WitherRitual.load(tag);
  ritual=settings.rebuiltBossesEnabled()?saved:null;
  inactiveRitual=settings.rebuiltBossesEnabled()?null:saved;
  if(ritual!=null){
   setNoGravity(true);
   ritual.applyHealth(this);
   syncRitualHeads(ritual.combat.brokenHeads(),ritual.combat.headsExposed());
   if(ritual.combat.isStaggered())setRitualPosture(WitherCombat.STAGGERED);
  }
 }
}
