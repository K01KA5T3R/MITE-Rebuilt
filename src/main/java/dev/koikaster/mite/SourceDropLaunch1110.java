package dev.koikaster.mite;
import java.util.function.DoubleSupplier;
/** ss(abw,DDD): Math.random is independent of the world's java.util.Random stream. */
public final class SourceDropLaunch1110 {
 public record State(float bob,float yaw,double vx,double vy,double vz){}
 public static State sample(boolean server,DoubleSupplier math){
  float bob=(float)(math.getAsDouble()*Math.PI*2.0);
  if(!server)return new State(bob,0,0,0,0);
  float yaw=(float)(math.getAsDouble()*360.0);
  double x=(float)(math.getAsDouble()*(double).2f-(double).1f),y=.2f,z=(float)(math.getAsDouble()*(double).2f-(double).1f);
  return new State(bob,yaw,x,y,z);
 }
 private SourceDropLaunch1110(){}
}
