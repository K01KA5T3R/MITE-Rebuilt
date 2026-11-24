package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** EntityLongdead and the tr branches needed by its actual owner. Native
 * AbstractSkeleton supplies movement/sounds, never native stray conversion,
 * random modern armor or vanilla bow-only goal selection. */
public class SourceLongdead extends AbstractSkeleton {
 private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(SourceLongdead.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> FRENZIED=SynchedEntityData.defineId(SourceLongdead.class,EntityDataSerializers.BOOLEAN);
 private OriginalMeleeGoal sourceMelee;
 private SourceLongdeadAI.ArrowGoal sourceRanged;
 private SourceLongdeadAI.SeekFiringPosition sourceSeek;
 private int wetTicks,frenziedCountdown;private boolean massiveFall;
 public SourceLongdead(EntityType<? extends SourceLongdead> type,Level level){
  super(type,level);
  for(var slot:new EquipmentSlot[]{EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD})setDropChance(slot,.085f*.25f);
  goalSelector.addGoal(1,new FloatGoal(this));
  goalSelector.addGoal(2,new SourceLongdeadAI.RestrictSun(this));
  goalSelector.addGoal(3,new OriginalFleeSunGoal(this,1));
  goalSelector.addGoal(5,new OriginalWanderGoal(this,.75));
  goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
  goalSelector.addGoal(6,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new OriginalHurtByGoal(this,false));
  targetSelector.addGoal(2,new OriginalNearestTargetGoal(this,Player.class,0,true));
  if(!level.isClientSide)OriginalRepairOwners.owner(this);
  sourceMelee=new OriginalMeleeGoal(this,Player.class,1.2,false,null);
  sourceRanged=new SourceLongdeadAI.ArrowGoal(this);
  sourceSeek=new SourceLongdeadAI.SeekFiringPosition(this);
  reassessWeaponGoal();
 }
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,0);entityData.define(FRENZIED,false);}
 public boolean guardian(){return this instanceof SourceLongdeadGuardian;}
 public static AttributeSupplier.Builder attributes(boolean guardian){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,guardian?24:12).add(Attributes.FOLLOW_RANGE,40).add(Attributes.MOVEMENT_SPEED,.29f).add(Attributes.ATTACK_DAMAGE,guardian?8:6).add(Attributes.ARMOR,0);}
 public int skeletonType(){return entityData.get(TYPE);}
 void skeletonType(int type){entityData.set(TYPE,type);refreshDimensions();}
 @Override public EntityDimensions getDimensions(Pose pose){return skeletonType()==1?EntityDimensions.scalable(.72f,2.34f):EntityDimensions.scalable(.6f,1.8f);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 @Override public int getExperienceReward(){return guardian()?25:15;}
 float naturalDefense(DamageSource source){return source!=null&&ClassicDefense.mundaneBypass(source)?0:guardian()?2:1;}
 @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected SoundEvent getAmbientSound(){return SoundEvents.SKELETON_AMBIENT;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SKELETON_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.SKELETON_DEATH;}
 @Override protected SoundEvent getStepSound(){return SoundEvents.SKELETON_STEP;}
 @Override public boolean canFireProjectileWeapon(ProjectileWeaponItem item){return item instanceof BowItem;}
 @Override public boolean fireImmune(){return skeletonType()==1||super.fireImmune();}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.is(DamageTypes.CACTUS)||source.is(DamageTypeTags.IS_PROJECTILE)&&source.getDirectEntity() instanceof AbstractArrow&&source.getEntity() instanceof AbstractSkeleton)return false;
  if(source.is(DamageTypes.FALL)&&amount>=getMaxHealth()*.5f)massiveFall=true;
  return super.hurt(source,amount);
 }
 @Override public boolean doHurtTarget(Entity target){boolean hit=super.doHurtTarget(target);if(hit&&skeletonType()==1&&target instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.WITHER,200),this);return hit;}
 /** tr.bT replaces goals whenever the held item changes, including custom bows. */
 @Override public void reassessWeaponGoal(){
  if(level()==null||level().isClientSide||sourceMelee==null)return;
  goalSelector.removeGoal(sourceMelee);goalSelector.removeGoal(sourceRanged);goalSelector.removeGoal(sourceSeek);
  if(getMainHandItem().getItem() instanceof BowItem){goalSelector.addGoal(4,sourceRanged);goalSelector.addGoal(3,sourceSeek);}
  else goalSelector.addGoal(4,sourceMelee);
 }
 protected ItemStack randomEquipment(Item item){
  var stack=new ItemStack(item);
  // Human58D fixes regional strength. These dungeon inhabitants use tension1
  // throughout their region; equipment never grows with days or habitation.
  if(random.nextFloat()<.1f){int levels=5+random.nextInt(18);for(var entry:ClassicEnchanting.selectNative(random,stack,levels,false))stack.enchant(entry.enchantment,entry.level);}
  if(stack.isDamageableItem())stack.setDamageValue(Math.min(stack.getMaxDamage()-1,(int)(stack.getMaxDamage()*((stack.isEnchanted()?.5f:.7f)+.3f*random.nextFloat()))));
  return stack;
 }
 protected void addRandomWeapon(){setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(Content.get(skeletonType()==2?"ancient_metal_sword":"ancient_metal_bow")));}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
  getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus",random.nextGaussian()*.05,AttributeModifier.Operation.MULTIPLY_BASE));
  skeletonType(world.getLevel().dimension()==Level.NETHER?1:random.nextFloat()<.5f?2:0);
  if(skeletonType()==1){setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(Items.STONE_SWORD));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);}
  else{
   addRandomWeapon();
   setItemSlot(EquipmentSlot.FEET,randomEquipment(Content.get("ancient_metal_chainmail_boots")));
   setItemSlot(EquipmentSlot.LEGS,randomEquipment(Content.get("ancient_metal_chainmail_leggings")));
   setItemSlot(EquipmentSlot.CHEST,randomEquipment(Content.get("ancient_metal_chainmail_chestplate")));
   setItemSlot(EquipmentSlot.HEAD,randomEquipment(Content.get("ancient_metal_chainmail_helmet")));
  }
  setCanPickUpLoot(true);reassessWeaponGoal();return data;
 }
 /** New037 forced-type entry used only by AncientBoneLord summons. The old
  * natural/dungeon finalizeSpawn body and its verified branches stay untouched. */
 SpawnGroupData finalizeSummonedSpawn(ServerLevelAccessor world,int forced){
  getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus",random.nextGaussian()*.05,AttributeModifier.Operation.MULTIPLY_BASE));
  skeletonType(forced);
  if(forced==1){setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));var stone=getMainHandItem();stone.setDamageValue(Math.min(stone.getMaxDamage()-1,(int)(stone.getMaxDamage()*(.7f+.3f*random.nextFloat()))));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);}
  else{addRandomWeapon();setItemSlot(EquipmentSlot.FEET,randomEquipment(Content.get("ancient_metal_chainmail_boots")));setItemSlot(EquipmentSlot.LEGS,randomEquipment(Content.get("ancient_metal_chainmail_leggings")));setItemSlot(EquipmentSlot.CHEST,randomEquipment(Content.get("ancient_metal_chainmail_chestplate")));setItemSlot(EquipmentSlot.HEAD,randomEquipment(Content.get("ancient_metal_chainmail_helmet")));}
  setCanPickUpLoot(true);var date=java.time.LocalDate.now();
  if(getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&date.getMonthValue()==10&&date.getDayOfMonth()==31&&random.nextFloat()<.25f){setItemSlot(EquipmentSlot.HEAD,new ItemStack(random.nextFloat()<.1f?Items.JACK_O_LANTERN:Items.CARVED_PUMPKIN));setDropChance(EquipmentSlot.HEAD,0);}
  reassessWeaponGoal();return null;
 }
 @Override protected boolean isSunBurnTick(){return false;}
 private void sourceSunlight(){
  if(skeletonType()!=1)OriginalMobEnvironment.sunlight(this,wetTicks);
 }
 public boolean sourceFrenzied(){return (level().isClientSide?entityData.get(FRENZIED):frenziedCountdown>0)||SourceMobFrenzy.blood(this);}
 public void frenziedByBoneLord(LivingEntity target){frenziedCountdown=20;entityData.set(FRENZIED,true);if(getTarget()==null)setTarget(target);}
 @Override public void aiStep(){
  if(!level().isClientSide){wetTicks=OriginalMobEnvironment.nextWetTicks(this,wetTicks);sourceSunlight();if(frenziedCountdown>0&&--frenziedCountdown==0)entityData.set(FRENZIED,false);}
  super.aiStep();
 }
 @Override public void performRangedAttack(LivingEntity target,float power){
  var ammunition=new ItemStack(Content.get("ancient_metal_arrow"));
  var arrow=((ArrowItem)ammunition.getItem()).createArrow(level(),ammunition,this);
  var bow=getMainHandItem();SourceSpecificImmunities.captureLauncher(arrow,bow);
  SourceSkeletonAim.launch(arrow,this,target);
  arrow.setBaseDamage(power*2+random.nextGaussian()*.25+level().getDifficulty().getId()*.11f);
  int damage=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,bow),knockback=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,bow);
  if(damage>0)arrow.setBaseDamage(arrow.getBaseDamage()+damage*.5+.5);if(knockback>0)arrow.setKnockback(knockback);
  if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,bow)>0||skeletonType()==1||isOnFire()&&random.nextInt(3)==0)arrow.setSecondsOnFire(100);
  playSound(SoundEvents.SKELETON_SHOOT,1,1/(random.nextFloat()*.4f+.8f));level().addFreshEntity(arrow);
 }
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){
  int count;
  if(skeletonType()==1){count=random.nextInt(3+looting)-1;if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Items.COAL);if(recent&&!massiveFall&&random.nextInt(200)<5+looting*2)spawnAtLocation(Items.WITHER_SKELETON_SKULL);}
  else if(skeletonType()!=2){count=random.nextInt(2+looting);if(count>0&&!recent)count-=random.nextInt(count+1);if(count>0)count=random.nextInt(3)==0?1:0;for(int i=0;i<count;i++)spawnAtLocation(Content.get("ancient_metal_arrow"));}
  count=random.nextInt(3);if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Items.BONE);
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("SkeletonType",(byte)skeletonType());}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("SkeletonType"))skeletonType(tag.getByte("SkeletonType"));reassessWeaponGoal();}
}
