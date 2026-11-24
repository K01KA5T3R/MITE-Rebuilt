package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
/** Client-only adapter for nn.spawnSteamParticles(10), no sounds. */
final class SourceSummonedClientEffects {
 static void spawn(int id){var world=Minecraft.getInstance().level;if(world==null)return;var mob=world.getEntity(id);if(mob==null)return;var random=((SourceFireRandom)mob).mite$fireRandom();
  for(int i=0;i<10;i++){double dx=random.nextGaussian()*.02f,dy=random.nextGaussian()*.02f,dz=random.nextGaussian()*.02f;world.addParticle(ParticleTypes.POOF,mob.getX()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),mob.getY()+random.nextFloat()*mob.getBbHeight(),mob.getZ()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),dx,dy,dz);}
 }
 private SourceSummonedClientEffects(){}
}
