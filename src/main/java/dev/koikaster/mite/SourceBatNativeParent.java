package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
/** Implemented in native Bat, so invokespecial resolves its immediate AmbientCreature parent. */
public interface SourceBatNativeParent {
 void mite$batParentTick();
 boolean mite$batParentHurt(DamageSource source,float amount);
}
