package dev.koikaster.mite;
import net.minecraft.core.particles.ParticleTypes;
/** continuous source client fire-elemental wet steam, separate from wet-undead one-shot hiss. */
public final class ClientFireElementalEffects {
 public static void singleSteam(Mobs.Undead mob){var random=mob.getRandom();double x=random.nextGaussian()*.02f,y=random.nextGaussian()*.02f,z=random.nextGaussian()*.02f;mob.level().addParticle(ParticleTypes.POOF,mob.getX()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),mob.getY()+random.nextFloat()*mob.getBbHeight(),mob.getZ()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),x,y,z);}
 public static void smoke(Mobs.Undead mob){var random=mob.getRandom();for(int i=0;i<10;i++){double x=random.nextGaussian()*.02f,y=random.nextGaussian()*.02f,z=random.nextGaussian()*.02f;mob.level().addParticle(ParticleTypes.LARGE_SMOKE,mob.getX()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),mob.getY()+random.nextFloat()*mob.getBbHeight(),mob.getZ()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),x,y,z);}}
 public static void tick(Mobs.Undead mob){if(!mob.level().isClientSide||!OriginalFireElementalLife.wet(mob))return;var random=mob.getRandom();int count=mob.isInWater()?10:1;for(int i=0;i<count;i++){double x=random.nextGaussian()*.02f,y=random.nextGaussian()*.02f,z=random.nextGaussian()*.02f;mob.level().addParticle(ParticleTypes.POOF,mob.getX()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),mob.getY()+random.nextFloat()*mob.getBbHeight(),mob.getZ()+random.nextFloat()*mob.getBbWidth()*2-mob.getBbWidth(),x,y,z);}}
 private ClientFireElementalEffects(){}
}
