package dev.koikaster.mite;
import java.util.Random;
/** ItemManure.particleEffect and xl.a; their action dispatch remains a separate owner. */
public final class SourceFertilizerRules925 {
 public interface Fx {double boundsMaxY();void particle(double x,double y,double z,double vx,double vy,double vz);}
 public static void particles(boolean manure,int x,int y,int z,int count,Random random,Fx host){
  if(count==0)count=manure?4:15;
  double height=host.boundsMaxY();
  for(int i=0;i<count;i++){
   double vx=random.nextGaussian()*.02,vy=random.nextGaussian()*.02,vz=random.nextGaussian()*.02;
   double px=(double)((float)x+random.nextFloat());
   double py=manure?(double)y+height+(double).15f+(double)(random.nextFloat()*.1f):(double)y+(double)random.nextFloat()*height;
   double pz=(double)((float)z+random.nextFloat());
   host.particle(px,py,pz,vx,vy,vz);
  }
 }
 private SourceFertilizerRules925(){}
}
