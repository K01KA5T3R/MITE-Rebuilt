package dev.koikaster.mite;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The common registry has no client classes, including on a dedicated server. */
public final class SourceRepairParticles {
 public static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,MiteReforged.ID);
 public static final RegistryObject<SimpleParticleType> REPAIR=PARTICLES.register("source_repair",()->new SimpleParticleType(false));
 private SourceRepairParticles(){}
}
