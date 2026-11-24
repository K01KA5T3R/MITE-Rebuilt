package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 bcw repair effect, nn random placement, bfl selection and EntityRepairFX. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRepairParticles {
 @SubscribeEvent public static void providers(RegisterParticleProvidersEvent event){event.registerSpriteSet(SourceRepairParticles.REPAIR.get(),Provider::new);}

 public static void repair(int entityId){
  var mc=Minecraft.getInstance();if(mc.level==null)return;
  var actor=mc.level.getEntity(entityId);var camera=mc.getCameraEntity();if(actor==null||camera==null)return;
  var random=((SourceFireRandom)actor).mite$fireRandom();
  // nn.O is width; nn.P is height. bcw uses P for the particle count.
  int count=Math.round(actor.getBbHeight()*8f);
  for(int i=0;i<count;i++){
   double x=actor.getX()+(double)(random.nextFloat()*actor.getBbWidth()*2f)-(double)actor.getBbWidth();
   double y=actor.getY()+(double).1f+(double)(random.nextFloat()*actor.getBbHeight());
   double z=actor.getZ()+(double)(random.nextFloat()*actor.getBbWidth()*2f)-(double)actor.getBbWidth();
   var setting=mc.options.particles().get();
   if(setting==ParticleStatus.DECREASED&&mc.level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;
   if(camera.distanceToSqr(x,y,z)>24*24||setting==ParticleStatus.MINIMAL)continue;
   // Use the engine after the source filters, avoiding a second modern distance/settings roll.
   mc.particleEngine.createParticle(SourceRepairParticles.REPAIR.get(),x,y,z,.8,.8,.8);
  }
 }

 private static final class Provider implements ParticleProvider<SimpleParticleType> {
  private final SpriteSet sprites;
  private Provider(SpriteSet sprites){this.sprites=sprites;}
  @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double red,double green,double blue){
   var particle=new Repair(level,x,y,z,sprites);particle.setColor((float)red,(float)green,(float)blue);return particle;
  }
 }

 private static final class Repair extends TextureSheetParticle {
  private static int randomNumberIndex;
  private Repair(ClientLevel level,double x,double y,double z,SpriteSet sprites){
   super(level,x,y,z,0,0.0025f,0);
   setSize(.02f,.02f);
   // Original beg.render multiplies EntityRepairFX.h by .1f.
   quadSize=.1f*(1f+random.nextFloat()*.2f);
   xd=0;yd=(double).0025f;zd=0;
   lifetime=(int)(15.0/((double)OriginalRandomTables.float_1[++randomNumberIndex&32767]*.6+.4));
   hasPhysics=false;gravity=0;setSprite(sprites.get(0,1));
  }
  @Override public void tick(){
   xo=x;yo=y;zo=z;move(xd,yd,zd);
   xd*=(double).99f;yd*=(double).99f;zd*=(double).99f;
   if(++age>lifetime)remove();
  }
  // Keep the complete source atlas unchanged; texture 99 is column 3, row 6.
  @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(3f/16f);}
  @Override protected float getU1(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(3f/16f+.0624375f);}
  @Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*(6f/16f);}
  @Override protected float getV1(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*(6f/16f+.0624375f);}
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
 }
 private ClientRepairParticles(){}
}
