package dev.koikaster.mite;

import java.time.LocalDate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
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

/** R195 tr, retaining the native Skeleton registry identity and native movement.
 * Longdead's already verified owner remains separate. */
public class SourceSkeleton extends Skeleton {
 private static final EntityDataAccessor<Integer> TYPE=SynchedEntityData.defineId(SourceSkeleton.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Boolean> FRENZIED=SynchedEntityData.defineId(SourceSkeleton.class,EntityDataSerializers.BOOLEAN);
 private OriginalMeleeGoal sourceMelee;
 private SourceSkeletonAI.ArrowGoal sourceRanged;
 private SourceSkeletonAI.SeekFiringPosition sourceSeek;
 private int wetTicks,frenziedCountdown,forcedType=-1;
 private boolean massiveFall;
 public SourceSkeleton(EntityType<? extends Skeleton> type,Level level){
  super(type,level);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40);
  goalSelector.addGoal(1,new FloatGoal(this));
  goalSelector.addGoal(2,new SourceSkeletonAI.RestrictSun(this));
  goalSelector.addGoal(3,new OriginalFleeSunGoal(this,1));
  goalSelector.addGoal(5,new OriginalWanderGoal(this,.75));
  goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
  goalSelector.addGoal(6,new RandomLookAroundGoal(this));
  targetSelector.addGoal(1,new OriginalHurtByGoal(this,false));
  targetSelector.addGoal(2,new OriginalNearestTargetGoal(this,Player.class,0,true));
  if(!level.isClientSide)OriginalRepairOwners.owner(this);
  sourceMelee=new OriginalMeleeGoal(this,Player.class,1.2,false,null);
  sourceRanged=new SourceSkeletonAI.ArrowGoal(this);
  sourceSeek=new SourceSkeletonAI.SeekFiringPosition(this);reassessWeaponGoal();refreshDimensions();
 }
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(TYPE,0);entityData.define(FRENZIED,false);}
 public int skeletonType(){return entityData.get(TYPE);}
 void skeletonType(int type){entityData.set(TYPE,type);refreshDimensions();}
 public int randomSkeletonType(){return level().dimension()==Level.NETHER?1:random.nextFloat()<.25f?2:0;}
 void forceSkeletonType(int type){if(type<0||type>2)throw new IllegalArgumentException("Skeleton type");forcedType=type;}
 @Override public EntityDimensions getDimensions(Pose pose){return skeletonType()==1?EntityDimensions.scalable(.72f,2.34f):EntityDimensions.scalable(.6f,1.8f);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 @Override public int getExperienceReward(){return 5;}
 @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override public boolean canFreeze(){return false;}
 @Override public boolean isFreezeConverting(){return false;}
 @Override public void setFreezeConverting(boolean value){super.setFreezeConverting(false);}
 @Override protected void doFreezeConversion(){}
 @Override public boolean canFireProjectileWeapon(ProjectileWeaponItem item){return item instanceof BowItem;}
 @Override public boolean fireImmune(){return skeletonType()==1||super.fireImmune();}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.is(DamageTypes.CACTUS)||source.is(DamageTypeTags.IS_PROJECTILE)&&source.getDirectEntity() instanceof AbstractArrow&&source.getEntity() instanceof AbstractSkeleton)return false;
  if(source.is(DamageTypes.FALL)&&amount>=getMaxHealth()*.5f)massiveFall=true;
  return super.hurt(source,amount);
 }
 @Override public boolean doHurtTarget(Entity target){boolean hit=super.doHurtTarget(target);if(hit&&skeletonType()==1&&target instanceof LivingEntity living)living.addEffect(new MobEffectInstance(MobEffects.WITHER,200),this);return hit;}
 @Override public void reassessWeaponGoal(){
  if(level()==null||level().isClientSide||sourceMelee==null)return;
  goalSelector.removeGoal(sourceMelee);goalSelector.removeGoal(sourceRanged);goalSelector.removeGoal(sourceSeek);
  if(getMainHandItem().getItem() instanceof BowItem){goalSelector.addGoal(4,sourceRanged);goalSelector.addGoal(3,sourceSeek);}else goalSelector.addGoal(4,sourceMelee);
 }
 /** og.enchantEquipment and ye.randomizeForMob at human58D's fixed tension1. */
 protected ItemStack randomEquipment(Item item,boolean mayEnchant){
  var stack=new ItemStack(item);
  if(mayEnchant&&random.nextFloat()<.1f)for(var entry:ClassicEnchanting.selectNative(random,stack,5+random.nextInt(18),false))stack.enchant(entry.enchantment,entry.level);
  if(stack.isDamageableItem())stack.setDamageValue(Math.min(stack.getMaxDamage()-1,(int)(stack.getMaxDamage()*((stack.isEnchanted()?.5f:.7f)+.3f*random.nextFloat()))));
  return stack;
 }
 protected void addRandomWeapon(){
  // Human58D removes day growth; the source rare 1/20 melee pool stays fixed.
  if(skeletonType()==2&&random.nextInt(20)==0){setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(Content.get(random.nextBoolean()?"rusted_iron_dagger":"rusted_iron_sword"),false));return;}
  setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(skeletonType()==2?Content.get("wood_club"):Items.BOW,true));
 }
 protected void addRandomEquipment(){
  addRandomWeapon();if(random.nextFloat()>=.15f)return;
  int material=random.nextInt(2);for(int i=0;i<3;i++)if(random.nextFloat()<.095f)material++;
  float stop=level().getDifficulty()==net.minecraft.world.Difficulty.HARD?.1f:.25f;
  var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
  var parts=new String[]{"helmet","chestplate","leggings","boots"};
  var leather=new Item[]{Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS};
  var materials=new String[]{"","rusted_iron_chainmail_","copper_chainmail_","rusted_iron_","copper_"};
  for(int i=0;i<4;i++){if(i>0&&random.nextFloat()<stop)break;if(!getItemBySlot(slots[i]).isEmpty())continue;setItemSlot(slots[i],randomEquipment(material==0?leather[i]:Content.get(materials[material]+parts[i]),true));}
 }
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag){
  getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus",random.nextGaussian()*.05,AttributeModifier.Operation.MULTIPLY_BASE));
  skeletonType(forcedType>=0?forcedType:randomSkeletonType());forcedType=-1;
  if(skeletonType()==1){setItemSlot(EquipmentSlot.MAINHAND,randomEquipment(Items.STONE_SWORD,false));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);}else addRandomEquipment();
  setCanPickUpLoot(true);var date=LocalDate.now();
  if(getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&date.getMonthValue()==10&&date.getDayOfMonth()==31&&random.nextFloat()<.25f){setItemSlot(EquipmentSlot.HEAD,new ItemStack(random.nextFloat()<.1f?Items.JACK_O_LANTERN:Items.CARVED_PUMPKIN));setDropChance(EquipmentSlot.HEAD,0);}
  reassessWeaponGoal();return data;
 }
 @Override protected boolean isSunBurnTick(){return false;}
 private void sourceSunlight(){
  if(skeletonType()!=1)OriginalMobEnvironment.sunlight(this,wetTicks);
 }
 public boolean sourceFrenzied(){return (level().isClientSide?entityData.get(FRENZIED):frenziedCountdown>0)||SourceMobFrenzy.blood(this);}
 public void frenziedByBoneLord(LivingEntity target){frenziedCountdown=20;entityData.set(FRENZIED,true);if(getTarget()==null)setTarget(target);}
 @Override public void aiStep(){if(!level().isClientSide){wetTicks=OriginalMobEnvironment.nextWetTicks(this,wetTicks);sourceSunlight();if(frenziedCountdown>0&&--frenziedCountdown==0)entityData.set(FRENZIED,false);}super.aiStep();}
 @Override public void performRangedAttack(LivingEntity target,float power){
  var ammunition=new ItemStack(Content.get("rusted_iron_arrow"));var arrow=((ArrowItem)ammunition.getItem()).createArrow(level(),ammunition,this);
  var bow=getMainHandItem();SourceSpecificImmunities.captureLauncher(arrow,bow);SourceSkeletonAim.launch(arrow,this,target);
  arrow.setBaseDamage(power*2+random.nextGaussian()*.25+level().getDifficulty().getId()*.11f);
  int damage=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,bow),knockback=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,bow);
  if(damage>0)arrow.setBaseDamage(arrow.getBaseDamage()+damage*.5+.5);if(knockback>0)arrow.setKnockback(knockback);
  if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,bow)>0||skeletonType()==1||isOnFire()&&random.nextInt(3)==0)arrow.setSecondsOnFire(100);
  playSound(SoundEvents.SKELETON_SHOOT,1,1/(random.nextFloat()*.4f+.8f));level().addFreshEntity(arrow);
 }
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){
  int count;
  if(skeletonType()==1){count=random.nextInt(3+looting)-1;if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Items.COAL);if(recent&&!massiveFall&&random.nextInt(200)<5+looting*2)spawnAtLocation(Items.WITHER_SKELETON_SKULL);}
  else if(skeletonType()!=2){count=random.nextInt(2+looting);if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Content.get("rusted_iron_arrow"));}
  count=random.nextInt(3);if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Items.BONE);
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("SkeletonType",(byte)skeletonType());tag.remove("StrayConversionTime");}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("SkeletonType"))skeletonType(tag.getByte("SkeletonType"));reassessWeaponGoal();}
}
