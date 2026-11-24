package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
/** Original Nightwing physiology. No additional weapon-material kill gate is imposed. */
public final class SourceNightwing extends SourcePredatoryBat {
 public SourceNightwing(EntityType<? extends Bat> type,Level level){super(type,level);xpReward=10;}
 @Override protected boolean sourceNightwing(){return true;}
 @Override public MobType getMobType(){return MobType.UNDEAD;}
 @Override public boolean isInvulnerableTo(DamageSource source){return source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)||super.isInvulnerableTo(source);}
 @Override public void aiStep(){if(!level().isClientSide){if(SourceBatContent.sunlight(level(),blockPosition()))hurt(OriginalElementalDamage.sunlight(this),1000);else if(tickCount%40==0){int amount=(int)((.4f-OriginalWeather.brightness(this))*10);if(amount>0)heal(amount);}}super.aiStep();}
}
