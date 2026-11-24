package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
/** Installed on Zombie, whose direct Monster parent is the deliberate bridge
 * boundary. Installing these on ZombifiedPiglin would still dispatch through
 * Zombie under JVM ACC_SUPER and retain unwanted conversion/reinforcements. */
public interface SourcePigmanNativeParent {
 void mite$pigmanMonsterTick();
 void mite$pigmanMonsterAiStep();
 boolean mite$pigmanMonsterHurt(DamageSource source,float amount);
 void mite$pigmanMonsterSave(CompoundTag tag);
 void mite$pigmanMonsterLoad(CompoundTag tag);
 boolean mite$pigmanMonsterKilledEntity(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.LivingEntity victim);
}
