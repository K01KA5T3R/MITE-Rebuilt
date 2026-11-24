package dev.koikaster.mite.visual;

/** R195 avj.a(float,int,int): one original smoothing step per rendered frame. */
public final class ClassicVignetteCurve {
 public static float step(float current,float brightness){
  float target=Math.max(0,Math.min(1,1-brightness));
  return (float)((double)current+(double)(target-current)*.01);
 }
 private ClassicVignetteCurve(){}
}
