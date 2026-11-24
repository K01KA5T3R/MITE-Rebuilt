package dev.koikaster.mite;
import java.util.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
/** Source bdo quad/nn particle motion; geometry, world height and view-origin APIs are native transports. */
public final class ClientSourceIcon1001 {
 public static void spawn(ClientLevel level,Item item,double x,double y,double z,double vx,double vy,double vz){
  var m=Minecraft.getInstance();var camera=m.getCameraEntity();if(camera==null||m.particleEngine==null)return;
  int setting=m.options.particles().get()==ParticleStatus.ALL?0:m.options.particles().get()==ParticleStatus.DECREASED?1:2;
  double sourceY=camera.getY()+(camera instanceof Player?(double)1.62f:0);
  if(!SourceIconCamera1012.admitted(setting,camera.getX(),sourceY,camera.getZ(),x,y,z,level.random::nextInt))return;
  m.particleEngine.add(new Icon(level,item,x,y,z,vx,vy,vz));
 }
 static final class Icon extends TextureSheetParticle {
  final float jitterU,jitterV;final SourceIconMotion1014.State motion;final World world;
  Icon(ClientLevel level,Item item,double x,double y,double z,double vx,double vy,double vz){
   super(level,x,y,z);
   // Native Particle/SingleQuadParticle's private initialization draws do not own nn.ab.
   var sourceRandom=new java.util.Random();
   var value=SourceIconCrackRules996.create(OriginalRandomTables.float_1,sourceRandom.nextInt(),Math::random,vx,vy,vz);
   xd=value.vx();yd=value.vy();zd=value.vz();quadSize=value.size();lifetime=value.lifetime();gravity=value.gravity();jitterU=value.jitterU();jitterV=value.jitterV();setSize(.2f,.2f);
   motion=new SourceIconMotion1014.State(x,y,z);world=new World(level);syncMotion();
   setSprite(Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(item),level,null,0).getParticleIcon());
  }
  private void syncMotion(){x=motion.x;y=motion.y;z=motion.z;onGround=motion.onGround;var b=motion.box;setBoundingBox(new AABB(b.a,b.b,b.c,b.d,b.e,b.f));}
  @Override public void move(double x,double y,double z){motion.vx=xd;motion.vy=yd;motion.vz=zd;SourceIconMotion1014.move(motion,world,x,y,z);xd=motion.vx;yd=motion.vy;zd=motion.vz;syncMotion();}
  @Override public void tick(){SourceIconCrackRules996.tick(new SourceIconCrackRules996.Tick(){
   public double vx(){return Icon.this.xd;}public double vy(){return Icon.this.yd;}public double vz(){return Icon.this.zd;}
   public int age(){return Icon.this.age;}public int lifetime(){return Icon.this.lifetime;}public float gravity(){return Icon.this.gravity;}
   public void previousPosition(){Icon.this.xo=Icon.this.x;Icon.this.yo=Icon.this.y;Icon.this.zo=Icon.this.z;}
   public void age(int value){Icon.this.age=value;}public void remove(){Icon.this.remove();}
   public void move(double x,double y,double z){Icon.this.move(x,y,z);}public boolean onGround(){return Icon.this.onGround;}
   public void velocity(double x,double y,double z){Icon.this.xd=x;Icon.this.yd=y;Icon.this.zd=z;}
  });}
  public double vx(){return xd;}public double vy(){return yd;}public double vz(){return zd;}public int age(){return age;}public int lifetime(){return lifetime;}public float gravity(){return gravity;}
  @Override public void render(VertexConsumer vertices,Camera camera,float partial){
   var local=Minecraft.getInstance().player;if(local==null)return;
   var coefficients=SourceIconCamera1012.coefficients(local.getXRot(),local.getYRot(),Minecraft.getInstance().options.getCameraType()==CameraType.THIRD_PERSON_FRONT);
   // The current world matrix is already anchored at native Camera.position. Changing that origin here
   // would shift particles relative to all block geometry, especially in third person.
   var origin=camera.getPosition();int light=getLightColor(partial);
   SourceIconQuad1007.render(new SourceIconQuad1007.State(xo,yo,zo,x,y,z,0,0,jitterU,jitterV,quadSize,rCol,gCol,bCol),new SourceIconQuad1007.Camera(origin.x,origin.y,origin.z,coefficients.rotationX(),coefficients.rotationXZ(),coefficients.rotationZ(),coefficients.rotationYZ(),coefficients.rotationXY()),partial,new SourceIconQuad1007.Icon(){public float u(double pixel){return sprite.getU(pixel);}public float v(double pixel){return sprite.getV(pixel);}},new SourceIconQuad1007.Sink(){
    float red,green,blue;public void color(float r,float g,float b){red=r;green=g;blue=b;}
    public void vertex(double x,double y,double z,double u,double v){vertices.vertex(x,y,z).uv((float)u,(float)v).color(red,green,blue,1f).uv2(light).endVertex();}
   });
  }
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.TERRAIN_SHEET;}
 }
 static final class World implements SourceIconMotion1014.World {
  final ClientLevel level;World(ClientLevel level){this.level=level;}
  public double minXZ(){return -30_000_000;}public double maxXZ(){return (double)29_999_999+.9999;}
  // Source 0..255 build space allowed motion down to-255. Preserve that void margin in the retained modern build space.
  public double minY(){return level.getMinBuildHeight()-255.0;}public double maxY(){return level.getMaxBuildHeight()-1.0;}
  static SourceParticleBox1014 box(AABB b){return new SourceParticleBox1014(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ);}
  public List<SourceParticleBox1014> collisions(SourceParticleBox1014 swept){
   var result=new ArrayList<SourceParticleBox1014>();var pos=new BlockPos.MutableBlockPos();
   for(int x=Mth.floor(swept.a);x<=Mth.ceil(swept.d)-1;x++)for(int z=Mth.floor(swept.c);z<=Mth.ceil(swept.f)-1;z++){
    if(!level.hasChunk(x>>4,z>>4))continue;
    for(int y=Mth.floor(swept.b)-1;y<=Mth.ceil(swept.e)-1;y++){
     if(y<level.getMinBuildHeight()||y>=level.getMaxBuildHeight())continue;pos.set(x,y,z);
     for(var local:level.getBlockState(pos).getCollisionShape(level,pos,CollisionContext.empty()).toAabbs()){var bounds=box(local.move(x,y,z));if(bounds.intersects(swept))result.add(bounds);}
    }
   }
   // Original nn.E/g return null; only sq(boat) exposes a collision body to a non-entity particle.
   var range=new AABB(swept.a,swept.b,swept.c,swept.d,swept.e,swept.f).inflate(.25);
   for(var actor:level.getEntities((net.minecraft.world.entity.Entity)null,range,e->e instanceof Boat&&!e.isRemoved())){var b=box(actor.getBoundingBox());if(b.intersects(swept))result.add(b);}
   return result;
  }
  public void afterCollision(SourceIconMotion1014.State s){
   var box=s.box;int minX=Mth.floor(box.a+.001),minY=Mth.floor(box.b+.001),minZ=Mth.floor(box.c+.001),maxX=Mth.floor(box.d-.001),maxY=Mth.floor(box.e-.001),maxZ=Mth.floor(box.f-.001);
   if(minY>=level.getMaxBuildHeight()||maxY<level.getMinBuildHeight())return;
   var pos=new BlockPos.MutableBlockPos();for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
    if(y<level.getMinBuildHeight()||y>=level.getMaxBuildHeight()||!level.hasChunk(x>>4,z>>4))continue;pos.set(x,y,z);var state=level.getBlockState(pos);
    if(state.is(Blocks.COBWEB))s.cobweb();else if(state.is(Blocks.SOUL_SAND)){s.vx*=.4;s.vz*=.4;}
   }
  }
 }
 private ClientSourceIcon1001(){}
}
