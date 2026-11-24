package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
/** Original AbstractHorse body remains the fallback for modern horse relatives. */
public interface SourceHorseNativeHurt {
 boolean mite$vanillaHorseHurt(DamageSource source,float amount);
 Object mite$horseState();void mite$horseState(Object value);
 void mite$horseHurtRear();
 net.minecraft.sounds.SoundEvent mite$horseAngrySound();
}
