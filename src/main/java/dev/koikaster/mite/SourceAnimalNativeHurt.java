package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
/** Native Animal bridge. Non-livestock still executes its original method body. */
public interface SourceAnimalNativeHurt {
 boolean mite$vanillaAnimalHurt(DamageSource source,float amount);
 boolean mite$animalParentHurt(DamageSource source,float amount);
 Object mite$animalState();void mite$animalState(Object state);
}
