package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** R195 tq. The native registry identity and Navigation/MoveControl are retained;
 * native merge/wake/melee goals are replaced by the actual source parent decisions. */
public class SourceSilverfish extends Silverfish {
 private final SourceSilverfishLife life;
 public SourceSilverfish(EntityType<? extends Silverfish> type,Level level){
  super(type,level);
  getAttribute(Attributes.MAX_HEALTH).setBaseValue(8);
  getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double).6f);
  getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3);
  getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);setHealth(getMaxHealth());
  life=new SourceSilverfishLife(this);goalSelector.addGoal(5,life);refreshDimensions();
 }
 public static AttributeSupplier.Builder attributes(){return Silverfish.createAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.MOVEMENT_SPEED,(double).6f).add(Attributes.ATTACK_DAMAGE,3).add(Attributes.FOLLOW_RANGE,16);}
 @Override protected void registerGoals(){}
 public SourceSilverfishLife sourceLife(){return life;}
 @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(.3f,.7f);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 @Override public int getExperienceReward(){return getType()==EntityType.SILVERFISH?5:10;}
 @Override protected net.minecraft.resources.ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
 /** on's old movement uses a .1 motor and .6 forward input. Native control's
  * requested attribute remains the source forward input, as for other old AI. */
 @Override public void setSpeed(float requested){super.setSpeed(.1f);setZza(requested);}
 @Override public void tick(){if(!level().isClientSide&&life!=null)life.beforeTick();super.tick();}
 protected boolean canTakePlayerSnowball(){return false;}
 @Override public boolean hurt(DamageSource source,float amount){
  if(source.getDirectEntity() instanceof Snowball&&!SourceSnowballTraits.proper(source)){if(source.getEntity() instanceof Player&&!canTakePlayerSnowball())return false;amount=1;}
  if(level().isClientSide||life==null)return super.hurt(source,amount);
  try(var frame=SourceParentHurtFrame.begin(this,outcome->life.afterDamage(source,outcome))){
   boolean accepted=super.hurt(source,amount);var outcome=frame.finish();
   if(outcome.negativelyAffected()&&(source.getEntity()!=null||source.is(DamageTypes.MAGIC)))life.alert();
   return accepted;
  }
 }
 @Override public boolean doHurtTarget(Entity target){
  float before=target instanceof LivingEntity living?living.getHealth():0;boolean hit=super.doHurtTarget(target);
  if(target instanceof LivingEntity living&&living.isAlive()&&living.getHealth()<before){
   if(getType()==SourceSilverfishContent.HOARY.get())living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,5),this);
   else if(getType()==SourceSilverfishContent.COPPERSPINE.get())living.addEffect(new MobEffectInstance(MobEffects.POISON,480,0),this);
  }
  return hit;
 }
 @Override public float getWalkTargetValue(BlockPos pos,LevelReader world){var below=world.getBlockState(pos.below());return below.is(Blocks.STONE)||below.is(Blocks.DEEPSLATE)?10:(SourceArachnidWorld.cameFromSpawner(this)?.7f:.5f)-SourceArachnidWorld.pointBrightness(level(),pos);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(life!=null)life.save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(life!=null)life.load(tag);}
}
