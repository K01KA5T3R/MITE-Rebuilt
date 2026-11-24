package dev.koikaster.mite;

import java.time.LocalDate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.*;
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

/** R195 tr type1, keeping the actual native WitherSkeleton subtype and registry
 * identity. This leaf remains type1 when its native spawn egg is used elsewhere. */
public final class SourceWitherSkeleton extends WitherSkeleton {
 private static final EntityDataAccessor<Boolean> FRENZIED=SynchedEntityData.defineId(SourceWitherSkeleton.class,EntityDataSerializers.BOOLEAN);
 private OriginalMeleeGoal sourceMelee;
 private SourceWitherSkeletonAI.ArrowGoal sourceRanged;
 private SourceWitherSkeletonAI.SeekFiringPosition sourceSeek;
 private int frenziedCountdown;private boolean massiveFall;
 public SourceWitherSkeleton(EntityType<? extends WitherSkeleton> type,Level level){
  super(type,level);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40);
  goalSelector.addGoal(1,new FloatGoal(this));goalSelector.addGoal(2,new SourceWitherSkeletonAI.RestrictSun(this));goalSelector.addGoal(3,new OriginalFleeSunGoal(this,1));goalSelector.addGoal(5,new OriginalWanderGoal(this,.75));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new OriginalHurtByGoal(this,false));targetSelector.addGoal(2,new OriginalNearestTargetGoal(this,Player.class,0,true));
  if(!level.isClientSide)OriginalRepairOwners.owner(this);sourceMelee=new OriginalMeleeGoal(this,Player.class,1.2,false,null);sourceRanged=new SourceWitherSkeletonAI.ArrowGoal(this);sourceSeek=new SourceWitherSkeletonAI.SeekFiringPosition(this);reassessWeaponGoal();refreshDimensions();
 }
 @Override protected void registerGoals(){}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(FRENZIED,false);}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.72f,2.34f);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 @Override public int getExperienceReward(){return 5;}
 @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected boolean isSunBurnTick(){return false;}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return SoundEvents.SKELETON_AMBIENT;}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return SoundEvents.SKELETON_HURT;}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return SoundEvents.SKELETON_DEATH;}
 @Override protected net.minecraft.sounds.SoundEvent getStepSound(){return SoundEvents.SKELETON_STEP;}
 @Override public boolean canFireProjectileWeapon(ProjectileWeaponItem item){return item instanceof BowItem;}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.is(DamageTypes.CACTUS)||source.is(DamageTypeTags.IS_PROJECTILE)&&source.getDirectEntity() instanceof AbstractArrow&&source.getEntity() instanceof AbstractSkeleton)return false;
  if(source.is(DamageTypes.FALL)&&amount>=getMaxHealth()*.5f)massiveFall=true;return super.hurt(source,amount);
 }
 @Override public void reassessWeaponGoal(){if(level()==null||level().isClientSide||sourceMelee==null)return;goalSelector.removeGoal(sourceMelee);goalSelector.removeGoal(sourceRanged);goalSelector.removeGoal(sourceSeek);if(getMainHandItem().getItem() instanceof BowItem){goalSelector.addGoal(4,sourceRanged);goalSelector.addGoal(3,sourceSeek);}else goalSelector.addGoal(4,sourceMelee);}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag){
  getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus",random.nextGaussian()*.05,AttributeModifier.Operation.MULTIPLY_BASE));
  var sword=new ItemStack(Items.STONE_SWORD);sword.setDamageValue(Math.min(sword.getMaxDamage()-1,(int)(sword.getMaxDamage()*(.7f+.3f*random.nextFloat()))));setItemSlot(EquipmentSlot.MAINHAND,sword);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);setCanPickUpLoot(true);
  var date=LocalDate.now();if(getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&date.getMonthValue()==10&&date.getDayOfMonth()==31&&random.nextFloat()<.25f){setItemSlot(EquipmentSlot.HEAD,new ItemStack(random.nextFloat()<.1f?Items.JACK_O_LANTERN:Items.CARVED_PUMPKIN));setDropChance(EquipmentSlot.HEAD,0);}reassessWeaponGoal();return group;
 }
 public boolean sourceFrenzied(){return (level().isClientSide?entityData.get(FRENZIED):frenziedCountdown>0)||SourceMobFrenzy.blood(this);}
 public void frenziedByBoneLord(LivingEntity target){frenziedCountdown=20;entityData.set(FRENZIED,true);if(getTarget()==null)setTarget(target);}
 @Override public void aiStep(){if(!level().isClientSide&&frenziedCountdown>0&&--frenziedCountdown==0)entityData.set(FRENZIED,false);super.aiStep();}
 @Override public void performRangedAttack(LivingEntity target,float power){
  var ammunition=new ItemStack(Content.get("rusted_iron_arrow"));var arrow=((ArrowItem)ammunition.getItem()).createArrow(level(),ammunition,this);var bow=getMainHandItem();SourceSpecificImmunities.captureLauncher(arrow,bow);SourceSkeletonAim.launch(arrow,this,target);
  arrow.setBaseDamage(power*2+random.nextGaussian()*.25+level().getDifficulty().getId()*.11f);int damage=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,bow),knockback=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,bow);if(damage>0)arrow.setBaseDamage(arrow.getBaseDamage()+damage*.5+.5);if(knockback>0)arrow.setKnockback(knockback);arrow.setSecondsOnFire(100);playSound(SoundEvents.SKELETON_SHOOT,1,1/(random.nextFloat()*.4f+.8f));level().addFreshEntity(arrow);
 }
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){
  int count=random.nextInt(3+looting)-1;if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Items.COAL);if(recent&&!massiveFall&&random.nextInt(200)<5+looting*2)spawnAtLocation(Items.WITHER_SKELETON_SKULL);count=random.nextInt(3);if(count>0&&!recent)count-=random.nextInt(count+1);for(int i=0;i<count;i++)spawnAtLocation(Items.BONE);
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("SkeletonType",(byte)1);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);reassessWeaponGoal();}
}
