package dev.koikaster.mite;

import net.minecraft.world.damagesource.DamageSource;

/** New031 Spider bridge. Its generated implementation invokespecials the
 * actual native Spider superclass hurt; it must never call virtual hurt and
 * re-enter the Phase leaf or the scoped caller.
 */
public interface SourceParentNativeHurt {
 boolean mite$nativeParentHurt(DamageSource source,float amount);
}
