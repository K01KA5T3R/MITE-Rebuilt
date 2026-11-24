package dev.koikaster.mite;
/** Original Skill constructor order and uf alternative all-bits requirements. */
public final class SourceSkillRules1685 {
 public static final int FIGHTING=1,ARCHERY=2,MINING=4,FOOD_PREPARATION=8,MASONRY=16,FARMING=32,TINKERING=64,CARPENTRY=128,BLACKSMITHING=256,BREWING=512,FINE_ARTS=1024,ENCHANTING=2048,FISHING=4096;
 public static boolean all(int owned,int required){return (owned&required)==required;}
 public static boolean any(int owned,int required){return (owned&required)!=0;}
 public static boolean alternatives(int owned,int[] sets){if(sets==null)return true;for(int set:sets)if(all(owned,set))return true;return false;}
 public static int count(int owned){return Integer.bitCount(owned&8191);}
 private SourceSkillRules1685(){}
}
