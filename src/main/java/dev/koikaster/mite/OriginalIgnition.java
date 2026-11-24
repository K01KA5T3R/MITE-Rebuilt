package dev.koikaster.mite;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;
/** nn.d seconds>=1/canCatchFire gate and20*seconds+10 before original/native FireProtection dampening. */
public final class OriginalIgnition {
 private static boolean scoped(Entity entity){var key=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());return key!=null&&(key.getNamespace().equals("minecraft")||key.getNamespace().equals(MiteReforged.ID));}
 public static boolean allowed(Object raw,int seconds){var entity=(Entity)raw;return !scoped(entity)||seconds>=1&&!entity.fireImmune();}
 public static int ticks(int original,Object raw){return scoped((Entity)raw)?original+10:original;}
 private OriginalIgnition(){}
}
