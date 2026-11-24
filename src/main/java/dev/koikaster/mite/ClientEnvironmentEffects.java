package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
/** bcw steam_with_hiss: native POOF is old explode, exact source particle count/position/motion and hiss pitch. */
public final class ClientEnvironmentEffects {
 public static void steam(int id){var mc=Minecraft.getInstance();if(mc.level==null)return;var actor=mc.level.getEntity(id);if(actor==null)return;var random=((SourceFireRandom)actor).mite$fireRandom();int count=actor.isInWater()?10:5;for(int i=0;i<count;i++){double dx=random.nextGaussian()*.02f,dy=random.nextGaussian()*.02f,dz=random.nextGaussian()*.02f;mc.level.addParticle(ParticleTypes.POOF,actor.getX()+random.nextFloat()*actor.getBbWidth()*2-actor.getBbWidth(),actor.getY()+random.nextFloat()*actor.getBbHeight(),actor.getZ()+random.nextFloat()*actor.getBbWidth()*2-actor.getBbWidth(),dx,dy,dz);}mc.level.playLocalSound(actor.getX(),actor.getY()+actor.getBbHeight()/2f,actor.getZ(),SoundEvents.FIRE_EXTINGUISH,SoundSource.HOSTILE,.7f,1.6f+(mc.level.random.nextFloat()-mc.level.random.nextFloat())*.4f,false);}
 private ClientEnvironmentEffects(){}
}
