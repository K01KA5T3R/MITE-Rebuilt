package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
/** bcw manure / level-event2005 -> ItemManure/xl -> bfl beq sprites98/82. */
public final class ClientFertilizerParticles925 {
 public static boolean event(int id,Object rawPos,int data){if(id!=SourceFertilizerFx925.MANURE_EVENT)return false;var level=Minecraft.getInstance().level;if(level!=null)particles(level,(BlockPos)rawPos,2+level.random.nextInt(3),true);return true;}
 public static void growth(Object raw,Object at,int count){if(raw instanceof ClientLevel level)particles(level,(BlockPos)at,count,false);}
 private static void particles(ClientLevel level,BlockPos pos,int count,boolean manure){
  SourceFertilizerRules925.particles(manure,pos.getX(),pos.getY(),pos.getZ(),count,LegacyItemRandom749.RANDOM,new SourceFertilizerRules925.Fx(){
   public double boundsMaxY(){var state=level.getBlockState(pos);if(state.isAir())return 1;var shape=state.getShape(level,pos);return shape.isEmpty()?1:shape.max(Direction.Axis.Y);}
   public void particle(double x,double y,double z,double vx,double vy,double vz){spawn(level,manure,x,y,z,vx,vy,vz);}
  });
 }
 private static void spawn(ClientLevel level,boolean manure,double x,double y,double z,double vx,double vy,double vz){
  var mc=Minecraft.getInstance();var camera=mc.getCameraEntity();if(camera==null)return;
  var setting=mc.options.particles().get();if(setting==ParticleStatus.DECREASED&&level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;
  if(camera.distanceToSqr(x,y,z)>24.0*24.0||setting==ParticleStatus.MINIMAL)return;
  mc.particleEngine.add(new Town(level,manure,x,y,z,vx,vy,vz));
 }
 public static void aura(Object raw,double x,double y,double z,double vx,double vy,double vz){if(raw instanceof ClientLevel level)spawnAura(level,x,y,z,vx,vy,vz);}
 private static void spawnAura(ClientLevel level,double x,double y,double z,double vx,double vy,double vz){var mc=Minecraft.getInstance();var camera=mc.getCameraEntity();if(camera==null)return;var setting=mc.options.particles().get();if(setting==ParticleStatus.DECREASED&&level.random.nextInt(3)==0)setting=ParticleStatus.MINIMAL;if(camera.distanceToSqr(x,y,z)>24.0*24.0||setting==ParticleStatus.MINIMAL)return;mc.particleEngine.add(new Town(level,0,x,y,z,vx,vy,vz));}
 static final class Town extends TextureSheetParticle {
  private static int sharedIndex;
  private final int tile;
  Town(ClientLevel level,boolean manure,double x,double y,double z,double vx,double vy,double vz){this(level,manure?98:82,x,y,z,vx,vy,vz);}
  Town(ClientLevel level,int sourceTile,double x,double y,double z,double vx,double vy,double vz){
   super(level,x,y,z);
   var value=SourceTownRules925.create(OriginalRandomTables.float_1,sharedIndex,new SourceTownRules925.Random(){public int nextInt(){return random.nextInt();}public float nextFloat(){return random.nextFloat();}},Math::random,vx,vy,vz);
   sharedIndex=value.sharedIndex();xd=value.vx();yd=value.vy();zd=value.vz();quadSize=value.size();lifetime=value.lifetime();hasPhysics=false;gravity=0;setSize(.02f,.02f);
   tile=sourceTile;if(tile==0)setColor(value.baseColor(),value.baseColor(),value.baseColor());else setColor(tile==98?.4f:1f,tile==98?.3f:1f,tile==98?.2f:1f);
   setSprite(((ClientFertilizerAtlas933)Minecraft.getInstance().particleEngine).mite$fertilizerAtlas().getSprite(Content.id("source_particles")));
  }
  @Override public void tick(){xo=x;yo=y;zo=z;move(xd,yd,zd);xd*=.99;yd*=.99;zd*=.99;if(++age>lifetime)remove();}
  @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*((float)(tile%16)/16);}
  @Override protected float getU1(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*((float)(tile%16)/16+.0624375f);}
  @Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*((float)(tile/16)/16);}
  @Override protected float getV1(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*((float)(tile/16)/16+.0624375f);}
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
 }
 private ClientFertilizerParticles925(){}
}
