package dev.koikaster.mite;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
/** The source sunlight damage type has no invented fire or mundane-immunity aspect. */
public final class OriginalElementalDamage {
 public static final ResourceKey<DamageType> SUNLIGHT=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("source_sunlight"));
 public static DamageSource sunlight(LivingEntity actor){return new DamageSource(actor.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SUNLIGHT));}
 public static final ResourceKey<DamageType> WATER=ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("source_water"));
 public static DamageSource water(LivingEntity actor){return new DamageSource(actor.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(WATER));}
 private OriginalElementalDamage(){}
}
