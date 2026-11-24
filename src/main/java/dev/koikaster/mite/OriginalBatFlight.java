package dev.koikaster.mite;

/** R195 port. ro.bi flight order; native adapter must own hanging geometry,
 * shared parent AI, Y-coordinate adaptation and actual collision dispatch. */
public final class OriginalBatFlight {
 public record Point(double x,double y,double z){}
 public interface World {
  void parentAi();boolean resting();void resting(boolean value);
  Integer newHangingY();void hangingY(Integer value);void wakeSound();
  boolean preventedFromHanging();void unhangPosition(int anchorY);
  double x();double y();double z();int blockX();int blockZ();int minimumFlightY();
  boolean air(int x,int y,int z);float scale();Point targetEyes();
  int ticksWithOffset();int nextInt(int bound);float nextFloat();
  double motionX();double motionY();double motionZ();void motion(double x,double y,double z);
  float yaw();void yaw(float value);void headYaw(float value);void forward(float value);
 }
 private int[] destination;
 private float waverX,waverY,waverZ;
 private boolean initialHangAttempted;
 public boolean initialHangAttempted(){return initialHangAttempted;}
 public void initialHangAttempted(boolean value){initialHangAttempted=value;}
 public void step(World world){
  world.parentAi();
  if(world.resting()){
   Integer anchor=world.newHangingY();world.hangingY(anchor);
   if(anchor==null){world.resting(false);world.wakeSound();}
   else{
    if(world.nextInt(200)==0)world.headYaw(world.nextInt(360));
    if(world.preventedFromHanging()||world.nextInt(1000)==0){world.resting(false);world.wakeSound();world.unhangPosition(anchor);}
   }
   return;
  }
  if(destination!=null&&(!world.air(destination[0],destination[1],destination[2])||destination[1]<world.minimumFlightY()))destination=null;
  int x=world.blockX(),z=world.blockZ();
  if(destination==null||world.nextInt(30)==0||distanceSquared(destination,x,(int)Math.floor(world.y()),z)<4){
   int range=(int)(7f*world.scale());
   destination=new int[]{x+world.nextInt(range)-world.nextInt(range),(int)Math.floor(world.y())+world.nextInt(6)-2,z+world.nextInt(range)-world.nextInt(range)};
  }
  if(destination[1]<world.minimumFlightY())destination[1]=world.minimumFlightY();
  double dx=destination[0]+.5-world.x(),dy=destination[1]+.1-world.y(),dz=destination[2]+.5-world.z();
  Point eye=world.targetEyes();
  if(eye!=null){
   if(eye.y+waverY>=world.minimumFlightY()-.5){dx=eye.x+waverX-world.x();dy=eye.y+waverY-world.y();dz=eye.z+waverZ-world.z();}
   if(world.ticksWithOffset()%20==0){waverX=world.nextFloat()-.5f;waverY=world.nextFloat()-.5f;waverZ=world.nextFloat()-.5f;}
  }
  double mx=world.motionX()+(Math.signum(dx)*.5-world.motionX())*(double).1f;
  double my=world.motionY()+(Math.signum(dy)*(double).7f-world.motionY())*(double).1f;
  double mz=world.motionZ()+(Math.signum(dz)*.5-world.motionZ())*(double).1f;
  world.motion(mx,my,mz);
  float angle=(float)(Math.atan2(mz,mx)*180/Math.PI)-90f;
  world.forward(.5f);world.yaw(world.yaw()+wrap(angle-world.yaw()));
  if(!world.preventedFromHanging()&&world.nextInt(initialHangAttempted?10:1)==0){Integer anchor=world.newHangingY();if(anchor!=null){world.hangingY(anchor);world.resting(true);}}
  initialHangAttempted=true;
 }
 private static float distanceSquared(int[] p,int x,int y,int z){float dx=p[0]-x,dy=p[1]-y,dz=p[2]-z;return dx*dx+dy*dy+dz*dz;}
 private static float wrap(float value){value%=360f;if(value>=180f)value-=360f;if(value< -180f)value+=360f;return value;}
}
