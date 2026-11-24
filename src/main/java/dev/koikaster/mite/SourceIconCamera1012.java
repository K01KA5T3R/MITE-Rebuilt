package dev.koikaster.mite;
/** atp.a coefficients and beh.a interpolation; native view-matrix anchoring is a separate transport. */
public final class SourceIconCamera1012 {
 public record Coefficients(float rotationX,float rotationXZ,float rotationZ,float rotationYZ,float rotationXY){}
 public static Coefficients coefficients(float pitch,float yaw,boolean front){
  int view=front?1:0;
  float d=LegacyLighting.cos(yaw*(float)Math.PI/180f)*(float)(1-view*2);
  float f=LegacyLighting.sin(yaw*(float)Math.PI/180f)*(float)(1-view*2);
  float g=-f*LegacyLighting.sin(pitch*(float)Math.PI/180f)*(float)(1-view*2);
  float h=d*LegacyLighting.sin(pitch*(float)Math.PI/180f)*(float)(1-view*2);
  float e=LegacyLighting.cos(pitch*(float)Math.PI/180f);
  return new Coefficients(d,e,f,g,h);
 }
 public record Origin(double x,double y,double z){}
 public static Origin origin(double oldX,double oldY,double oldZ,double x,double y,double z,float partial){return new Origin(oldX+(x-oldX)*(double)partial,oldY+(y-oldY)*(double)partial,oldZ+(z-oldZ)*(double)partial);}
 /** bfl admission after its camera/particle-engine guard; world RNG is drawn before distance. */
 public static boolean admitted(int setting,double cameraX,double cameraY,double cameraZ,double x,double y,double z,java.util.function.IntUnaryOperator worldRandom){
  if(setting==1&&worldRandom.applyAsInt(3)==0)setting=2;
  double dx=cameraX-x,dy=cameraY-y,dz=cameraZ-z;if(dx*dx+dy*dy+dz*dz>24.0*24.0)return false;
  return setting<=1;
 }
 private SourceIconCamera1012(){}
}
