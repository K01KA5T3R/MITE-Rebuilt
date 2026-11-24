package dev.koikaster.mite;
import java.util.List;

/** sk combat/world decisions. Native binding must preserve the answered V023
 * dragon-breath feature; these source rules are not a modern phase manager. */
public final class SourceDragonRules {
 public interface Target {double x();double z();}
 public interface Victim extends Target {boolean living();void push(double x,double y,double z);void damage(float amount);}
 public interface Crystal {boolean removed();}
 public static final class State {public double x,y=100,z;public Target target;public boolean force;public int deathTicks;}
 public record Box(double minX,double minY,double minZ,double maxX,double maxY,double maxZ){}
 public interface Host {
  State state();boolean client();double x();double y();double z();float yaw();void yaw(float value);
  int ticks();int nextInt(int bound);float nextFloat();float health();float maximumHealth();void health(float amount);
  Crystal crystal();void crystal(Crystal value);List<? extends Crystal> crystals();double distanceSquared(Crystal value);void linkedCrystalDamage(float amount);
  List<? extends Target> players();int block(int x,int y,int z);boolean griefing();boolean remove(int x,int y,int z);
  void particle(boolean huge,double x,double y,double z);void deathEvent(int x,int y,int z);
  void move(double x,double y,double z);int orbValue(int total);void orb(int value);void portal(int x,int z);void discard();
 }
 public interface Damage<R> {float amount();void scale(float factor,float floor);boolean playerResponsible();boolean explosion();R parent();}
 public static void crystals(Host h){
  crystals(h,10,1f);
 }
 static void crystals(Host h,int healInterval,float healing){
  var linked=h.crystal();
  if(linked!=null){if(linked.removed()){if(!h.client())h.linkedCrystalDamage(10f);h.crystal(null);}else if(h.ticks()%healInterval==0&&h.health()<h.maximumHealth())h.health(h.health()+healing);}
  if(h.nextInt(10)==0){Crystal best=null;double distance=Double.MAX_VALUE;for(var candidate:h.crystals()){double d=h.distanceSquared(candidate);if(!(d<distance))continue;distance=d;best=candidate;}h.crystal(best);}
 }
 public static void knockBack(double bodyCenterX,double bodyCenterZ,List<? extends Victim> entities){
  for(var entity:entities){if(!entity.living())continue;double x=entity.x()-bodyCenterX,z=entity.z()-bodyCenterZ,d=x*x+z*z;entity.push(x/d*4,(double).2f,z/d*4);}
 }
 public static void bite(List<? extends Victim> entities){for(int i=0;i<entities.size();i++){var entity=entities.get(i);if(entity.living())entity.damage(15f);}}
 public static void chooseTarget(Host h){
  var s=h.state();s.force=false;
  if(h.nextInt(2)==0&&!h.players().isEmpty())s.target=h.players().get(h.nextInt(h.players().size()));
  else{double x,y,z;do{s.x=0;s.y=70f+h.nextFloat()*50f;s.z=0;s.x+=(double)(h.nextFloat()*120f-60f);s.z+=(double)(h.nextFloat()*120f-60f);x=h.x()-s.x;y=h.y()-s.y;z=h.z()-s.z;}while(!(x*x+y*y+z*z>100));s.target=null;}
 }
 public static boolean walls(Host h,Box box){
  int x0=(int)Math.floor(box.minX),y0=(int)Math.floor(box.minY),z0=(int)Math.floor(box.minZ),x1=(int)Math.floor(box.maxX),y1=(int)Math.floor(box.maxY),z1=(int)Math.floor(box.maxZ);boolean blocked=false,broken=false;
  for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++){int block=h.block(x,y,z);if(block==0)continue;if(block!=49&&block!=121&&block!=7&&h.griefing())broken=h.remove(x,y,z)||broken;else blocked=true;}
  if(broken){double x=box.minX+(box.maxX-box.minX)*(double)h.nextFloat(),y=box.minY+(box.maxY-box.minY)*(double)h.nextFloat(),z=box.minZ+(box.maxZ-box.minZ)*(double)h.nextFloat();h.particle(false,x,y,z);}return blocked;
 }
 public static <R> R hurtPart(Host h,boolean head,Damage<R> damage){
  if(!head&&damage.amount()>1f)damage.scale(.25f,1f);
  float angle=h.yaw()*(float)Math.PI/180f,sin=SourceDragonTrig.sin(angle),cos=SourceDragonTrig.cos(angle);
  var s=h.state();s.x=h.x()+(double)(sin*5f)+(double)((h.nextFloat()-.5f)*2f);s.y=h.y()+(double)(h.nextFloat()*3f)+1;s.z=h.z()-(double)(cos*5f)+(double)((h.nextFloat()-.5f)*2f);s.target=null;
  return damage.playerResponsible()||damage.explosion()?damage.parent():null;
 }
 public static <R> R hurtDirect(){return null;}
 public static void death(Host h){
  var s=h.state();++s.deathTicks;
  if(s.deathTicks>=180&&s.deathTicks<=200){float x=(h.nextFloat()-.5f)*8f,y=(h.nextFloat()-.5f)*4f,z=(h.nextFloat()-.5f)*8f;h.particle(true,h.x()+(double)x,h.y()+2+(double)y,h.z()+(double)z);}
  if(!h.client()){if(s.deathTicks>150&&s.deathTicks%5==0)orbs(h,100);if(s.deathTicks==1)h.deathEvent((int)h.x(),(int)h.y(),(int)h.z());}
  h.move(0,(double).1f,0);h.yaw(h.yaw()+20f);
  if(s.deathTicks==200&&!h.client()){orbs(h,200);h.portal((int)Math.floor(h.x()),(int)Math.floor(h.z()));h.discard();}
 }
 private static void orbs(Host h,int total){for(int value;total>0;total-=value){value=h.orbValue(total);h.orb(value);}}
 private SourceDragonRules(){}
}
