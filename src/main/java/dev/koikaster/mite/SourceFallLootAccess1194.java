package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
/** Virtual transports retain native Mob table bookkeeping and equipment callbacks. */
public interface SourceFallLootAccess1194 {
 void mite$fallLootTable1194(DamageSource source,boolean recent);
 void mite$fallLootCustom1194(DamageSource source,int looting,boolean recent);
}
