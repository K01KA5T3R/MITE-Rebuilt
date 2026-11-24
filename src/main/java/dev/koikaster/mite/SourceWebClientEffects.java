package dev.koikaster.mite;
import net.minecraft.client.*;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;
import net.minecraft.util.*;
import net.minecraft.world.entity.Entity;
/** bcw burned_up_in_lava/splash and nn's exact particle placement/count loops. */
public final class SourceWebClientEffects {
 private static final RandomSource SOUND_RANDOM=RandomSource.create();
 private static void particle(SimpleParticleType type,double x,double y,double z,double vx,double vy,double vz){
  var mc=Minecraft.getInstance();var camera=mc.getCameraEntity();if(mc.level==null||camera==null)return;
  var setting=mc.options.particles().get();if(setting==ParticleStatus.DECREASED&&mc.level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;
  if(camera.distanceToSqr(x,y,z)>576||setting==ParticleStatus.MINIMAL)return;mc.particleEngine.createParticle(type,x,y,z,vx,vy,vz);
 }
 public static void play(SourceWebEffects.Packet impact){
  var mc=Minecraft.getInstance();if(mc.level==null)return;var retired=SourceWebEffects.takeRetiredClientActor(impact.entity());var actor=mc.level.getEntity(impact.entity());if(actor==null&&retired!=null&&retired.level()==mc.level)actor=retired;var random=actor instanceof SourceFireRandom source?source.mite$fireRandom():RandomSource.create(impact.fallbackSeed());var motion=new net.minecraft.world.phys.Vec3(impact.motionX(),impact.motionY(),impact.motionZ());int kind=impact.kind();
  if(kind==SourceWebProjectile.ImpactFx.LAVA_BURN.ordinal()){
   for(int i=0;i<10;i++){double dx=random.nextGaussian()*(double).02f,dy=random.nextGaussian()*(double).02f,dz=random.nextGaussian()*(double).02f;particle(ParticleTypes.SMOKE,impact.x()+(double)(random.nextFloat()*impact.width()*2f)-(double)impact.width(),impact.y()+(double)(random.nextFloat()*impact.height()),impact.z()+(double)(random.nextFloat()*impact.width()*2f)-(double)impact.width(),dx,dy,dz);}
   mc.level.playLocalSound(impact.x(),impact.y()+(double)(impact.height()/2f),impact.z(),SoundEvents.FIRE_EXTINGUISH,SoundSource.HOSTILE,.4f,2f+SOUND_RANDOM.nextFloat()*.4f,false);
  }else if(kind==SourceWebProjectile.ImpactFx.SPLASH.ordinal()){
   float y=Mth.floor(impact.minY());
   for(int i=0;(float)i<1f+impact.width()*20f;i++){float dx=(random.nextFloat()*2f-1f)*impact.width(),dz=(random.nextFloat()*2f-1f)*impact.width();particle(ParticleTypes.BUBBLE,impact.x()+(double)dx,(double)(y+1f),impact.z()+(double)dz,motion.x,motion.y-(double)(random.nextFloat()*.2f),motion.z);}
   for(int i=0;(float)i<1f+impact.width()*20f;i++){float dx=(random.nextFloat()*2f-1f)*impact.width(),dz=(random.nextFloat()*2f-1f)*impact.width();particle(ParticleTypes.SPLASH,impact.x()+(double)dx,(double)(y+1f),impact.z()+(double)dz,motion.x,motion.y,motion.z);}
   float volume=(float)Math.sqrt(motion.x*motion.x*(double).2f+motion.y*motion.y+motion.z*motion.z*(double).2f)*.2f;volume=Math.min(volume,1);
   mc.level.playLocalSound(impact.x(),impact.y()+(double)(impact.height()/2f),impact.z(),SoundEvents.GENERIC_SPLASH,SoundSource.HOSTILE,volume,1f+(SOUND_RANDOM.nextFloat()-SOUND_RANDOM.nextFloat())*.4f,false);
  }
 }
 private SourceWebClientEffects(){}
}
