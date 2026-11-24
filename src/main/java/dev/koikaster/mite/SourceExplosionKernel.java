package dev.koikaster.mite;

import java.util.*;

/** Direct abr algorithm. Native world owners implement all Host operations;
 * this kernel never invokes the modern explosion ray/damage calculation.
 * Source block resistance is aqz.cG (hardness), NOT modern blast resistance. */
public final class SourceExplosionKernel {
 public record Vector(double x,double y,double z){}
 public record Box(double minX,double minY,double minZ,double maxX,double maxY,double maxZ){}
 public record Pos(int x,int y,int z){
  // aco comes unchanged from the original1.6.4 client jar, not the patch archive.
  @Override public int hashCode(){return x*8976890+y*981131+z;}
 }
 public interface EntityView {
  Object identity();boolean handleExplosion(SourceExplosionKernel explosion);
  double x();double y();double z();double eyeHeight();Box box();
  boolean item();boolean player();void damage(SourceExplosionKernel explosion,float damage);
  double dampenedKnockback(double impact);void addMotion(double x,double y,double z);
 }
 public interface Livestock {boolean removed();void spookedUntil(long time);}
 public interface BlockView {
  float resistance(SourceExplosionKernel explosion);boolean chargedWitherProtected();
  boolean mycelium();boolean solidStandardForm();
 }
 public interface Host {
  float nextWorldFloat();int nextWorldInt(int bound);int nextFireInt(int bound);long totalTime();
  /** Air is null. Retain the original block object across phaseB clearing. */
  BlockView block(Pos position);
  boolean exploderCanDestroy(SourceExplosionKernel explosion,Pos position,BlockView block,float force);
  /** Drop setExploded info BEFORE setAir, as in abr.doPreExplosionA. */
  void breakFragile(SourceExplosionKernel explosion,Pos position,BlockView block);
  void setAir(Pos position);void clearParticleBlock(Pos position);void myceliumToDirtAndUndermine(Pos position);
  /** Exact loaded AABB order, excluding only the exploder. */
  List<? extends EntityView> entities(Box bounds);
  List<? extends Livestock> livestock(Box bounds);
  boolean physicalLine(Vector from,Vector to);
  void centerEffect(double x,double y,double z,float pitch,boolean huge);
  void blockParticles(Vector midpoint,Vector blockPoint,Vector motion);
  /** The particles branch has ALREADY cleared with flag2 before this call.
   * Construct current BlockBreakInfo, then captured.onBlockAboutToBeBroken, conditional
   * drop, setAir flag3 and captured.onBlockDestroyedByExplosion. */
  void breakPhaseB(SourceExplosionKernel explosion,Pos position,BlockView captured);
  void setFire(Pos position);
 }
 private final Host host;private final double x,y,z;private final float blockSize,livingSize;
 static float livingRadius(float power){return power*4f;}
 private final boolean chargedWither;private boolean flaming;private final boolean smoking;
 private final List<Pos> affected=new ArrayList<>();private final Map<Object,Vector> playerKnockback=new HashMap<>();
 public SourceExplosionKernel(Host host,double x,double y,double z,float blockSize,float livingSize,boolean flaming,boolean smoking,boolean chargedWither,boolean infernalOrNetherspawn){
  this.host=Objects.requireNonNull(host);this.x=x;this.y=y;this.z=z;this.blockSize=blockSize;this.livingSize=livingSize;this.flaming=flaming||infernalOrNetherspawn;this.smoking=smoking;this.chargedWither=chargedWither;
 }
 private static Pos at(double x,double y,double z){return new Pos((int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z));}
 private static boolean edge(int x,int y,int z){return x==0||x==15||y==0||y==15||z==0||z==15;}
 private static Vector direction(int x,int y,int z){double dx=(float)x/15f*2f-1f,dy=(float)y/15f*2f-1f,dz=(float)z/15f*2f-1f,length=Math.sqrt(dx*dx+dy*dy+dz*dz);return new Vector(dx/length,dy/length,dz/length);}
 private void preExplosion(){
  for(int a=0;a<16;a++)for(int b=0;b<16;b++)for(int c=0;c<16;c++){
   if(!edge(a,b,c))continue;var direction=direction(a,b,c);float force=blockSize*(.7f+host.nextWorldFloat()*.6f);force*=2;force*=1.25f;double px=x,py=y,pz=z;float step=.3f;
   while(force>0){
    var pos=at(px,py,pz);var block=host.block(pos);
    if(block!=null){float resistance=block.resistance(this);force=resistance<0||resistance>.1f?0:force-(resistance+.3f)*step;}
    if(force>0&&host.exploderCanDestroy(this,pos,block,force)){if(block!=null)host.breakFragile(this,pos,block);else host.setAir(pos);}
    else if(force>=0&&y>pos.y&&block!=null&&block.mycelium())host.myceliumToDirtAndUndermine(pos);
    px+=direction.x*(double)step;py+=direction.y*(double)step;pz+=direction.z*(double)step;force-=step*.75f;
   }
  }
 }
 public void phaseA(){
  preExplosion();var positions=new HashSet<Pos>();
  for(int a=0;a<16;a++)for(int b=0;b<16;b++)for(int c=0;c<16;c++){
   if(!edge(a,b,c))continue;var direction=direction(a,b,c);double px=x,py=y,pz=z;float step=.3f;
   for(float force=blockSize*(.7f+host.nextWorldFloat()*.6f);force>0;force-=step*.75f){
    var pos=at(px,py,pz);var block=host.block(pos);
    if(block!=null){float resistance=block.resistance(this);if(resistance>.8f&&chargedWither&&!block.chargedWitherProtected())resistance=.8f;force=resistance<0?0:force-(resistance+.3f)*step;}
    if(force>0&&host.exploderCanDestroy(this,pos,block,force))positions.add(pos);
    px+=direction.x*(double)step;py+=direction.y*(double)step;pz+=direction.z*(double)step;
   }
  }
  affected.addAll(positions);float radius=livingRadius(livingSize);
  var bounds=new Box(Math.floor(x-radius-1),Math.floor(y-radius-1),Math.floor(z-radius-1),Math.floor(x+radius+1),Math.floor(y+radius+1),Math.floor(z+radius+1));
  for(var entity:host.entities(bounds)){
   if(entity.handleExplosion(this))continue;double dx=entity.x()-x,dy=entity.y()-y,dz=entity.z()-z;double normalized=(double)(float)Math.sqrt(dx*dx+dy*dy+dz*dz)/(double)radius;
   if(normalized>1)continue;
   dy=entity.y()+entity.eyeHeight()-y;double length=(double)(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(length==0)continue;dx/=length;dy/=length;dz/=length;
   double impact=(1-normalized)*exposure(entity.box());float damage=radius*(float)impact*4f;damage*=.6666667f;if(damage>=.5f)entity.damage(this,damage);
   double push=entity.dampenedKnockback(impact);if(!entity.item())entity.addMotion(dx*push,dy*push,dz*push);if(entity.player())playerKnockback.put(entity.identity(),new Vector(dx*impact,dy*impact,dz*impact));
  }
  var herdBounds=new Box(x-((double)radius-1)*4,y-((double)radius-1)*4,z-((double)radius-1)*4,x+((double)radius+1)*4,y+((double)radius+1)*4,z+((double)radius+1)*4);
  for(var animal:host.livestock(herdBounds))if(!animal.removed())animal.spookedUntil(host.totalTime()+400L+host.nextWorldInt(400));
 }
 /** abw.getBlockDensity float stepping and source physical reach. Native modern
  * Explosion.getSeenPercent offsets/selection must not silently replace it. */
 private float exposure(Box box){
  double sx=1/((box.maxX-box.minX)*2+1),sy=1/((box.maxY-box.minY)*2+1),sz=1/((box.maxZ-box.minZ)*2+1);int hit=0,total=0;var center=new Vector(x,y,z);
  for(float a=0;a<=1;a=(float)((double)a+sx))for(float b=0;b<=1;b=(float)((double)b+sy))for(float c=0;c<=1;c=(float)((double)c+sz)){
   if(host.physicalLine(new Vector(box.minX+(box.maxX-box.minX)*(double)a,box.minY+(box.maxY-box.minY)*(double)b,box.minZ+(box.maxZ-box.minZ)*(double)c),center))hit++;total++;
  }
  return (float)hit/(float)total;
 }
 public void phaseB(boolean particles){
  host.centerEffect(x,y,z,(1f+(host.nextWorldFloat()-host.nextWorldFloat())*.2f)*.7f,blockSize>=2&&smoking);
  if(smoking)for(var pos:affected){
   var captured=host.block(pos);
   if(particles){
    double px=(float)pos.x+host.nextWorldFloat(),py=(float)pos.y+host.nextWorldFloat(),pz=(float)pos.z+host.nextWorldFloat();double dx=px-x,dy=py-y,dz=pz-z,length=(double)(float)Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=length;dy/=length;dz/=length;double strength=.5/(length/(double)blockSize+.1);strength*=(double)(host.nextWorldFloat()*host.nextWorldFloat()+.3f);
    host.blockParticles(new Vector((px+x)/2,(py+y)/2,(pz+z)/2),new Vector(px,py,pz),new Vector(dx*strength,dy*strength,dz*strength));
    host.clearParticleBlock(pos);
   }
   if(captured!=null)host.breakPhaseB(this,pos,captured);
  }
  if(flaming)for(var pos:affected){var block=host.block(pos);var below=host.block(new Pos(pos.x,pos.y-1,pos.z));if(block==null&&below!=null&&below.solidStandardForm()&&host.nextFireInt(3)==0)host.setFire(pos);}
 }
 public List<Pos> affected(){return Collections.unmodifiableList(affected);}
 public Map<Object,Vector> playerKnockback(){return Collections.unmodifiableMap(playerKnockback);}
}
