package dev.koikaster.mite;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Native villager identity is a facade over the same tw state. Native private
 * conversion time is never loaded or ticked; source conversion is authoritative. */
public final class SourceZombieVillager extends ZombieVillager implements SourceZombieOwner {
 private static final EntityDataAccessor<Boolean> VILLAGER=SynchedEntityData.defineId(SourceZombieVillager.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Boolean> CONVERTING=SynchedEntityData.defineId(SourceZombieVillager.class,EntityDataSerializers.BOOLEAN);
 private final SourceZombieBody source;
 public SourceZombieVillager(EntityType<? extends ZombieVillager> type,Level level){super(type,level);sourceProfession(0);source=new SourceZombieBody(this,this);source.goals();refreshDimensions();}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(VILLAGER,false);entityData.define(CONVERTING,false);}@Override protected void registerGoals(){}
 @Override public SourceZombieBody sourceZombie(){return source;}@Override public boolean sourceVillager(){return entityData.get(VILLAGER);}@Override public void sourceVillager(boolean value){entityData.set(VILLAGER,value);}@Override public int sourceProfession(){return SourceZombieBody.profession(getVillagerData().getProfession());}@Override public void sourceProfession(int value){setVillagerData(getVillagerData().setProfession(SourceZombieBody.profession(value)).setLevel(1));}@Override public boolean sourceConverting(){return entityData.get(CONVERTING);}@Override public void sourceConverting(boolean value){entityData.set(CONVERTING,value);}@Override public boolean isConverting(){return sourceConverting();}@Override public float sourceDropChance(int slot){return getEquipmentDropChance(SourcePigmanGear.slot(slot));}
 @Override public void setVillagerData(VillagerData value){super.setVillagerData(value);sourceVillager(true);}
 @Override public boolean isBaby(){return false;}@Override public void setBaby(boolean ignored){super.setBaby(false);}@Override protected boolean supportsBreakDoorGoal(){return false;}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f,1.8f);}@Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public boolean canBeAffected(MobEffectInstance effect){var kind=effect.getEffect();return kind!=MobEffects.REGENERATION&&kind!=MobEffects.POISON&&kind!=MobEffects.WITHER&&super.canBeAffected(effect);}
 @Override public void tick(){if(source!=null)source.beforeTick();((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterTick();}
 @Override public void aiStep(){if(source!=null)source.beforeAi();((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterAiStep();if(source!=null)source.afterAi();}
 @Override public boolean wantsToPickUp(net.minecraft.world.item.ItemStack stack){return false;}@Override public boolean hurt(DamageSource damage,float amount){return source==null?super.hurt(damage,amount):source.hurt(damage,amount);}@Override public boolean doHurtTarget(Entity target){return SourceMobStrikeNative.attack(this,target);}
 @Override public boolean killedEntity(ServerLevel level,LivingEntity victim){return SourceZombieKills.killed(this,level,victim,((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterKilledEntity(level,victim));}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}@Override protected void dropCustomDeathLoot(DamageSource damage,int looting,boolean recent){source.deathLoot(recent,looting);}@Override protected void dropEquipment(){}@Override public int getExperienceReward(){return 15;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){return source.initialize(world,reason,data);}
 @Override public InteractionResult mobInteract(Player player,InteractionHand hand){return source.interact(player,hand);}@Override public void handleEntityEvent(byte event){if(source==null||!source.handleEvent(event))super.handleEntityEvent(event);}
 @Override public void addAdditionalSaveData(CompoundTag tag){((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterSave(tag);VillagerData.CODEC.encodeStart(NbtOps.INSTANCE,getVillagerData()).result().ifPresent(data->tag.put("VillagerData",data));source.save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterLoad(tag);if(tag.contains("VillagerData",Tag.TAG_COMPOUND))VillagerData.CODEC.parse(NbtOps.INSTANCE,tag.get("VillagerData")).result().ifPresent(this::setVillagerData);sourceProfession(sourceProfession());source.load(tag);}
 @Override public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.phys.Vec3 factor){super.makeStuckInBlock(state,factor);if(source!=null)source.web(state);}
 @Override public void die(DamageSource damage){if(source!=null)source.digging.cancel();super.die(damage);}
 @Override public SoundEvent getAmbientSound(){return SoundEvents.ZOMBIE_AMBIENT;}@Override public SoundEvent getHurtSound(DamageSource source){return SoundEvents.ZOMBIE_HURT;}@Override public SoundEvent getDeathSound(){return SoundEvents.ZOMBIE_DEATH;}@Override public SoundEvent getStepSound(){return SoundEvents.ZOMBIE_STEP;}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
}
