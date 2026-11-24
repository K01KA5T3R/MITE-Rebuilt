package dev.koikaster.mite;
import java.util.function.DoubleSupplier;
/** Selected beg constructors followed by bdo constructors; no modern item-particle random draws. */
public final class SourceIconCrackRules996 {
 public record Initial(double vx,double vy,double vz,float size,int lifetime,float jitterU,float jitterV,float gravity){}
 public static Initial create(float[] table,int initialIndex,DoubleSupplier math,double vx,double vy,double vz){
  int index=initialIndex;float u=table[++index&32767]*3f,v=table[++index&32767]*3f;
  float size=table[++index&32767]+1f;int life=(int)(4f/(table[++index&32767]*.9f+.1f));
  double x=0+(double)(table[++index&32767]*.8f)-(double).4f,y=0+(double)(table[++index&32767]*.8f)-(double).4f,z=0+(double)(table[++index&32767]*.8f)-(double).4f;
  float scale=(table[++index&32767]*(float)math.getAsDouble()+1f)*.15f;float length=(float)Math.sqrt(x*x+y*y+z*z);
  x=x/(double)length*(double)scale*(double).4f;y=y/(double)length*(double)scale*(double).4f+(double).1f;z=z/(double)length*(double)scale*(double).4f;
  size/=2f;x*=(double).1f;y*=(double).1f;z*=(double).1f;x+=vx;y+=vy;z+=vz;
  return new Initial(x,y,z,.1f*size,life,u,v,1f);
 }
 public interface Tick {double vx();double vy();double vz();int age();int lifetime();float gravity();void previousPosition();void age(int value);void remove();void move(double x,double y,double z);boolean onGround();void velocity(double x,double y,double z);}
 public static void tick(Tick h){
  h.previousPosition();int age=h.age();h.age(age+1);if(age>=h.lifetime())h.remove();
  double x=h.vx(),y=h.vy()-.04*(double)h.gravity(),z=h.vz();h.velocity(x,y,z);h.move(x,y,z);
  // The native/source movement transport may zero motion against a colliding face.
  x=h.vx()*(double).98f;y=h.vy()*(double).98f;z=h.vz()*(double).98f;
  if(h.onGround()){x*=(double).7f;z*=(double).7f;}h.velocity(x,y,z);
 }
 public static float iconStart(float jitter){return jitter/4f*16f;}
 public static float iconEnd(float jitter){return (jitter+1f)/4f*16f;}
 private SourceIconCrackRules996(){}
}
