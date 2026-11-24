package dev.koikaster.mite;

import java.time.LocalDate;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** R195 tn/tw/EntityAnimalWatcher/tm owner. Native registry/network identity,
 * collision/movement73A remain; modern Zombie/Piglin gameplay is bypassed. */
public final class SourcePigman extends ZombifiedPiglin implements SourcePigmanLife.Host,SourcePigmanGear.Host,SourcePigmanLoot.Host,OriginalDigging.Host,IteEliteSpawns.Host {
 private static final EntityDataAccessor<Boolean> ITE_ELITE=SynchedEntityData.defineId(SourcePigman.class,EntityDataSerializers.BOOLEAN);
 private static final UUID BOOST_ID=UUID.fromString(SourcePigmanLife.ATTACK_SPEED_ID);
 private static final AttributeModifier BOOST=new AttributeModifier(BOOST_ID,"Attacking speed boost",SourcePigmanLife.ATTACK_BOOST,AttributeModifier.Operation.ADDITION);
 private final SourcePigmanLife life=new SourcePigmanLife();
 private final SourcePigmanGear gear;
 private final SourcePigmanPickup pickup;
 private final SourceLegacyOffhand legacyOffhand;
 private final SourcePigmanAI sourceAi;
 private final OriginalDigging digging;
 /** on.j is an Entity, not necessarily LivingEntity. This is the only attack
  * target authority; native getTarget exposes its living view. */
 private Entity sourceTarget;
 private boolean massiveFall,pickedHeld,smart;
 public SourcePigman(EntityType<? extends ZombifiedPiglin> type,Level level){
  super(type,level);
  getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SourcePigmanLife.BASE_SPEED);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(SourcePigmanLife.BASE_ATTACK);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(SourcePigmanLife.FOLLOW_RANGE);getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);getAttribute(Attributes.ARMOR).setBaseValue(0);getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);setHealth(20);
  // tw.az consumes this draw before tn resets reinforcement base to zero.
  random.nextDouble();smart=random.nextInt(8)==0;
  setMaxUpStep(.5f);setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.LAVA,0);((GroundPathNavigation)getNavigation()).setCanOpenDoors(true);
  gear=new SourcePigmanGear(this);pickup=new SourcePigmanPickup(this);legacyOffhand=new SourceLegacyOffhand(this);digging=new OriginalDigging(this);sourceAi=new SourcePigmanAI(this);goalSelector.addGoal(5,sourceAi);refreshDimensions();
 }
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ITE_ELITE,false);}
 @Override public boolean iteElite(){return entityData.get(ITE_ELITE);}
 @Override public void iteElite(boolean elite){entityData.set(ITE_ELITE,elite);}
 @Override protected Component getTypeName(){return iteElite()?Component.translatable("entity.mite_reforged.ite_zombie_pigman_lord"):super.getTypeName();}
 private SourcePigmanNativeParent parent(){return (SourcePigmanNativeParent)(Object)this;}
 public SourcePigmanLife sourceLife(){return life;}public SourcePigmanAI sourceAi(){return sourceAi;}public SourcePigmanPickup sourcePickup(){return pickup;}public SourcePigmanGear sourceGear(){return gear;}
 public SourceLegacyOffhand sourceLegacyOffhand(){return legacyOffhand;}
 public boolean sourceSmart(){return smart;}public Entity sourceRawTarget(){return sourceTarget;}public void sourceRawTarget(Entity value){sourceTarget=value;}
 public Entity sourceEntityToAttack(){if(sourceTarget!=null&&sourceTarget.isRemoved())sourceTarget=null;return sourceTarget;}
 @Override public void setTarget(LivingEntity target){sourceTarget=target!=null&&target.isRemoved()?null:target;}
 @Override public LivingEntity getTarget(){var value=sourceEntityToAttack();return value instanceof LivingEntity living?living:null;}
 @Override public boolean isAggressive(){return sourceTarget!=null;}
 @Override public boolean isBaby(){return false;}@Override public void setBaby(boolean ignored){super.setBaby(false);}
 @Override protected boolean convertsInWater(){return false;}
 @Override protected void customServerAiStep(){}
 @Override public boolean wantsToPickUp(ItemStack stack){return false;}
 @Override public int getExperienceReward(){return iteElite()?IteEliteSpawns.EXPERIENCE:15;}
 @Override public boolean doHurtTarget(Entity target){return SourceMobStrikeNative.attack(this,target);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f,1.8f);}
 @Override public boolean fireImmune(){return false;}
 @Override public boolean isInvulnerableTo(DamageSource source){return source.is(DamageTypeTags.IS_FIRE)||super.isInvulnerableTo(source);}
 @Override public boolean canBeAffected(MobEffectInstance effect){var kind=effect.getEffect();return kind!=MobEffects.REGENERATION&&kind!=MobEffects.POISON&&kind!=MobEffects.WITHER&&super.canBeAffected(effect);}
 @Override protected boolean isSunBurnTick(){return false;}
 @Override public void tick(){if(sourceAi!=null){life.beforeParent(this);digging.beforeAi();sourceAi.harm.beforeTick();}parent().mite$pigmanMonsterTick();}
 @Override public void aiStep(){if(sourceAi!=null)sourceAi.beforeParent();parent().mite$pigmanMonsterAiStep();if(gear!=null&&!iteElite()){gear.pickup();pickup.tick();}}
 @Override public void setSpeed(float requested){super.setSpeed(.1f);setZza(requested);}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
 @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return (SourceArachnidWorld.cameFromSpawner(this)?.7f:.5f)-SourceArachnidWorld.pointBrightness(level(),pos);}
 @Override public boolean checkSpawnRules(LevelAccessor world,MobSpawnType reason){return world.getDifficulty()!=Difficulty.PEACEFUL;}
 @Override public boolean checkSpawnObstruction(LevelReader world){return SourcePigmanLife.canSpawn(new SourcePigmanLife.SpawnHost(){public boolean nonPeaceful(){return level().getDifficulty()!=Difficulty.PEACEFUL;}public boolean noEntityCollision(){return world.isUnobstructed(SourcePigman.this);}public boolean noBlockCollision(){return world.noCollision(SourcePigman.this,getBoundingBox());}public boolean noLiquid(){return !world.containsAnyLiquid(getBoundingBox());}});}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.is(DamageTypeTags.IS_FIRE))return false;
  if(source.is(net.minecraft.world.damagesource.DamageTypes.FALL)&&amount>=getMaxHealth()*.5f)massiveFall=true;
  if(level().isClientSide||sourceAi==null)return parent().mite$pigmanMonsterHurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->sourceAi.harm.afterDamage(source,outcome.negativelyAffected()))){
   boolean accepted=parent().mite$pigmanMonsterHurt(source,amount);var outcome=frame.finish();var responsible=source.getEntity();
   if(!outcome.destroyed()&&outcome.negativelyAffected()){
    if(responsible!=this&&responsible!=getVehicle()&&responsible!=getFirstPassenger())sourceTarget=responsible;
    if(responsible instanceof Player)smart=true;
   }
   life.afterParentDamage(this,accepted||outcome.negativelyAffected(),outcome.negativelyAffected(),responsible);return accepted;
  }
 }
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){
  digging.cancel();
  if(iteElite()){IteEliteSpawns.dropConsumables(this,recent);return;}
  if(!massiveFall||random.nextInt(10)==0)SourcePigmanLoot.drop(this,recent,looting,massiveFall);pickup.drop();gear.death(recent,looting);legacyOffhand.drop();
 }
 @Override protected void dropEquipment(){if(!iteElite())super.dropEquipment();}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
  SourcePigmanParentSpawn.initialize(new SourcePigmanParentSpawn.Host(){
   public double nextGaussian(){return random.nextGaussian();}public float nextFloat(){return random.nextFloat();}public double nextDouble(){return random.nextDouble();}public float worldNextFloat(){return world.getRandom().nextFloat();}
   private void bonus(Attribute key,String name,double value,AttributeModifier.Operation operation){getAttribute(key).addPermanentModifier(new AttributeModifier(name,value,operation));}
   public void followBonus(double value){bonus(Attributes.FOLLOW_RANGE,"Random spawn bonus",value,AttributeModifier.Operation.MULTIPLY_BASE);}public void lootPickup(boolean value){setCanPickUpLoot(value);}public void equipment(){setItemSlot(EquipmentSlot.MAINHAND,gear.initialWeapon(SourcePigmanLoot.weapon(SourcePigman.this)));}
   public boolean headEmpty(){return getItemBySlot(EquipmentSlot.HEAD).isEmpty();}public boolean halloween(){var date=LocalDate.now();return date.getMonthValue()==10&&date.getDayOfMonth()==31;}public void pumpkin(boolean lit){setItemSlot(EquipmentSlot.HEAD,new ItemStack(lit?Blocks.JACK_O_LANTERN:Blocks.CARVED_PUMPKIN));}public void headDropChance(float value){setDropChance(EquipmentSlot.HEAD,value);}
   public void knockbackBonus(double value){bonus(Attributes.KNOCKBACK_RESISTANCE,"Random spawn bonus",value,AttributeModifier.Operation.ADDITION);}public void reinforcementBonus(double value){bonus(Attributes.SPAWN_REINFORCEMENTS_CHANCE,"Leader zombie bonus",value,AttributeModifier.Operation.ADDITION);}public void maximumHealthBonus(double value){bonus(Attributes.MAX_HEALTH,"Leader zombie bonus",value,AttributeModifier.Operation.MULTIPLY_TOTAL);}
   public void villager(boolean value){}public void baby(boolean value){setBaby(false);}
  },data,SourceZombieBody.localSpawnFactor(world,blockPosition()));
  ((SourceSpawnBaseAccess)(Mob)this).mite$sourceSpawnType(reason);
  return data;
 }
 @Override protected SoundEvent getAmbientSound(){return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;}@Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.ZOMBIFIED_PIGLIN_HURT;}@Override protected SoundEvent getDeathSound(){return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;}
 @Override public boolean killedEntity(net.minecraft.server.level.ServerLevel level,LivingEntity victim){return SourcePigmanKills.killed(this,level,victim,parent().mite$pigmanMonsterKilledEntity(level,victim));}
 @Override public void die(DamageSource source){if(digging!=null)digging.cancel();super.die(source);}
 @Override public void addAdditionalSaveData(CompoundTag tag){parent().mite$pigmanMonsterSave(tag);tag.putShort("Anger",life.savedAnger());tag.putShort("AttackTime",(short)sourceAi.attackCooldown());tag.putBoolean("is_smart",smart);tag.putBoolean("picked_up_a_held_item",pickedHeld);tag.putBoolean("has_taken_massive_fall_damage",massiveFall);tag.putInt("ConversionTime",-1);tag.putBoolean("R195PigmanOwner",true);sourceAi.harm.save(tag);digging.state.save(tag);pickup.save(tag);legacyOffhand.save(tag);tag.remove("IsBaby");tag.remove("IsVillager");tag.remove("AngerTime");tag.remove("AngryAt");IteEliteSpawns.save(this,tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){parent().mite$pigmanMonsterLoad(tag);life.loadAnger(tag.contains("Anger",Tag.TAG_ANY_NUMERIC)?tag.getShort("Anger"):(short)tag.getInt("AngerTime"));sourceAi.loadCooldown(tag.getShort("AttackTime"));smart=tag.getBoolean("is_smart");pickedHeld=tag.getBoolean("picked_up_a_held_item");massiveFall=tag.getBoolean("has_taken_massive_fall_damage");sourceAi.harm.load(tag);digging.state.load(tag);pickup.load(tag);legacyOffhand.load(tag);setBaby(false);if(!tag.getBoolean("R195PigmanOwner")){getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.5);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40);getAttribute(Attributes.ARMOR).setBaseValue(0);getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718"));}IteEliteSpawns.restore(this,this,tag);}
 @Override public OriginalDigging originalDigging(){return digging;}
 @Override public Mob actor(){return this;}@Override public float dropChance(int slot){return getEquipmentDropChance(SourcePigmanGear.slot(slot));}@Override public void dropChance(int slot,float value){setDropChance(SourcePigmanGear.slot(slot),value);}@Override public boolean massiveFall(){return massiveFall;}@Override public boolean pickedUpHeld(){return pickedHeld;}@Override public void pickedUpHeld(boolean value){pickedHeld=value;}
 @Override public boolean client(){return level().isClientSide;}@Override public Object rawTarget(){return sourceTarget;}@Override public Object entityToAttack(){return sourceEntityToAttack();}@Override public void target(Object value){sourceTarget=(Entity)value;}@Override public void removeAttackBoost(){getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(BOOST_ID);}@Override public void addAttackBoost(){getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(BOOST);}
 @Override public int nextInt(int n){return random.nextInt(n);}@Override public float nextFloat(){return random.nextFloat();}@Override public void angerSound(float pitch){playSound(SoundEvents.ZOMBIFIED_PIGLIN_ANGRY,getSoundVolume()*2,pitch);}
 @Override public Object closestPlayer(float range){return sourceAi.closestPlayer(range);}@Override public boolean player(Object value){return value instanceof Player;}@Override public Iterable<?> nearbyEntities(){return level().getEntities(this,getBoundingBox().inflate(32,32,32));}@Override public boolean pigman(Object value){return value instanceof SourcePigman;}@Override public void provokePeer(Object peer,Object responsible){var pig=(SourcePigman)peer;pig.life.provoke(pig,responsible);}
 @Override public int rareDropBound(){return 200;}@Override public void drop(SourcePigmanLoot.Item kind){spawnAtLocation(switch(kind){case ROTTEN_FLESH->Items.ROTTEN_FLESH;case GOLD_NUGGET->Items.GOLD_NUGGET;case GOLD_INGOT->Items.GOLD_INGOT;default->throw new IllegalArgumentException("tn drop");});}
}
