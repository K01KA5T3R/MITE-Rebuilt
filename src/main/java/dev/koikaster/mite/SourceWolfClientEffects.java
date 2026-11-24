package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.particles.ParticleTypes;
/** bcw heal -> nn.spawnParticle(heart,0): one heart centered above the owner's head. */
public final class SourceWolfClientEffects {
 public static void heal(int entity){var mc=Minecraft.getInstance();if(mc.level==null)return;var actor=mc.level.getEntity(entity);var camera=mc.getCameraEntity();if(actor==null||camera==null)return;var random=((SourceFireRandom)actor).mite$fireRandom();double x=actor.getX(),y=actor.getY()+actor.getBbHeight()+random.nextFloat()*.2f,z=actor.getZ();var setting=mc.options.particles().get();if(setting==ParticleStatus.DECREASED&&mc.level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;if(camera.distanceToSqr(x,y,z)<=24*24&&setting!=ParticleStatus.MINIMAL)mc.particleEngine.createParticle(ParticleTypes.HEART,x,y,z,0,0,0);}
 private SourceWolfClientEffects(){}
}
