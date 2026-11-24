package dev.koikaster.mite;

/** Retained sb leaf decisions. Native world,damage,projectile and block support
 * transport are supplied by the concrete owner, not substituted with native
 * SnowGolem's per-tick melting or modern ranged goal. */
public final class SourceSnowGolemRules {
 public interface Host {
  void parentLiving();boolean server();int ticksWithOffset();boolean inWater();boolean wet();
  double x();double y();double z();float temperature(int x,int z);void melt(float amount);
  boolean air(int x,int y,int z);boolean snowMayOccur(int x,int y,int z);void snow(int x,int y,int z);
  int nextInt(int bound);float nextFloat();void dropSnowball();
  double createProjectile();void aim(double x,double y,double z,float velocity,float wander);void bowSound(float volume,float pitch);void spawnProjectile();
 }
 public record Target(double x,double y,double z,float eyeHeight){}
 public static void living(Host h){
  h.parentLiving();
  if(h.server()){
   if(h.ticksWithOffset()%(h.inWater()?40:100)==0&&h.wet())h.melt(1);
   else{
    float temperature=h.temperature(floor(h.x()),floor(h.z()));
    if(temperature>=.2f){int rate=temperature<=.4f?4000:temperature<=.6f?3000:temperature<=.8f?2000:temperature<=1?1000:temperature<=1.2f?500:temperature<=1.4f?300:temperature<=1.6f?200:100;if(h.ticksWithOffset()%rate==0)h.melt(1);}
   }
  }
  for(int i=0;i<4;i++){int x=floor(h.x()+(double)((float)(i%2*2-1)*.25f)),y=floor(h.y()),z=floor(h.z()+(double)((float)(i/2%2*2-1)*.25f));if(h.air(x,y,z)&&h.temperature(x,z)<.8f&&h.snowMayOccur(x,y,z))h.snow(x,y,z);}
 }
 public static float damage(boolean melt,boolean arrow,float amount){if(!melt)amount*=2;if(arrow&&amount>1){amount*=.5f;if(amount<1)amount=1;}return amount;}
 public static void attack(Host h,Target target){double projectileY=h.createProjectile(),dx=target.x()-h.x(),dy=target.y()+(double)target.eyeHeight()-(double)1.1f-projectileY,dz=target.z()-h.z();float arc=(float)Math.sqrt(dx*dx+dz*dz)*.2f;h.aim(dx,dy+(double)arc,dz,1.6f,12);h.bowSound(1,1/(h.nextFloat()*.4f+.8f));h.spawnProjectile();}
 public static void drops(Host h,int looting){int count=h.nextInt(8+looting*4);for(int i=0;i<count;i++)h.dropSnowball();}
 public static boolean repairItem(boolean present,boolean snowball){return present&&snowball;}
 public static boolean biological(){return false;}
 public static boolean healsWithTime(){return false;}
 public static boolean canDouseFire(){return true;}
 public static boolean canCatchFire(){return true;}
 public static int experience(){return 0;}
 private static int floor(double value){int i=(int)value;return value<i?i-1:i;}
 private SourceSnowGolemRules(){}
}
