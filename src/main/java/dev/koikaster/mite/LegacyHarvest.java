package dev.koikaster.mite;

/** R195 xj tool/material effectiveness and uf's portable/empty-hand fallback. */
public final class LegacyHarvest {
 public enum Tool {HAND,PICKAXE,AXE,SHOVEL,HOE,SWORD,MATTOCK,OTHER}
 public enum Family {FREE,LOG,PICKAXE,AXE_OR_PICKAXE,SANDSTONE_SLAB}
 public static int material(String material){return switch(material){
  case "wood"->0;case "flint","stone"->1;case "obsidian","rusted_iron","copper","silver","gold","bronze"->2;
  case "iron","ancient_metal","diamond","steel"->3;case "mithril","netherite"->4;case "adamantium"->5;case "vibranium","holy_steel"->6;default->0;
 };}
 public static boolean allows(Tool tool,int level,Family family,int required,boolean portable){
  if(portable||required==0)return true;
  // xj explicitly exempts an axe used on a sandstone slab from the normal level test.
  if(family==Family.SANDSTONE_SLAB&&tool==Tool.AXE)return true;
  if(level<required)return false;
  return switch(family){case LOG->tool==Tool.AXE;case PICKAXE,SANDSTONE_SLAB->tool==Tool.PICKAXE;case AXE_OR_PICKAXE->tool==Tool.PICKAXE||tool==Tool.AXE;case FREE->true;};
 }
 private LegacyHarvest(){}
}
