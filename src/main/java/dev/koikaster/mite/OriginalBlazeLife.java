package dev.koikaster.mite;
/** Staged td.c effects before its actual parent AI. Not bound to native Blaze. */
public final class OriginalBlazeLife {
 public interface Host {
  boolean client();boolean wet();boolean blazeOnGround();double x();double y();double z();float width();float height();float eyeHeight();Double targetEyeY();double motionY();void motionY(double value);void waterHurt(float value);
  int nextInt(int bound);float nextFloat();double nextGaussian();double nextDouble();void fireSound(double x,double y,double z,float volume,float pitch);void smoke(double x,double y,double z);
 }
 float offset=.5f;int ticks;
 public void beforeParent(Host host){
  if(!host.client()){
   if(host.wet())host.waterHurt(1f);--ticks;if(ticks<=0){ticks=100;offset=.5f+(float)host.nextGaussian()*3f;}
   var target=host.targetEyeY();if(target!=null&&target>host.y()+(double)host.eyeHeight()+(double)offset)host.motionY(host.motionY()+((double).3f-host.motionY())*(double).3f);
  }
  if(host.nextInt(24)==0)host.fireSound(host.x()+.5,host.y()+.5,host.z()+.5,1f+host.nextFloat(),host.nextFloat()*.7f+.3f);
  if(!host.blazeOnGround()&&host.motionY()<0)host.motionY(host.motionY()*.6);
  for(int i=0;i<2;i++)host.smoke(host.x()+(host.nextDouble()-.5)*(double)host.width(),host.y()+host.nextDouble()*(double)host.height(),host.z()+(host.nextDouble()-.5)*(double)host.width());
 }
}
