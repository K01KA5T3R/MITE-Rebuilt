package dev.koikaster.mite;
/** R195 bdd's32-block wet-biome falloff and bfe's sound factors only. */
public final class SourceRainSoundRules {
 public interface Columns {boolean loaded(int x,int z);boolean wet(int x,int z);}
 public record Sound(float volume,float pitch){}
 public static float strength(float rain,double viewerX,double viewerZ,boolean blood,Columns columns){
  if((double)rain==0.0)return 0;int x=(int)Math.floor(viewerX),z=(int)Math.floor(viewerZ);
  if(columns.wet(x,z)||blood)return rain;
  double nearest=Double.MAX_VALUE;int falloff=32;
  for(int dx=-falloff;dx<=falloff;++dx)for(int dz=-falloff;dz<=falloff;++dz){
   if(!columns.loaded(x+dx,z+dz))continue;if(!columns.wet(x+dx,z+dz))continue;
   double a=(double)((float)(x+dx)+.5f)-viewerX,b=(double)((float)(z+dz)+.5f)-viewerZ,distance=a*a+b*b;if(distance<nearest)nearest=distance;
  }
  if((nearest-=.5)<=0)return rain;
  return nearest>=(double)(falloff*falloff)?0:Math.max(rain*(1-(float)Math.sqrt(nearest)/(float)falloff),0);
 }
 public static Sound sound(float volume,float pitch,boolean outdoors,float strength,float sleep){
  if(!outdoors){volume*=.25f;pitch*=.25f;}
  return new Sound(volume*sleep*(float)Math.pow(strength,4.0),pitch);
 }
 private SourceRainSoundRules(){}
}
