package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** tv body. Native identity, data/renderer, Raid parent and modern navigation
 * remain; one source owner handles drinking, targeting, throwing and summons. */
public final class SourceWitch extends Witch implements SourceWitchLife.Host,SourceWitchLoot.Host,SourceSpawnOrigin {
 private static final UUID DRINK_UUID=UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
 private static final AttributeModifier DRINK=new AttributeModifier(DRINK_UUID,"Drinking speed penalty",-.25,AttributeModifier.Operation.ADDITION);
 final SourceWitchLife sourceLife=new SourceWitchLife();
 private final OriginalParentHarmState harm;
 private boolean decidedToFlee,fleeing,homeLoaded;private int homeX,homeY,homeZ,curseSeed;private long spookedUntil;
 public SourceWitch(EntityType<? extends Witch> type,Level level){
  super(type,level);getAttribute(Attributes.MAX_HEALTH).setBaseValue(26);setHealth(26);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.25);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);xpReward=20;
  harm=new OriginalParentHarmState(this,new OriginalParentHarmState.Flags(){public boolean hasDecidedToFlee(){return decidedToFlee;}public void hasDecidedToFlee(boolean v){decidedToFlee=v;}public boolean fleeing(){return fleeing;}public void fleeing(boolean v){fleeing=v;}});
  parent().mite$witchRaidGoals();SourceWitchGoals.install(this);if(!level.isClientSide)curseSeed=new java.util.Random().nextInt();
 }
 private SourceWitchNativeParent parent(){return (SourceWitchNativeParent)(Object)this;}
 @Override protected void registerGoals(){}
 @Override public void tick(){if(!level().isClientSide&&harm!=null)harm.beforeTick();super.tick();}
 @Override public void aiStep(){if(sourceLife!=null)sourceLife.beforeParent(this);parent().mite$witchParentAiStep();}
 @Override protected float getDamageAfterMagicAbsorb(DamageSource source,float amount){return parent().mite$witchParentMagic(source,amount);}
 float naturalDefense(DamageSource source){return source!=null&&source.is(DamageTypes.INDIRECT_MAGIC)?10:0;}
 @Override public boolean hurt(DamageSource source,float amount){
  if(level().isClientSide||harm==null)return super.hurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->harm.afterDamage(source,outcome.negativelyAffected()))){boolean result=super.hurt(source,amount);frame.finish();return result;}
 }
 @Override public void die(DamageSource source){if(!level().isClientSide)OriginalCurseEffects.sourceWitchDeath(this);super.die(source);}
 @Override public boolean canAttackType(EntityType<?> type){return type!=EntityType.CREEPER&&type!=EntityType.GHAST;}
 @Override public int getExperienceReward(){return 20;}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){SourceWitchLoot.drop(this,looting);}
 @Override protected SoundEvent getAmbientSound(){return null;}
 @Override protected SoundEvent getHurtSound(DamageSource source){return SourceWitchContent.HURT.get();}
 @Override protected SoundEvent getDeathSound(){return SourceWitchContent.DEATH.get();}
 @Override protected float getSoundVolume(){return .2f;}
 @Override public void playAmbientSound(){
  double nearest=Double.POSITIVE_INFINITY;for(var player:level().players())nearest=Math.min(nearest,distanceToSqr(player));
  if(nearest<=256){if(spookedUntil>level().getGameTime()){if(random.nextInt(2)==0)playSound(SourceWitchContent.HURT.get(),.2f,getVoicePitch());}else spookedUntil=0;return;}if(nearest>4096)return;
  float chance=getTarget() instanceof Player?.3f:!SourceSpecialSpawns.sourceDay(level())?.1f:0;
  if(random.nextFloat()<chance)SourceWitchCackle.send(this,.6f,getVoicePitch());
 }
 @Override public void performRangedAttack(LivingEntity target,float factor){
  if(isDrinkingPotion())return;var potion=new SourceWitchPotion(this,32732);potion.setXRot(potion.getXRot()- -20f);
  var motion=target.getDeltaMovement();double dx=target.getX()+motion.x-getX(),dy=target.getEyeY()-(double)1.1f-getY(),dz=target.getZ()+motion.z-getZ();
  float horizontal=(float)Math.sqrt(dx*dx+dz*dz);int subtype=32732;
  if(horizontal>=8&&!target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))subtype=32698;
  else if(target.getHealth()>=8&&!target.hasEffect(MobEffects.POISON))subtype=32660;
  else if(horizontal<=3&&!target.hasEffect(MobEffects.WEAKNESS)&&random.nextFloat()<.25f)subtype=32696;
  potion.setItem(SourceWitchContent.potion(subtype));float squared=(float)(dx*dx+dz*dz);
  if(target instanceof ServerPlayer){float lead=(float)Math.pow(squared,.5);lead*=.5f+random.nextFloat();var predicted=SourceArachnidPrediction.at(target,lead);dx=predicted.x-getX();dz=predicted.z-getZ();}
  potion.shoot(dx,dy+(double)(horizontal*.2f),dz,.75f,8);
  double correction=squared*.001f-.025f;var velocity=potion.getDeltaMovement();potion.setDeltaMovement(velocity.x*(double)1.2f,(velocity.y+correction)*(double)1.2f,velocity.z*(double)1.2f);level().addFreshEntity(potion);
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){
  super.addAdditionalSaveData(tag);if(sourceLife!=null&&sourceLife.hasSummonedWolves)tag.putBoolean("has_summoned_wolves",true);tag.putInt("curse_random_seed",curseSeed);if(harm!=null)harm.save(tag);
  tag.putInt("spawn_x",homeX);tag.putInt("spawn_y",homeY);tag.putInt("spawn_z",homeZ);tag.putLong("spooked_until",spookedUntil);tag.putBoolean("R195WitchOwner",true);
 }
 @Override public void readAdditionalSaveData(CompoundTag tag){
  super.readAdditionalSaveData(tag);sourceLife.hasSummonedWolves=tag.getBoolean("has_summoned_wolves");sourceLife.summonCountdown=0;sourceLife.summonTarget=null;sourceLife.drinkTimer=0;setUsingItem(false);getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(DRINK_UUID);
  if(tag.contains("curse_random_seed"))curseSeed=tag.getInt("curse_random_seed");else if(tag.getCompound("ForgeData").contains("MiteCurseSeed"))curseSeed=tag.getCompound("ForgeData").getInt("MiteCurseSeed");
  // OriginalOrigin's successful native world-add capture also runs on disk
  // reload. This source owner's one home authority must retain the saved home.
  homeX=tag.getInt("spawn_x");homeY=tag.getInt("spawn_y");homeZ=tag.getInt("spawn_z");homeLoaded=tag.contains("spawn_x");spookedUntil=tag.getLong("spooked_until");if(harm!=null)harm.load(tag);
  if(!tag.getBoolean("R195WitchOwner")&&getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()==16)getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);
 }
 @Override public int mite$originX(){return homeX;}@Override public int mite$originY(){return homeY;}@Override public int mite$originZ(){return homeZ;}
 @Override public void mite$setOrigin(int x,int y,int z){if(!homeLoaded){homeX=x;homeY=y;homeZ=z;}}
 BlockPos home(){return new BlockPos(homeX,homeY,homeZ);}int sourceCurseSeed(){return curseSeed;}
 @Override public boolean client(){return level().isClientSide;}@Override public boolean drinking(){return isDrinkingPotion();}@Override public float nextFloat(){return random.nextFloat();}
 @Override public boolean burning(){return isOnFire();}@Override public boolean fireResistance(){return hasEffect(MobEffects.FIRE_RESISTANCE);}@Override public boolean wounded(){return getHealth()<getMaxHealth();}
 @Override public boolean hasTarget(){return getTarget()!=null;}@Override public boolean speedEffect(){return hasEffect(MobEffects.MOVEMENT_SPEED);}@Override public double targetDistanceSquared(){return getTarget().distanceToSqr(this);}
 @Override public int beginPotion(int subtype){setItemSlot(EquipmentSlot.MAINHAND,SourceWitchContent.potion(subtype));int duration=getMainHandItem().getUseDuration();setUsingItem(true);var speed=getAttribute(Attributes.MOVEMENT_SPEED);speed.removeModifier(DRINK);speed.addTransientModifier(DRINK);return duration;}
 @Override public void finishPotion(){setUsingItem(false);var held=getMainHandItem();setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);if(held.is(Items.POTION))for(var effect:PotionUtils.getMobEffects(held))addEffect(new MobEffectInstance(effect));getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(DRINK);}
 @Override public void magic(){level().broadcastEntityEvent(this,(byte)15);}
 @Override public Object lastHarmingEntity(){return harm==null?null:harm.harming();}@Override public boolean player(Object actor){return actor instanceof Player;}@Override public int summonWolves(Object target){return SourceWitchSummons.summon(this,target);}
 @Override public int nextInt(int bound){return random.nextInt(bound);}
 @Override public void drop(SourceWitchLoot.Item item,int subtype){
  if(item==SourceWitchLoot.Item.POTION){spawnAtLocation(SourceWitchContent.potion(subtype));return;}
  Item value=switch(item){case GLOWSTONE_DUST->Items.GLOWSTONE_DUST;case SUGAR->Items.SUGAR;case REDSTONE->Items.REDSTONE;case SPIDER_EYE->Items.SPIDER_EYE;case GLASS_BOTTLE->Items.GLASS_BOTTLE;case GUNPOWDER->Items.GUNPOWDER;case STICK->Items.STICK;case FLINT_KNIFE->Content.get("flint_knife");case IRON_NUGGET->Items.IRON_NUGGET;case WHEAT_SEEDS->Items.WHEAT_SEEDS;case PUMPKIN_SEEDS->Items.PUMPKIN_SEEDS;case CARROT->Items.CARROT;case POTATO->Items.POTATO;case ONION->Content.ONION.get();case DANDELION->Items.DANDELION;case ALLIUM->Items.ALLIUM;default->throw new IllegalStateException("Source Witch loot");};spawnAtLocation(value);
 }
}
