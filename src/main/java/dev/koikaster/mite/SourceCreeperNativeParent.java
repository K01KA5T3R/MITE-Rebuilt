package dev.koikaster.mite;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
/** New bridge must be installed on native Creeper before activating the candidate. */
public interface SourceCreeperNativeParent {
 void mite$creeperParentTarget(net.minecraft.world.entity.LivingEntity target);
 void mite$creeperParentTick();
 boolean mite$creeperParentStrike(Entity target);
 boolean mite$creeperParentFall(float distance,float multiplier,DamageSource source);
}
