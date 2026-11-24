package dev.koikaster.mite;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

/** R195 tg with one on/tm owner. Native identity/model/data synchronization and V103 terrain remain. */
public final class SourceEnderman extends EnderMan {
 private static final UUID ATTACK_ID=UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
 private static final AttributeModifier ATTACK_SPEED=new AttributeModifier(ATTACK_ID,"Attacking speed boost",(double)6.2f,AttributeModifier.Operation.ADDITION);
 private final SourceEndermanAI sourceAi;
 private final SourceEndermanValuables valuables;
 int stareTicks,teleportTicks;private LivingEntity previousTarget;private boolean provoked,decoy;
 public SourceEnderman(EntityType<? extends EnderMan> type,Level world){super(type,world);getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).3f);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);valuables=new SourceEndermanValuables(this);sourceAi=new SourceEndermanAI(this);goalSelector.addGoal(5,sourceAi);}
 @Override protected void registerGoals(){}
 public SourceEndermanAI sourceAi(){return sourceAi;}
 public SourceEndermanValuables valuables(){return valuables;}
 public boolean sourceDecoy(){return decoy;}
 public void makeSourceDecoy(){decoy=true;}
 boolean sourceWet(){return isInWaterRainOrBubble();}
 private SourceEndermanNativeParent parent(){return (SourceEndermanNativeParent)(Object)this;}
 void screaming(boolean value){parent().mite$endermanCreepy(value);}
 @Override public void setTarget(LivingEntity target){parent().mite$endermanParentTarget(target!=null&&target.isRemoved()?null:target);}
 @Override public LivingEntity getTarget(){var target=super.getTarget();if(target!=null&&target.isRemoved()){setTarget(null);return null;}return target;}
 /** of.bg supplies .1 acceleration; on.bl supplies the attribute as forward input. */
 @Override public void setSpeed(float requested){super.setSpeed(.1f);setZza(requested);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return size.height*.85f;}
 @Override public boolean isSensitiveToWater(){return false;}
 @Override public boolean canBeAffected(MobEffectInstance effect){var kind=effect.getEffect();return kind!=MobEffects.REGENERATION&&kind!=MobEffects.POISON&&kind!=MobEffects.WITHER&&super.canBeAffected(effect);}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor){parent().mite$endermanParentSynced(accessor);}
 @Override public void tick(){if(!level().isClientSide&&sourceAi!=null)sourceAi.harm.beforeTick();super.tick();}
 @Override public void aiStep(){beforeParent();if(sourceAi!=null)sourceAi.beforeParent();parent().mite$endermanParentAiStep();if(valuables!=null)valuables.tick();}
 @Override protected void customServerAiStep(){parent().mite$endermanParentCustomAiStep();}
 void beforeParent(){
  if(!level().isClientSide&&sourceWet())hurt(OriginalElementalDamage.water(this),1);
  var target=getTarget();if(previousTarget!=target){var speed=getAttribute(Attributes.MOVEMENT_SPEED);speed.removeModifier(ATTACK_ID);if(target!=null)speed.addTransientModifier(ATTACK_SPEED);}previousTarget=target;
  SourceEndermanBlocks.tick(this);
  for(int i=0;i<2;i++)level().addParticle(ParticleTypes.PORTAL,getX()+(random.nextDouble()-.5)*getBbWidth(),getY()+random.nextDouble()*getBbHeight()-.25,getZ()+(random.nextDouble()-.5)*getBbWidth(),(random.nextDouble()-.5)*2,-random.nextDouble(),(random.nextDouble()-.5)*2);
  boolean teleported=false;
  if(getTarget()==null&&level().isDay()&&!level().isClientSide&&random.nextInt(4)==0){float light=OriginalWeather.brightness(this);if(light>.5f&&level().canSeeSky(blockPosition())&&random.nextFloat()*30<(light-.4f)*2){setTarget(null);screaming(false);provoked=false;if(SourceEndermanGeometry.random(this))teleported=true;}}
  if(!level().isClientSide&&!teleported&&(tickCount+getId()*47)%20==0&&random.nextInt(10)==0&&SourceEndermanGeometry.valuable(this))teleported=true;
  if(!level().isClientSide&&(sourceWet()||isOnFire())){setTarget(null);screaming(false);provoked=false;SourceEndermanGeometry.random(this);}
  if(isCreepy()&&!provoked&&random.nextInt(100)==0&&!level().isClientSide)screaming(false);
  setJumping(false);target=getTarget();if(target!=null)lookAt(target,100,100);
  if(!level().isClientSide&&isAlive()){if(target!=null){if(++teleportTicks>30){if(random.nextInt(2)==0){if(random.nextInt(3)==0)SourceEndermanGeometry.random(this);else SourceEndermanGeometry.toward(this,target);}teleportTicks=0;}}else{screaming(false);teleportTicks=0;}}
 }
 Player findPlayer(){Player nearest=null;double shortest=-1;for(var player:level().players()){if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;double limit=64;if(player.isCrouching())limit*=(double).8f;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));double distance=player.distanceToSqr(getX(),getY()+(double)(getBbHeight()/2f),getZ());if(distance>=limit*limit||shortest!=-1&&distance>=shortest||!hasLineOfSight(player))continue;nearest=player;shortest=distance;}
  if(nearest!=null){if(looking(nearest)){provoked=true;if(stareTicks==0)stare(nearest);if(stareTicks++==5){stareTicks=0;screaming(true);return nearest;}}else stareTicks=0;}return null;
 }
 boolean looking(Player player){
  if(decoy)return false;if(OriginalCurseEffects.enderman(this,player)==1)return true;
  if(SourceEndermanValuables.ender(player.getMainHandItem())){stare(player);stareTicks=5;return true;}
  int count=0;for(var stack:player.getInventory().items)if(SourceEndermanValuables.ender(stack))count+=stack.getCount();for(var stack:player.getInventory().armor)if(SourceEndermanValuables.ender(stack))count+=stack.getCount();
  if(count>0&&random.nextInt(2000)<count){stare(player);stareTicks=5;return true;}
  if(player.getItemBySlot(EquipmentSlot.HEAD).is(Blocks.CARVED_PUMPKIN.asItem())||player.getItemBySlot(EquipmentSlot.HEAD).is(Blocks.PUMPKIN.asItem()))return false;
  var view=player.getViewVector(1).normalize();var delta=new Vec3(getX()-player.getX(),getBoundingBox().minY+(double)(getBbHeight()/2f)-player.getEyeY(),getZ()-player.getZ());double distance=delta.length();return view.dot(delta.normalize())>1-.025/distance&&player.hasLineOfSight(this);
 }
 private void stare(Player player){level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ENDERMAN_STARE,getSoundSource(),1,1);}
 @Override public boolean hasLineOfSight(Entity target){boolean seen=OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,false);if(seen&&target instanceof Player)OriginalDespawn.refresh(this,-1200);return seen;}
 @Override public float getWalkTargetValue(BlockPos pos,LevelReader world){return (SourceArachnidWorld.cameFromSpawner(this)?.7f:.5f)-SourceArachnidWorld.pointBrightness(level(),pos);}
 @Override public boolean hurt(DamageSource source,float amount){
  if(level().isClientSide||sourceAi==null)return parent().mite$endermanParentHurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->sourceAi.afterDamage(source,outcome))){boolean accepted=parent().mite$endermanParentHurt(source,amount);var outcome=frame.finish();
   // tg is a leaf AFTER tm/of: keep leaf teleport RNG after the parent's death/loot work.
   if(outcome.negativelyAffected()){screaming(true);if(source.getEntity() instanceof Player)provoked=true;if(source.getEntity()!=null&&source.getEntity()!=source.getDirectEntity()&&!source.is(DamageTypeTags.IS_EXPLOSION)){provoked=false;for(int i=0;i<64;i++)if(SourceEndermanGeometry.random(this))break;}}
   return accepted;
  }
 }
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recent){int count=random.nextInt(2+looting);for(int i=0;i<count;i++)spawnAtLocation(net.minecraft.world.item.Items.ENDER_PEARL);valuables.drop();}
 @Override public void addAdditionalSaveData(CompoundTag tag){parent().mite$endermanParentSave(tag);var state=getCarriedBlock();if(state!=null)tag.put("carriedBlockState",NbtUtils.writeBlockState(state));else tag.remove("carriedBlockState");if(sourceAi!=null)sourceAi.save(tag);if(valuables!=null)valuables.save(tag);tag.putBoolean("R195EndermanOwner",true);tag.remove("AngerTime");tag.remove("AngryAt");}
 @Override public void readAdditionalSaveData(CompoundTag tag){parent().mite$endermanParentLoad(tag);if(!tag.getBoolean("R195EndermanOwner")){if(getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==7)getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);if(getAttribute(Attributes.FOLLOW_RANGE).getBaseValue()==64)getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);}var state=tag.contains("carriedBlockState",Tag.TAG_COMPOUND)?NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK),tag.getCompound("carriedBlockState")):null;setCarriedBlock(state==null||state.isAir()?null:state);if(sourceAi!=null)sourceAi.load(tag);if(valuables!=null)valuables.load(tag);}
}
