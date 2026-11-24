package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
/** The modern Raider parent remains for the human-approved raid lifecycle;
 * native Witch drinking, combat goals and magic multiplier are bypassed. */
public interface SourceWitchNativeParent {
 void mite$witchRaidGoals();void mite$witchParentAiStep();
 float mite$witchParentMagic(DamageSource source,float amount);
}
