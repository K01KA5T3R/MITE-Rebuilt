package dev.koikaster.mite;
import java.util.Random;import java.util.function.DoubleSupplier;
/** bcw.renderBrokenItem's five source vectors; sound follows all particle callbacks in handleEntityFX. */
public final class SourceBrokenItemRules996 {
 public interface Host {boolean clientWorld();double x();double legacyY();double z();float pitch();float yaw();float legacyEyeOffset();void particle(double x,double y,double z,double vx,double vy,double vz);}
 public static void render(Host h,Random renderer,DoubleSupplier math){
  if(!h.clientWorld())return;
  for(int i=0;i<5;i++){
   double vx=((double)renderer.nextFloat()-.5)*.1,vy=math.getAsDouble()*.1+.1,vz=0;
   float angle=-h.pitch()*(float)Math.PI/180f;float c=LegacyLighting.cos(angle),s=LegacyLighting.sin(angle);
   double y=vy*(double)c+vz*(double)s,z=vz*(double)c-vy*(double)s;vy=y;vz=z;
   angle=-h.yaw()*(float)Math.PI/180f;c=LegacyLighting.cos(angle);s=LegacyLighting.sin(angle);
   double x=vx*(double)c+vz*(double)s;z=vz*(double)c-vx*(double)s;vx=x;vz=z;
   double px=((double)renderer.nextFloat()-.5)*.3,py=(double)(-renderer.nextFloat())*.6-.3,pz=.6;
   angle=-h.pitch()*(float)Math.PI/180f;c=LegacyLighting.cos(angle);s=LegacyLighting.sin(angle);
   y=py*(double)c+pz*(double)s;z=pz*(double)c-py*(double)s;py=y;pz=z;
   angle=-h.yaw()*(float)Math.PI/180f;c=LegacyLighting.cos(angle);s=LegacyLighting.sin(angle);
   x=px*(double)c+pz*(double)s;z=pz*(double)c-px*(double)s;px=x;pz=z;
   h.particle(px+h.x(),py+(h.legacyY()+(double)h.legacyEyeOffset()),pz+h.z(),vx,vy+.05,vz);
  }
 }
 public static float soundPitch(float worldRandom){return .8f+worldRandom*.4f;}
 public static double soundY(double footY,float sourceHeight){return footY+(double)(sourceHeight/2f);}
 private SourceBrokenItemRules996(){}
}
