package dev.koikaster.mite.visual;

/** The 1.6.4/R195 sky-to-air-fog blend, expressed in modern chunk distances. */
public final class ClassicAirFog {
 public static float distanceIndex(float chunks){
  return Math.max(0,Math.min(3,(float)(Math.log(16/Math.max(1,chunks))/Math.log(2))));
 }
 public static float[] color(float[] base,float[] sky,float chunks,float[] sunrise,float facing,float rain,float thunder){
  float index=distanceIndex(chunks),blend=1-(float)Math.pow(1/(4-index),.25);
  float r=base[0],g=base[1],b=base[2];
  if(index<2&&facing>0&&sunrise!=null){
   float amount=facing*sunrise[3];
   r=r*(1-amount)+sunrise[0]*amount;
   g=g*(1-amount)+sunrise[1]*amount;
   b=b*(1-amount)+sunrise[2]*amount;
  }
  r+=(sky[0]-r)*blend;g+=(sky[1]-g)*blend;b+=(sky[2]-b)*blend;
  if(rain>0){r*=1-rain*.5f;g*=1-rain*.5f;b*=1-rain*.4f;}
  if(thunder>0){float amount=1-thunder*.5f;r*=amount;g*=amount;b*=amount;}
  return new float[]{r,g,b};
 }
 private ClassicAirFog(){}
}
