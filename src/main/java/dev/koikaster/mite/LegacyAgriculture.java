package dev.koikaster.mite;

/** Arithmetic transcribed from R195 BlockGrowingPlant/anr/aof, independent of modern world APIs. */
public final class LegacyAgriculture {
 public static boolean cropAt(int mask,int x,int z){return (mask&(1<<((x+2)*5+z+2)))!=0;}
 public static int cropBit(int x,int z){return 1<<((x+2)*5+z+2);}
 public static float temperatureModifier(float temperature){
  float difference;
  if(temperature<.8f)difference=.8f-temperature;
  else if(temperature>1.2f)difference=temperature-1.2f;
  else return 1f;
  return Math.max(1f-difference,0f);
 }
 public static float proximityModifier(int crops){
  boolean n=cropAt(crops,0,-1),e=cropAt(crops,1,0),s=cropAt(crops,0,1),w=cropAt(crops,-1,0);
  int neighbors=(n?1:0)+(e?1:0)+(s?1:0)+(w?1:0);
  if(neighbors>1)return 1f;if(neighbors==0)return .5f;
  if(n&&(cropAt(crops,0,-2)||cropAt(crops,1,-1)||cropAt(crops,-1,-1)))return 1f;
  if(e&&(cropAt(crops,1,-1)||cropAt(crops,2,0)||cropAt(crops,1,1)))return 1f;
  if(s&&(cropAt(crops,1,1)||cropAt(crops,0,2)||cropAt(crops,-1,1)))return 1f;
  if(w&&(cropAt(crops,-1,-1)||cropAt(crops,-1,1)||cropAt(crops,-2,0)))return 1f;
  return .75f;
 }
 /** Soil is x-major3x3 metadata: -1 means non-farmland, bits0..2 moisture, bit3 fertility. */
 public static float growthRate(float temperature,int[] soil,int crops){
  if(soil[4]>=0&&(soil[4]&7)==0)return 0f;
  float rate=1f;
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
   int metadata=soil[(x+1)*3+z+1];float credit=metadata<0?0f:metadata>0?3f:1f;
   if(x!=0||z!=0)credit/=4f;rate+=credit;
  }
  boolean alongX=cropAt(crops,-1,0)||cropAt(crops,1,0),alongZ=cropAt(crops,0,-1)||cropAt(crops,0,1);
  boolean diagonal=cropAt(crops,-1,-1)||cropAt(crops,1,-1)||cropAt(crops,1,1)||cropAt(crops,-1,1);
  if(diagonal||alongX&&alongZ)rate/=2f;
  if(soil[4]>=0&&(soil[4]&8)!=0)rate*=1.5f;
  rate*=temperatureModifier(temperature);
  // R195 ordinary crops return1 for both humidity states.
  rate*=1f;rate*=.25f;return rate*proximityModifier(crops);
 }
 public static float dryingChance(float temperature,boolean highHumidity){return Math.max(temperature-.15f-(highHumidity?.5f:0f),0f);}
 public static boolean growthLight(int brightness){return brightness==15;}
 public static int growthDenominator(float rate){return rate>0f?(int)(25f/rate)+1:0;}
 private LegacyAgriculture(){}
}
