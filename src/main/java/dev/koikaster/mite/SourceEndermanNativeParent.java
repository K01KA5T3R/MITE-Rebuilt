package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
/** Installed on native EnderMan. Each bridge invokes its Monster parent once. */
public interface SourceEndermanNativeParent {
 void mite$endermanParentAiStep();
 void mite$endermanParentCustomAiStep();
 boolean mite$endermanParentHurt(DamageSource source,float amount);
 void mite$endermanParentTarget(LivingEntity target);
 void mite$endermanParentSave(CompoundTag tag);
 void mite$endermanParentLoad(CompoundTag tag);
 void mite$endermanParentSynced(EntityDataAccessor<?> accessor);
 void mite$endermanCreepy(boolean value);
}
