package dev.koikaster.mite;
/** Tick arithmetic adapted from the supplied R195 FoodStats (ux.onUpdate).
 * Action exhaustion, nutrient categories and enchantment inputs are supplied by the modern adapter.
 */
public final class LegacyFood {
 public int satiation,nutrition;
 public float hunger,nutritionHunger,healProgress,starveProgress;
 public record Context(float health,boolean needsHealing,boolean naturalRegeneration,boolean malnourished,boolean sleeping,int difficulty,float hungerFactor,float globalRate,float regenerationModifier){}
 public record Result(boolean heal,boolean starveDamage){}
 public void addHunger(float amount,float globalRate){hunger=Math.min(hunger+amount*globalRate,40f);}
 public Result tick(Context input){
  addHunger(.002f*input.hungerFactor(),input.globalRate());nutritionHunger+=.002f*.25f;
  if(hunger>=4f){
   hunger-=4f;
   if(satiation>0||nutrition>0){
    if(satiation<1||nutritionHunger+.001f>=4f&&nutrition>0){nutrition--;nutritionHunger=0;}
    else satiation--;
   }
  }
  boolean heal=false,damage=false;
  if(nutrition==0){
   healProgress=0;starveProgress+=.002f;
   if(starveProgress>=1){damage=input.health()>10||input.difficulty()>=3||input.health()>1&&input.difficulty()>=2;starveProgress-=1;nutritionHunger=0;}
  }else{
   healProgress+=(.0004f+nutrition*.00002f)*(input.malnourished()?.25f:1f)*(input.sleeping()?4f:1f)*input.regenerationModifier();starveProgress=0;
   if(input.naturalRegeneration()&&input.needsHealing()){
    if(healProgress>=1){heal=true;addHunger(1,input.globalRate());healProgress-=1;}
   }else healProgress=0;
  }
  return new Result(heal,damage);
 }
 public static boolean canIngest(int satiation,int nutrition,int cap,int foodSatiation,int foodNutrition){
  if(satiation<cap){if(foodSatiation>0)return true;if(nutrition>=cap)return false;}
  else if(foodSatiation>0&&nutrition>0)return false;
  return foodNutrition>0&&nutrition<cap;
 }
}
