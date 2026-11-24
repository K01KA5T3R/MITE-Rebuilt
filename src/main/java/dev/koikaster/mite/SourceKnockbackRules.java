package dev.koikaster.mite;

/** The two distinct of knockback methods. A successful wrapper reports an
 * affected result even when the attribute roll prevents movement. */
public final class SourceKnockbackRules {
 public interface Host {
  boolean canBeKnockedBack();int resistanceTicks();void resistanceTicks(int value);
  double x();double z();float yaw();void attackedYaw(float value);
  double globalRandom();double nextDouble();double resistance();
  boolean player();boolean earthElemental();void impulse();
  double motionX();double motionY();double motionZ();void motion(double x,double y,double z);
  void direct(float amount,double dx,double dz);
 }
 public static boolean knockBack(Host h,boolean attackerPresent,double attackerX,double attackerZ,float amount){
  if(!attackerPresent||amount<=0||!h.canBeKnockedBack()||h.resistanceTicks()>0)return false;
  double dx=attackerX-h.x(),dz=attackerZ-h.z();
  while(dx*dx+dz*dz<1.0E-4){dx=(h.globalRandom()-h.globalRandom())*.01;dz=(h.globalRandom()-h.globalRandom())*.01;}
  h.attackedYaw((float)(Math.atan2(dz,dx)*180/Math.PI)-h.yaw());
  h.direct(amount,dx,dz);
  return true;
 }
 public static void direct(Host h,float amount,double dx,double dz){
  if(h.nextDouble()>=h.resistance()){
   h.impulse();float length=(float)Math.sqrt(dx*dx+dz*dz),strength=.4f;
   double x=h.motionX()/2,y=h.motionY()/2,z=h.motionZ()/2;
   x-=dx/(double)length*(double)strength;y+=(double)strength;z-=dz/(double)length*(double)strength;
   if(y>(double).4f)y=.4f;
   if(!h.player()){x*=(double).8f;y*=(double).8f;z*=(double).8f;}
   if(h.earthElemental()){x*=(double).4f;y*=(double).4f;z*=(double).4f;}
   x*=(double)amount;y*=(double)amount;z*=(double)amount;
   h.motion(x,y,z);h.resistanceTicks(10);
  }
 }
 private SourceKnockbackRules(){}
}
