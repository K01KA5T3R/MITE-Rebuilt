package dev.koikaster.mite;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Ordinary tw keeps native Zombie.tick only for the human's V047 water
 * conversion. Its gameplay AI/hurt/loot/initialization belong to the source. */
public final class SourceZombie extends Zombie implements SourceZombieOwner,IteEliteSpawns.Host {
 private static final EntityDataAccessor<Boolean> VILLAGER=SynchedEntityData.defineId(SourceZombie.class,EntityDataSerializers.BOOLEAN),CONVERTING=SynchedEntityData.defineId(SourceZombie.class,EntityDataSerializers.BOOLEAN);
 private static final EntityDataAccessor<Integer> PROFESSION=SynchedEntityData.defineId(SourceZombie.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> ITE_ELITE=SynchedEntityData.defineId(SourceZombie.class,EntityDataSerializers.BOOLEAN);
 private final SourceZombieBody source;
 public SourceZombie(EntityType<? extends Zombie> type,Level level){super(type,level);source=new SourceZombieBody(this,this);source.goals();refreshDimensions();}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(VILLAGER,false);entityData.define(CONVERTING,false);entityData.define(PROFESSION,0);entityData.define(ITE_ELITE,false);}
 @Override public boolean iteElite(){return entityData.get(ITE_ELITE);}
 @Override public void iteElite(boolean elite){entityData.set(ITE_ELITE,elite);}
 @Override protected Component getTypeName(){return iteElite()?Component.translatable("entity.mite_reforged.ite_zombie_lord"):super.getTypeName();}
 @Override protected void registerGoals(){}
 @Override public SourceZombieBody sourceZombie(){return source;}@Override public boolean sourceVillager(){return entityData.get(VILLAGER);}@Override public void sourceVillager(boolean value){entityData.set(VILLAGER,value);}@Override public int sourceProfession(){return entityData.get(PROFESSION);}@Override public void sourceProfession(int value){entityData.set(PROFESSION,value);}@Override public boolean sourceConverting(){return entityData.get(CONVERTING);}@Override public void sourceConverting(boolean value){entityData.set(CONVERTING,value);}@Override public float sourceDropChance(int slot){return getEquipmentDropChance(SourcePigmanGear.slot(slot));}
 @Override public boolean isBaby(){return false;}@Override public void setBaby(boolean ignored){super.setBaby(false);}@Override protected boolean supportsBreakDoorGoal(){return false;}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f,1.8f);}@Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public boolean canBeAffected(MobEffectInstance effect){var kind=effect.getEffect();return kind!=MobEffects.REGENERATION&&kind!=MobEffects.POISON&&kind!=MobEffects.WITHER&&super.canBeAffected(effect);}
 @Override protected boolean convertsInWater(){return !WorldContentMode.settings(level()).classicVisuals()&&!sourceVillager()&&!iteElite();}
 @Override public boolean isUnderWaterConverting(){return !WorldContentMode.settings(level()).classicVisuals()&&!sourceVillager()&&super.isUnderWaterConverting();}
 @Override public void tick(){if(source!=null)source.beforeTick();super.tick();}
 @Override public void aiStep(){if(source!=null)source.beforeAi();((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterAiStep();if(source!=null)source.afterAi();}
 @Override public boolean wantsToPickUp(net.minecraft.world.item.ItemStack stack){return false;}
 @Override public boolean hurt(DamageSource damage,float amount){return source==null?super.hurt(damage,amount):source.hurt(damage,amount);}
 @Override public boolean doHurtTarget(Entity target){return SourceMobStrikeNative.attack(this,target);}
 @Override public boolean killedEntity(ServerLevel level,LivingEntity victim){return SourceZombieKills.killed(this,level,victim,((SourcePigmanNativeParent)(Object)this).mite$pigmanMonsterKilledEntity(level,victim));}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}@Override protected void dropCustomDeathLoot(DamageSource damage,int looting,boolean recent){if(iteElite())IteEliteSpawns.dropConsumables(this,recent);else source.deathLoot(recent,looting);}@Override protected void dropEquipment(){}
 @Override public int getExperienceReward(){return iteElite()?IteEliteSpawns.EXPERIENCE:15;}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){return source.initialize(world,reason,data);}
 @Override public InteractionResult mobInteract(Player player,InteractionHand hand){return source.interact(player,hand);}@Override public void handleEntityEvent(byte event){if(source==null||!source.handleEvent(event))super.handleEntityEvent(event);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);source.save(tag);IteEliteSpawns.save(this,tag);}@Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);source.load(tag);IteEliteSpawns.restore(this,this,tag);}
 @Override public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.phys.Vec3 factor){super.makeStuckInBlock(state,factor);if(source!=null)source.web(state);}
 @Override public void die(DamageSource damage){if(source!=null)source.digging.cancel();super.die(damage);}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
}
