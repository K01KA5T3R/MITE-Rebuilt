package dev.koikaster.mite;
/** Verified R195 growth/XP/level bonuses alongside explicitly designed Reforged rules. */
public final class Rules {
 private Rules() {}
 public static final int MIN_LEVEL=-40;
 public static final int MAX_LEVEL=200;
 public static final int MAX_XP=Integer.MAX_VALUE-1; // Avoid the original overflow/sentinel loop at Integer.MAX_VALUE.
 /** R195 EntityPlayer table starts at 30 XP, then adds 40, 50, ... per level. */
 public static int xpForLevel(int level) {
  if(level>MAX_LEVEL)return Integer.MAX_VALUE;
  if(level<0)return Math.max(MIN_LEVEL,level)*20;
  long l=level;return (int)(5L*l*l+25L*l);
 }
 public static int level(int xp) {
  if(xp<0)return Math.max(MIN_LEVEL,Math.floorDiv(xp,20));
  int lo=0,hi=MAX_LEVEL;
  while(lo<hi) {int m=(lo+hi+1)>>>1;if(xpForLevel(m)<=Math.max(0,xp))lo=m;else hi=m-1;}
  return lo;
 }
 public static int foodCap(int level) {return Math.min(20,6+Math.max(0,level)/5*2);}
 public static double healthCap(int level,int initial) {return Math.min(20,Math.max(2,initial)+Math.max(0,Math.min(MAX_LEVEL,level))/5*2);}
 public static float meleeMultiplier(int level){
  if(level<0)return 1f+Math.max(MIN_LEVEL,level)*.02f;
  return 1f+Math.min(MAX_LEVEL,level)*.005f;
 }
 public static float harvestingMultiplier(int level){
  return 1f+Math.max(MIN_LEVEL,Math.min(MAX_LEVEL,level))*.02f;
 }
 public static int deathXp(int xp) {
  int current=level(xp);
  if(current==MIN_LEVEL)return xpForLevel(MIN_LEVEL);
  int start=xpForLevel(current),previous=xpForLevel(current-1);
  if(current==MAX_LEVEL)return previous;
  int span=xpForLevel(current+1)-start;
  return previous+(int)((long)(xp-start)*(start-previous)/span);
 }
 public static int classicDeathXp(int xp) {
  return xp >= xpForLevel(1) ? 0 : (int)Math.max(xpForLevel(MIN_LEVEL), (long)xp-xpForLevel(1));
 }
 public static float levelProgress(int xp,int level) {
  if(level>=MAX_LEVEL)return 0;
  int start=xpForLevel(level);
  return Math.max(0,(xp-start)/(float)(xpForLevel(level+1)-start));
 }
 public static int craftingTicks(int cells,int tier,double multiplier) {
  return (int)Math.max(1,Math.min(72000,(20+cells*12L+Math.max(0,tier)*100L)*multiplier));
 }
 public enum Moon {NORMAL,HARVEST,BLOOD,BLUE,MOON_DOG}
 /** R195 abw.is*Moon: input is the zero-based unadjusted world day (dayTime / 24000). */
 public static Moon moon(long worldDay) {
  long day=worldDay+1;
  if(day%128==0)return Moon.BLUE;
  if(day%32==0)return Moon.BLOOD;
  long next=day+8;
  if(next%32==0&&next%128!=0)return Moon.HARVEST;
  if(next%128==0)return Moon.MOON_DOG;
  return Moon.NORMAL;
 }
 public static int furnaceHeat(int capacity,int fuelHeat) {return Math.min(capacity,fuelHeat);}
 public static boolean canSmelt(int capacity,int fuelHeat,int required) {return required>0&&furnaceHeat(capacity,fuelHeat)>=required;}
 public static int addXp(int xp,int amount) {return (int)Math.max(xpForLevel(MIN_LEVEL),Math.min(MAX_XP,(long)xp+amount));}
 public static double armorFraction(int damage,int maxDamage){return maxDamage<=0?1:Math.max(.5,Math.min(1,2-2*damage/(double)maxDamage));}
 public static float fishingBonusChance(boolean rain,boolean night){return .05f+(rain?.15f:0)+(night?.05f:0);}
 public static int recoveredXp(int damage,int xp,double efficiency) {return Math.min(Math.max(0,damage),(int)Math.floor(Math.max(0,xp)*2d*Math.max(0,efficiency)));}
}
