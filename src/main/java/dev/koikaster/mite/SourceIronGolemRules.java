package dev.koikaster.mite;

/** Original sd control flow. Calls to rv/of and to the world remain explicit
 * host boundaries; a non-null source result is independent of hurt's boolean. */
public final class SourceIronGolemRules {
 public static final class State {
  public int villageDelay,attackTicks,flowerTicks;
 }
 public record Point(int x,int y,int z){}
 public record Village(Point center,int radius){}
 public record Hit(boolean present,boolean knockedBack,boolean accepted){}
 public interface Target {boolean monster();void addVertical(double amount);}
 public interface Host {
  State state();int nextInt(int bound);float nextFloat();
  double x();double y();double z();double motionX();double motionZ();double yOffset();float width();double boxMinY();
  Village villageAt(int x,int y,int z,int extra);void village(Village value);void miteClearVillageRestriction();void restrict(Point center,int radius);
  void parentAi();void parentLiving();void parentPush(Target target);void setTarget(Target target);void parentEvent(int event);
  int block(int x,int y,int z);int metadata(int x,int y,int z);
  void particle(int block,int metadata,double x,double y,double z,double dx,double dy,double dz);
  void event(int event);Hit damage(Target target,float amount);void throwSound();void drop(boolean rose);
 }
 public static void ai(Host h){
  var s=h.state();if(--s.villageDelay<=0){s.villageDelay=70+h.nextInt(50);var v=h.villageAt(floor(h.x()),floor(h.y()),floor(h.z()),32);h.village(v);if(v==null)h.miteClearVillageRestriction();else h.restrict(v.center(),(int)((float)v.radius()*.6f));}h.parentAi();
 }
 public static void living(Host h){
  h.parentLiving();var s=h.state();if(s.attackTicks>0)--s.attackTicks;if(s.flowerTicks>0)--s.flowerTicks;
  if(h.motionX()*h.motionX()+h.motionZ()*h.motionZ()>2.500000277905201E-7&&h.nextInt(5)==0){int x=floor(h.x()),y=floor(h.y()-(double).2f-h.yOffset()),z=floor(h.z()),block=h.block(x,y,z);if(block>0)h.particle(block,h.metadata(x,y,z),h.x()+((double)h.nextFloat()-.5)*(double)h.width(),h.boxMinY()+.1,h.z()+((double)h.nextFloat()-.5)*(double)h.width(),4*((double)h.nextFloat()-.5),.5,((double)h.nextFloat()-.5)*4);}
 }
 public static void push(Host h,Target target){if(target.monster()&&h.nextInt(20)==0)h.setTarget(target);h.parentPush(target);}
 public static Hit attack(Host h,Target target){
  h.state().attackTicks=10;h.event(4);var result=h.damage(target,(float)(7+h.nextInt(15)));if(!result.present())return result;if(result.knockedBack())target.addVertical((double).4f);h.throwSound();return result;
 }
 public static void event(Host h,int event){if(event==4){h.state().attackTicks=10;h.throwSound();}else if(event==11)h.state().flowerTicks=400;else h.parentEvent(event);}
 public static void flower(Host h,boolean value){h.state().flowerTicks=value?400:0;h.event(11);}
 /** This is sd.dropFewItems, separate from of's massive-fall dispatch gate. */
 public static void drops(Host h,boolean recent,boolean massiveFall,int looting){int count=h.nextInt(3);for(int i=0;i<count;i++)h.drop(true);if(recent&&!massiveFall){count=3+h.nextInt(3+looting);for(int i=0;i<count;i++)h.drop(false);}}
 public static boolean canTarget(boolean playerCreated,boolean player,boolean parent){return playerCreated&&player?false:parent;}
 public static int experience(int parent){return parent*4;}
 public static float reach(float parent){return parent+.5f;}
 public static int air(int previous){return previous;}
 public static boolean catchesFire(){return false;}
 public static boolean harmedByFire(){return false;}
 private static int floor(double value){int i=(int)value;return value<i?i-1:i;}
 private SourceIronGolemRules(){}
}
