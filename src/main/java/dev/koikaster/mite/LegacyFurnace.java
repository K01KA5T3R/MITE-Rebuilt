package dev.koikaster.mite;
/** Direct scalar/state branch port of asg/aab and SlotFuel, awaiting modern block/container adapters. */
public final class LegacyFurnace {
 public static final int COOK_TICKS=200;
 public enum SandResult {NONE,SANDSTONE,GLASS}
 public record State(int burn,int heat,int cook,boolean consumedFuel,boolean completed,boolean extinguished){}
 public static boolean fuelFits(int fuelHeat,int maxHeat,boolean large,boolean acceptsLarge){return fuelHeat>0&&fuelHeat<=maxHeat&&(!large||acceptsLarge);}
 public static SandResult sand(int count,int heat){return count<4||heat<1?SandResult.NONE:heat==1?SandResult.SANDSTONE:SandResult.GLASS;}
 public static int consumed(boolean sand,int outputIsGlassOrSandstone){return sand&&outputIsGlassOrSandstone!=0?4:1;}
 public static int extraClay(int outputLimit,int currentOutput,int remainingInput){return Math.min(3,Math.min(outputLimit-currentOutput,remainingInput));}
 public static State tick(boolean server,int oldBurn,int oldHeat,int oldCook,boolean flooded,boolean smothered,boolean canSmeltWithFuel,int fuelDuration,int fuelHeat,boolean canSmeltAfterFuelUpdate){return tick(server,oldBurn,oldHeat,oldCook,flooded,smothered,canSmeltWithFuel,fuelDuration,fuelHeat,canSmeltAfterFuelUpdate,COOK_TICKS);}
 public static State tick(boolean server,int oldBurn,int oldHeat,int oldCook,boolean flooded,boolean smothered,boolean canSmeltWithFuel,int fuelDuration,int fuelHeat,boolean canSmeltAfterFuelUpdate,int cookTarget){
  if(server&&oldBurn!=1&&(flooded||smothered))return new State(0,oldHeat,0,false,false,true);
  int burn=oldBurn,heat=oldHeat,cook=oldCook;boolean consumed=false,completed=false;
  if(burn>0)burn--;else heat=0;
  if(server){if(burn==0&&canSmeltWithFuel){burn=fuelDuration;if(burn>0){heat=fuelHeat;consumed=true;}}
   if(burn>0&&canSmeltAfterFuelUpdate){cook++;if(cook>=Math.max(1,cookTarget)){cook=0;completed=true;}}else cook=0;
  }
  return new State(burn,heat,cook,consumed,completed,false);
 }
 private LegacyFurnace(){}
}
