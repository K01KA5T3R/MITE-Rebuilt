package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
/** Direct ob/FlyingMob parent, bypassing the modern Ghast leaf only. */
public interface SourceGhastNativeParent {
 boolean mite$ghastParentHurt(DamageSource source,float amount);
 boolean mite$ghastParentInvulnerable(DamageSource source);
 void mite$ghastParentSave(CompoundTag tag);
 void mite$ghastParentLoad(CompoundTag tag);
}
