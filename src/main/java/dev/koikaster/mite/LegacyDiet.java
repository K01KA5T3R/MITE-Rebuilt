package dev.koikaster.mite;

/** Scalar nutrient and insulin arithmetic adapted from the supplied R195 jv/yc/enum classes. */
public final class LegacyDiet {
 public static final int NUTRIENT_LIMIT=160000,INSULIN_LIMIT=192000;
 public int protein=NUTRIENT_LIMIT,fats=NUTRIENT_LIMIT,phytonutrients=NUTRIENT_LIMIT,insulin,resistance;
 public void tick(){
  if(protein>0)protein--;if(fats>0)fats--;if(phytonutrients>0)phytonutrients--;
  if(insulin>0)setInsulin(insulin-1);
 }
 public boolean malnourished(){return protein==0||phytonutrients==0;}
 public boolean metabolizesSugar(){return resistance<2;}
 public void setInsulin(int value){
  insulin=Math.max(0,Math.min(INSULIN_LIMIT,value));
  if(insulin==0){resistance=0;return;}
  int target=insulin>=144000?3:insulin>=96000?2:insulin>=48000?1:0;
  if(target==0){if(resistance!=0)resistance=1;}
  else if(resistance==0||resistance<target)resistance=target;
  else if(resistance>target)resistance=target+1;
 }
 public int adjustedSatiation(int base,int sugar){return metabolizesSugar()||sugar<=0?base:base-(sugar<1000?1:sugar/1000);}
 public int addFood(int nutrition,int sugar,int nutrientMask){
  int amount=Math.max(0,nutrition)*8000;
  if((nutrientMask&1)!=0)protein=Math.min(NUTRIENT_LIMIT,protein+amount);
  if((nutrientMask&2)!=0)fats=Math.min(NUTRIENT_LIMIT,fats+amount);
  if((nutrientMask&4)!=0)phytonutrients=Math.min(NUTRIENT_LIMIT,phytonutrients+amount);
  int response=(int)(sugar*4.8f);setInsulin(Math.min(INSULIN_LIMIT,insulin+response));return response;
 }
 public boolean needsNutrients(int nutrientMask){return protein==0&&(nutrientMask&1)!=0||phytonutrients==0&&(nutrientMask&4)!=0;}
 public static float hungerMultiplier(boolean rain,boolean thunder,int waterDepth,boolean freezing,float biomeTemperature,boolean malnourished){
  float rainFactor=rain?(thunder?.5f:.25f):0,immersion=waterDepth>=2?.5f:waterDepth==1?.25f:0;
  float wetness=Math.max(rainFactor,immersion);
  if(rain&&!thunder&&immersion==.25f)wetness+=.125f;
  if(freezing)wetness*=2;else if(biomeTemperature>=1.4f)wetness=0;
  return 1+wetness+(malnourished?.5f:0);
 }
}
