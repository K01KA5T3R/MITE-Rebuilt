package dev.koikaster.mite;

/** td.attackEntity direct leaf. Staged only: the actual legacy-parent caller,
 * wet/hover/particles, synchronization and projectile owner are not bound yet. */
public final class OriginalBlazeAttack {
 public interface Target {double x();double z();double minY();double maxY();float height();double predictedX(float lead);double predictedZ(float lead);}
 public interface Host {
  double x();double y();double z();double minY();double maxY();float height();int cooldown();void cooldown(int value);
  float nextFloat();double nextGaussian();void melee(Target target);void charged(boolean value);void effect(int id,int x,int y,int z);void fireball(double dx,double dy,double dz,double spawnY);void yaw(float yaw);void attacked();
 }
 int burst;
 public void attack(Host host,Target target,float distance){
  if(host.cooldown()<=0&&distance<2f&&target.maxY()>host.minY()&&target.minY()<host.maxY()){
   host.cooldown(20);host.melee(target);
  }else if(distance<30f){
   double dx=target.x()-host.x(),dy=target.minY()+(double)(target.height()/2f)-(host.y()+(double)(host.height()/2f)),dz=target.z()-host.z();
   float lead=(float)Math.pow(dx*dx+dy*dy+dz*dz,.42)*2f;lead*=Math.min(.5f+host.nextFloat(),1f);
   dx=target.predictedX(lead)-host.x();dz=target.predictedZ(lead)-host.z();
   if(host.cooldown()==0){
    ++burst;if(burst==1){host.cooldown(20);host.charged(true);}else if(burst<=4)host.cooldown(6);else{host.cooldown(20);burst=0;host.charged(false);}
    if(burst>1){float spread=(float)Math.floor(distance)*.5f;spread*=.5f;host.effect(1009,(int)host.x(),(int)host.y(),(int)host.z());host.fireball(dx+host.nextGaussian()*spread,dy,dz+host.nextGaussian()*spread,host.y()+(double)(host.height()/2f)+.5);}
   }
   host.yaw((float)(Math.atan2(dz,dx)*180/Math.PI)-90f);host.attacked();
  }
 }
}
