package dev.koikaster.mite;

/** Extracted uf block-strength arithmetic; returns a fraction of a block per tick. */
public final class LegacyMining {
 public static float damage(float hardness,float base,boolean portable,int efficiency,int level,int haste,int fatigue,boolean water,boolean aquaAffinity,boolean grounded,boolean foodEnergy){
  if(hardness<0)return 0;if(hardness==0)return 1;
  float minimum=-Float.MAX_VALUE;float strength;
  if(portable)strength=minimum=4f*hardness;else{strength=base;if(efficiency>0&&base>=1)strength+=efficiency*efficiency+1;}
  if(haste>=0)strength*=1f+(haste+1)*.2f;
  if(fatigue>=0)strength*=1f-(fatigue+1)*.2f;
  if(water&&!aquaAffinity)strength/=5f;
  if(!grounded)strength/=5f;
  if(!foodEnergy)strength/=5f;
  strength=Math.max(strength*(1f+level*.02f),minimum);
  if(strength<=0)return 0;
  float relative=hardness/strength;
  return relative<.0029296875f?1f:(1f/relative)/512f;
 }
 public static float materialEfficiency(String material){return switch(material){case "wood"->1f;case "flint","rusted_iron"->1.25f;case "stone","obsidian"->1.5f;case "copper","silver","gold"->1.75f;case "iron","ancient_metal"->2f;case "mithril","diamond"->2.5f;case "netherite"->2.75f;case "adamantium"->3f;case "vibranium","holy_steel"->3.5f;case "bronze"->1.875f;case "steel"->2.25f;default->1f;};}
 private LegacyMining(){}
}
