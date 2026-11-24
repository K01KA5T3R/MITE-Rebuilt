package dev.koikaster.mite;

/** Retained ub/nk behavior. Human V058-V060 remove generated-village economy,
 * professions, trade refresh/rewards and breeding; no substitute economy. */
public final class SourceVillagerRules {
 public static final class State {public int villageDelay;public boolean generatedVillageBonus,playing;}
 public record Point(int x,int y,int z){}
 public record Village(Point center,int radius){}
 public interface Host {
  State state();int nextInt(int bound);float nextFloat();double nextGaussian();
  double x();double y();double z();float width();float height();
  void enqueueVillage(int x,int y,int z);Village villageAt(int x,int y,int z,int range);void bind(Village village);void boostKnownPlayers(int amount);
  void parentAi();void parentLiving();boolean client();int age();void age(int value);void resize();
  void particle(int kind,double x,double y,double z,double dx,double dy,double dz);void parentEvent(int event);
 }
 public static void ai(Host h){
  var s=h.state();if(--s.villageDelay<=0){int x=floor(h.x()),y=floor(h.y()),z=floor(h.z());h.enqueueVillage(x,y,z);s.villageDelay=70+h.nextInt(50);var village=h.villageAt(x,y,z,32);h.bind(village);if(village!=null&&s.generatedVillageBonus){s.generatedVillageBonus=false;h.boostKnownPlayers(5);}}
  h.parentAi();
 }
 public static void living(Host h){h.parentLiving();if(h.client())h.resize();else{int age=h.age();if(age<0)h.age(age+1);else if(age>0)h.age(age-1);}}
 public static int growingAge(int previous,int requested,boolean cauldron,boolean headPassable){return previous<0&&requested>=0&&cauldron&&!headPassable?-1:requested;}
 public static void event(Host h,int event){if(event==12||event==13||event==14)particles(h,event);else h.parentEvent(event);}
 private static void particles(Host h,int kind){for(int i=0;i<5;i++){double dx=h.nextGaussian()*.02,dy=h.nextGaussian()*.02,dz=h.nextGaussian()*.02;h.particle(kind,h.x()+(double)(h.nextFloat()*h.width()*2f)-(double)h.width(),h.y()+1+(double)(h.nextFloat()*h.height()),h.z()+(double)(h.nextFloat()*h.width()*2f)-(double)h.width(),dx,dy,dz);}}
 public static float pathWeight(boolean raining,float parent){return raining?-1f:parent;}
 public static int experience(){return 0;}
 private static int floor(double value){int i=(int)value;return value<i?i-1:i;}
 private SourceVillagerRules(){}
}
