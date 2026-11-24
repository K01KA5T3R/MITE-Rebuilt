package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Actual sb registry owner. Native registration/navigation/entity transport
 * stays SnowGolem; source owns its body,9tasks,heat,damage,repair and appearance. */
public final class SourceSnowGolem extends SnowGolem implements SourceSnowGolemRules.Host,OriginalParentHarmState.Flags {
 public static final ResourceKey<DamageType> MELT=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("source_melt"));
 private OriginalParentHarmState harm;private SourceSnowball projectile;private boolean decided,fleeing,constructed;private int lastSourceFireContact=Integer.MIN_VALUE;
 public SourceSnowGolem(EntityType<? extends SnowGolem> type,Level level){
  super(type,level);getAttribute(Attributes.MAX_HEALTH).setBaseValue(8);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).2f);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);setHealth(8);setMaxUpStep(.5f);setPathfindingMalus(BlockPathTypes.WATER,8);xpReward=0;
  harm=new OriginalParentHarmState(this,this);SourceSnowGolemGoals.install(this);super.setPumpkin(true);refreshDimensions();constructed=true;
 }
 private SourceSnowGolemParent parent(){return (SourceSnowGolemParent)(Object)this;}
 @Override protected void registerGoals(){}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.6f,1.8f);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 @Override public void tick(){if(!level().isClientSide&&harm!=null)harm.beforeTick();super.tick();}
 @Override public void aiStep(){SourceSnowGolemRules.living(this);}
 @Override public void performRangedAttack(LivingEntity target,float power){SourceSnowGolemRules.attack(this,new SourceSnowGolemRules.Target(target.getX(),target.getY(),target.getZ(),target.getEyeHeight()));}
 @Override public boolean hurt(DamageSource source,float amount){
  if(ClassicDefense.name(source).equals("pepsin"))return false;
  float adjusted=SourceSnowGolemRules.damage(source.is(MELT),source.getDirectEntity() instanceof AbstractArrow&&!(source.getDirectEntity() instanceof ThrownTrident),amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->{if(harm!=null&&!level().isClientSide)harm.afterDamage(source,outcome.negativelyAffected());})){boolean result=parent().mite$snowParentHurt(source,adjusted);frame.finish();return result;}
 }
 @Override public boolean canBeAffected(MobEffectInstance effect){var p=effect.getEffect();return SourceConstructRules.potion(false,p==MobEffects.REGENERATION,p==MobEffects.POISON,p==MobEffects.WITHER)&&super.canBeAffected(effect);}
 @Override protected int increaseAirSupply(int air){return SourceConstructRules.recoveredAir();}
 @Override public void setHealth(float health){int before=constructed?(int)getHealth():0;super.setHealth(health);if(constructed&&!level().isClientSide&&(int)getHealth()>before)Network.repairFx(this);}
 @Override public boolean isSensitiveToWater(){return false;}
 boolean beginSourceFireContact(){if(lastSourceFireContact==tickCount)return false;lastSourceFireContact=tickCount;return true;}
 @Override public void setSecondsOnFire(int seconds){SourceSnowFireContact.ignite(this,seconds);}
 @Override public void lavaHurt(){if(!isRemoved()&&!level().isClientSide){hurt(damageSources().lava(),4);setSecondsOnFire(15);}}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override public int getExperienceReward(){return 0;}
 @Override public int getAmbientSoundInterval(){return 120;}
 @Override protected SoundEvent getAmbientSound(){return null;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return null;}
 @Override protected SoundEvent getDeathSound(){return null;}
 @Override public boolean removeWhenFarAway(double distance){return false;}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){SourceSnowGolemRules.drops(this,looting);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){return parent().mite$snowParentInteract(player,hand);}
 @Override public boolean hasPumpkin(){return true;}
 @Override public void setPumpkin(boolean value){super.setPumpkin(true);}
 @Override public void shear(SoundSource source){}
 @Override public boolean readyForShearing(){return false;}
 @Override public boolean isShearable(ItemStack item,Level world,BlockPos pos){return false;}
 @Override public List<ItemStack> onSheared(Player player,ItemStack item,Level world,BlockPos pos,int fortune){return List.of();}
 @Override public void addAdditionalSaveData(CompoundTag tag){parent().mite$snowParentSave(tag);tag.putBoolean("R195SnowGolemOwner",true);harm.save(tag);tag.remove("Pumpkin");}
 @Override public void readAdditionalSaveData(CompoundTag tag){parent().mite$snowParentLoad(tag);harm.load(tag);super.setPumpkin(true);if(!tag.getBoolean("R195SnowGolemOwner")){getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).2f);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);}projectile=null;}
 @Override public boolean hasDecidedToFlee(){return decided;}@Override public void hasDecidedToFlee(boolean value){decided=value;}@Override public boolean fleeing(){return fleeing;}@Override public void fleeing(boolean value){fleeing=value;}
 @Override public void parentLiving(){parent().mite$snowParentLiving();}
 @Override public boolean server(){return !level().isClientSide;}@Override public int ticksWithOffset(){return tickCount+getId()*47;}
 @Override public boolean inWater(){return isInWater();}@Override public boolean wet(){return isInWater()||OriginalWeather.rain(level(),blockPosition())||OriginalWeather.rain(level(),BlockPos.containing(getX(),getY()+getBbHeight(),getZ()));}
 @Override public double x(){return getX();}@Override public double y(){return getY();}@Override public double z(){return getZ();}
 @Override public float temperature(int x,int z){return level().getBiome(BlockPos.containing(x,getY(),z)).value().getBaseTemperature();}
 @Override public void melt(float amount){hurt(new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MELT)),amount);}
 @Override public boolean air(int x,int y,int z){return level().getBlockState(new BlockPos(x,y,z)).is(Blocks.AIR);}
 @Override public boolean snowMayOccur(int x,int y,int z){var below=new BlockPos(x,y-1,z);return level().getBlockState(below).isFaceSturdy(level(),below,Direction.UP);}
 @Override public void snow(int x,int y,int z){level().setBlock(new BlockPos(x,y,z),Blocks.SNOW.defaultBlockState(),3);}
 @Override public int nextInt(int bound){return random.nextInt(bound);}@Override public float nextFloat(){return random.nextFloat();}
 @Override public void dropSnowball(){spawnAtLocation(Items.SNOWBALL);}
 @Override public double createProjectile(){projectile=new SourceSnowball(level(),this);return projectile.getY();}
 @Override public void aim(double x,double y,double z,float speed,float wander){projectile.shoot(x,y,z,speed,wander);}
 @Override public void bowSound(float volume,float pitch){playSound(SoundEvents.ARROW_SHOOT,volume,pitch);}
 @Override public void spawnProjectile(){level().addFreshEntity(projectile);projectile=null;}
}
