package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** aps ambient emission -> bfl filtering -> bej's two ordinary portal types.
 * The already imported runegate provider and atlas are reused unchanged. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SourceRuneGateClient {
 @SubscribeEvent public static void providers(RegisterParticleProvidersEvent event){
  event.registerSpriteSet(SourceRunestoneContent.UNDERWORLD.get(),sprites->new Provider(sprites,false));
  event.registerSpriteSet(SourceRunestoneContent.NETHER.get(),sprites->new Provider(sprites,true));
 }
 public static void animate(Object rawState,Object rawLevel,Object rawPos,Object rawRandom){
  var state=(BlockState)rawState;var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var random=(RandomSource)rawRandom;
  var role=state.getValue(SourcePortalState.DESTINATION);boolean overworld=role==SourcePortalState.Destination.OVERWORLD;
  boolean rune=state.getValue(SourcePortalState.RUNEGATE);
  if(random.nextInt(100)==0)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,
   overworld&&(rune||level.dimension()==Level.OVERWORLD)?SourceRunestoneContent.SOUND.get():SoundEvents.PORTAL_AMBIENT,SoundSource.BLOCKS,.5f,random.nextFloat()*.4f+.8f,false);
  var type=overworld?SourcePhaseParticles.RUNEGATE.get():role==SourcePortalState.Destination.NETHER?SourceRunestoneContent.NETHER.get():SourceRunestoneContent.UNDERWORLD.get();
  var client=Minecraft.getInstance();var camera=client.getCameraEntity();
  for(int i=0;i<4;i++){
   double x=(float)pos.getX()+random.nextFloat(),y=(float)pos.getY()+random.nextFloat(),z=(float)pos.getZ()+random.nextFloat();
   int sign=random.nextInt(2)*2-1;double vx=((double)random.nextFloat()-.5)*.5,vy=((double)random.nextFloat()-.5)*.5,vz=((double)random.nextFloat()-.5)*.5;
   if(!level.getBlockState(pos.west()).is(Blocks.NETHER_PORTAL)&&!level.getBlockState(pos.east()).is(Blocks.NETHER_PORTAL)){x=pos.getX()+.5+.25*sign;vx=random.nextFloat()*2f*sign;}
   else{z=pos.getZ()+.5+.25*sign;vz=random.nextFloat()*2f*sign;}
   var setting=client.options.particles().get();if(setting==ParticleStatus.DECREASED&&level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;
   if(camera==null||setting==ParticleStatus.MINIMAL||camera.distanceToSqr(x,y,z)>24*24)continue;
   client.particleEngine.createParticle(type,x,y,z,vx,vy,vz);
  }
 }
 private record Provider(SpriteSet sprites,boolean nether) implements ParticleProvider<SimpleParticleType>{
  public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double vx,double vy,double vz){return new Portal(level,x,y,z,vx,vy,vz,sprites,nether);}
 }
 private static final class Portal extends TextureSheetParticle {
  private final double initialX,initialY,initialZ;private final float initialSize;private final int sourceTexture;
  Portal(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites,boolean nether){
   super(level,x,y,z);initialX=x;initialY=y;initialZ=z;xd=vx;yd=vy;zd=vz;hasPhysics=false;gravity=0;setSize(.2f,.2f);
   float value=random.nextFloat()*.6f+.4f;initialSize=random.nextFloat()*.2f+.5f;
   if(nether)setColor(.74509805f*value,.14509805f*value,.043137256f*value);else setColor(.9f*value,.3f*value,value);
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
 private SourceRuneGateClient(){}
}
