package dev.koikaster.mite;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bcw/nn/beb/bfl index8, using the already imported, unchanged source atlas. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SourceWitherClientFx {
 @SubscribeEvent public static void providers(RegisterParticleProvidersEvent event){event.registerSpriteSet(SourceWitherFx.VAMPIRIC.get(),Provider::new);}
 public static void gain(int entity){
  var mc=Minecraft.getInstance();if(mc.level==null)return;var actor=mc.level.getEntity(entity);var camera=mc.getCameraEntity();if(actor==null||camera==null)return;var random=((SourceFireRandom)actor).mite$fireRandom();double x=actor.getX(),y=actor.getY()+actor.getBbHeight()+random.nextFloat()*.2f,z=actor.getZ();var setting=mc.options.particles().get();if(setting==ParticleStatus.DECREASED&&mc.level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;if(camera.distanceToSqr(x,y,z)<=24*24&&setting!=ParticleStatus.MINIMAL)mc.particleEngine.createParticle(SourceWitherFx.VAMPIRIC.get(),x,y,z,0,0,0);
 }
 private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz){return new Gain(level,x,y,z,sprites);}}
 private static final class Gain extends TextureSheetParticle {
  final float originalSize;
  Gain(ClientLevel level,double x,double y,double z,SpriteSet sprites){super(level,x,y,z,0,0,0);xd*=(double).01f;yd*=(double).01f;zd*=(double).01f;yd+=.1;quadSize*=.75f;quadSize*=2;originalSize=quadSize;lifetime=16;hasPhysics=true;setSprite(sprites.get(0,1));}
  @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();move(xd,yd,zd);if(y==yo){xd*=1.1;zd*=1.1;}xd*=(double).86f;yd*=(double).86f;zd*=(double).86f;if(onGround){xd*=(double).7f;zd*=(double).7f;}}
  @Override public float getQuadSize(float partial){return originalSize*Mth.clamp(((float)age+partial)/(float)lifetime*32,0,1);}
  @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*.5f;}
  @Override protected float getU1(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(.5f+.0624375f);}
  @Override protected float getV0(){return sprite.getV0();}
  @Override protected float getV1(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*.0624375f;}
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
 }
 private SourceWitherClientFx(){}
}
