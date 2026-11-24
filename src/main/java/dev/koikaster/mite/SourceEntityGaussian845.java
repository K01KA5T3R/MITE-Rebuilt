package dev.koikaster.mite;

/** java.util.Random's original cached polar Gaussian, fed by the entity's uniform stream. */
public final class SourceEntityGaussian845 {
 @FunctionalInterface public interface Uniform {double nextDouble();}
 private boolean cached;private double next;
 public double next(Uniform random){
  if(cached){cached=false;return next;}
  double x,y,s;do{x=2*random.nextDouble()-1;y=2*random.nextDouble()-1;s=x*x+y*y;}while(s>=1||s==0);
  double scale=StrictMath.sqrt(-2*StrictMath.log(s)/s);next=y*scale;cached=true;return x*scale;
 }
}
