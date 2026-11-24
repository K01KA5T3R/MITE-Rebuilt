package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bcw particle_trail -> bfl runegate -> bej TYPE_RUNEGATE. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SourcePhaseClientParticles {
 @SubscribeEvent public static void providers(RegisterParticleProvidersEvent event){event.registerSpriteSet(SourcePhaseParticles.RUNEGATE.get(),Provider::new);}
 public static void trail(BlockPos destination,short count,BlockPos approximateOrigin){
  var client=Minecraft.getInstance();var level=client.level;var camera=client.getCameraEntity();if(level==null||camera==null)return;
  double dx=destination.getX()-approximateOrigin.getX(),dy=destination.getY()-approximateOrigin.getY(),dz=destination.getZ()-approximateOrigin.getZ();
  for(int i=0;i<count;i++){
   double fraction=level.random.nextDouble();
   float vx=(level.random.nextFloat()-.5f)*.2f,vy=(level.random.nextFloat()-.5f)*.2f,vz=(level.random.nextFloat()-.5f)*.2f;
   double x=approximateOrigin.getX()+dx*fraction+(level.random.nextDouble()-.5)*1.0+.5;
   double y=approximateOrigin.getY()+dy*fraction+level.random.nextDouble()*1.0-.5;
   double z=approximateOrigin.getZ()+dz*fraction+(level.random.nextDouble()-.5)*1.0+.5;
   var setting=client.options.particles().get();if(setting==ParticleStatus.DECREASED&&level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;
   if(setting==ParticleStatus.MINIMAL||camera.distanceToSqr(x,y,z)>24.0*24.0)continue;
   client.particleEngine.createParticle(SourcePhaseParticles.RUNEGATE.get(),x,y,z,vx,vy,vz);
  }
 }
 private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
  public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){return new Runegate(level,x,y,z,vx,vy,vz,sprites);}
 }
 private static final class Runegate extends TextureSheetParticle {
  private final double initialX,initialY,initialZ;private final float initialSize;private final int sourceTexture;
  Runegate(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites){
   super(level,x,y,z);initialX=x;initialY=y;initialZ=z;xd=vx;yd=vy;zd=vz;hasPhysics=false;gravity=0;setSize(.2f,.2f);
   float value=random.nextFloat()*.6f+.4f;initialSize=random.nextFloat()*.2f+.5f;
   value=(value+1)*.5f;setColor(.03137255f*value,.4745098f*value,.6f*value);
   lifetime=(int)(Math.random()*10)+40;sourceTexture=(int)(Math.random()*8);setSprite(sprites.get(0,1));
  }
  public void tick(){xo=x;yo=y;zo=z;float fraction=(float)age/(float)lifetime,curve=1-(-fraction+fraction*fraction*2);setPos(initialX+xd*(double)curve,initialY+yd*(double)curve+(double)(1-fraction),initialZ+zd*(double)curve);if(age++>=lifetime)remove();}
  public float getQuadSize(float partial){float fraction=((float)age+partial)/(float)lifetime;fraction=1-fraction;fraction*=fraction;return .1f*(initialSize*(1-fraction));}
  public int getLightColor(float partial){int packed=super.getLightColor(partial);float fraction=(float)age/(float)lifetime;fraction*=fraction;fraction*=fraction;int sky=(packed>>16&255)+(int)(fraction*15*16);return packed&255|Math.min(sky,240)<<16;}
  protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*((float)sourceTexture/16);}
  protected float getU1(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*((float)sourceTexture/16+.0624375f);}
  protected float getV0(){return sprite.getV0();}
  protected float getV1(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*.0624375f;}
  public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
 }
 private SourcePhaseClientParticles(){}
}
