package dev.koikaster.mite;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
/** Common-only registry; the new runegate particle uses the existing unchanged source atlas. */
public final class SourcePhaseParticles {
 public static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,MiteReforged.ID);
 public static final RegistryObject<SimpleParticleType> RUNEGATE=PARTICLES.register("source_runegate",()->new SimpleParticleType(false));
 private SourcePhaseParticles(){}
}
