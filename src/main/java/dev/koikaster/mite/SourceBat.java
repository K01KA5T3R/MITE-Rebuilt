package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;

/** Actual ro/rn/og owner. Predatory leaves share flight, source parent memory and native result capture. */
public class SourceBat extends SourceBatFlightOwner {
 private boolean decidedToFlee,fleeing;
 private OriginalParentHarmState harm;
 private final MoveControl parentMove;
 private final LookControl parentLook;
 private final JumpControl parentJump;
 public SourceBat(EntityType<? extends Bat> type,Level level){
  super(type,level);getAttribute(Attributes.MAX_HEALTH).setBaseValue(3*sourceScale());setHealth(getMaxHealth());xpReward=0;
  parentMove=moveControl;parentLook=lookControl;parentJump=jumpControl;
  // Native Mob's final AI invokes controls after customServerAiStep. ro.bi instead
  // invokes all og controls before flight. Keep the real controls, move their call,
  // and make the later native dispatch inert; never run either controller twice.
  moveControl=new MoveControl(this){@Override public void tick(){}};
  lookControl=new LookControl(this){@Override public void tick(){}};
  jumpControl=new JumpControl(this){@Override public void tick(){}};
 }
 public static AttributeSupplier.Builder attributes(float scale){return Bat.createAttributes().add(Attributes.MAX_HEALTH,3*scale).add(Attributes.ATTACK_DAMAGE,scale).add(Attributes.FOLLOW_RANGE,32);}
 @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return dimensions.height*.85f;}
 @Override public int getExperienceReward(){return xpReward;}
 private OriginalParentHarmState harm(){if(harm==null)harm=new OriginalParentHarmState(this,new OriginalParentHarmState.Flags(){public boolean hasDecidedToFlee(){return decidedToFlee;}public void hasDecidedToFlee(boolean v){decidedToFlee=v;}public boolean fleeing(){return fleeing;}public void fleeing(boolean v){fleeing=v;}});return harm;}
 @Override public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader reader){return SourceBatContent.unobstructed(this,reader);}
 @Override protected void sourceParentAi(){parentMove.tick();parentLook.tick();parentJump.tick();}
 @Override protected void sourceCollision(Entity other){sourceCollisionSound();}
 @Override public void tick(){if(!level().isClientSide)harm().beforeTick();super.tick();}
 @Override public boolean hurt(DamageSource source,float amount){
  if(isInvulnerableTo(source)||!isAlive()||source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)&&hasEffect(MobEffects.FIRE_RESISTANCE))return false;
  try(var scope=SourceBatParentDamageScope.begin(this,source);var frame=SourceParentHurtFrame.begin(this,outcome->{if(!level().isClientSide)harm().afterDamage(source,outcome.negativelyAffected());})){
   boolean accepted=((SourceBatNativeParent)this).mite$batParentHurt(source,amount);frame.finish();
   boolean wake=!level().isClientSide&&isResting()&&scope.hasParentResult();
   if(wake)setResting(false);return accepted||wake;
  }
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);harm().save(tag);}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);harm().load(tag);}
}
