package dev.koikaster.mite;
import net.minecraft.core.particles.ParticleTypes;
/** EntityNetherspawn.c's cosmetic RNG remains entirely on the tracked client. */
final class SourceNetherspawnClient {
 static void tick(SourceNetherspawn actor){
  if(actor.sourceWet())SourceChemicalClientFx.particles(actor,ParticleTypes.POOF,actor.isInWater()?10:1);
  var random=actor.getRandom();actor.level().addParticle(ParticleTypes.LARGE_SMOKE,actor.getX()+(random.nextDouble()-.5)*(double)actor.getBbWidth(),actor.getY()+random.nextDouble()*(double)actor.getBbHeight(),actor.getZ()+(random.nextDouble()-.5)*(double)actor.getBbWidth(),0,0,0);
 }
 private SourceNetherspawnClient(){}
}
