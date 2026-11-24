package dev.koikaster.mite;
/** Direct R195 xj/yn/ya/yb/zl and named tool-method port; quality intentionally disabled by38D. */
public final class LegacyToolWear {
 public enum Kind {PICKAXE,AXE,HATCHET,SHOVEL,HOE,SWORD,DAGGER,KNIFE,MATTOCK,WARHAMMER,BATTLEAXE,SCYTHE,SHEARS,CUDGEL,CLUB}
 public static int components(Kind kind){return switch(kind){case PICKAXE,AXE->3;case HATCHET,SHOVEL,DAGGER,KNIFE,CUDGEL->1;case HOE,SWORD,SCYTHE,SHEARS,CLUB->2;case MATTOCK,BATTLEAXE->4;case WARHAMMER->5;};}
 public static float durability(String material){return durability(material,false);}
 static float durability(String material,boolean r196){return switch(material){case "wood"->.5f;case "flint"->1;case "obsidian"->2;case "rusted_iron","copper","silver"->4;case "gold"->r196?R196Gold.DURABILITY:3;case "iron"->8;case "ancient_metal","diamond"->16;case "mithril"->64;case "adamantium"->256;case "vibranium"->512;case "holy_steel"->1024;
  // These materials have no corresponding R195 tool: explicit modern/selected expansion mappings.
  case "stone"->1;case "bronze"->6;case "steel"->16;case "netherite"->128;default->0;};}
 public static int maxDamage(Kind kind,String material){return maxDamage(kind,material,false);}
 static int maxDamage(Kind kind,String material,boolean r196){return (int)(4f*(float)components(kind)*durability(material,r196)*100f);}
 public static float blockDecay(Kind kind,boolean sandstone,boolean wheatOrGrass,boolean clothPlantVine){
  float axe=sandstone?1.875f:1f;
  return switch(kind){case PICKAXE,SHEARS->1f;case AXE->axe;case HATCHET->axe*4f/3f;case SHOVEL->.5f;case HOE,SWORD,DAGGER->2f;case KNIFE->clothPlantVine?2f/4f:2f/2f;case MATTOCK->.5f*.8f;case WARHAMMER->1f*2f/3f;case BATTLEAXE->axe*1.25f;case SCYTHE->wheatOrGrass?.5f:2f;case CUDGEL,CLUB->.25f;};
 }
 public static int blockDamage(float hardness,float decayRate){if(hardness<=0)return 0;float decay=100f*decayRate;return Math.max(Math.max((int)(hardness*decay),(int)(decay/20f)),1);}
 public static int attackDamage(Kind kind,boolean vampiric,boolean sharpness){float decay=switch(kind){case PICKAXE,AXE,SHOVEL,MATTOCK->1f;case HATCHET->1f*4f/3f;case HOE,SHEARS->2f;case SWORD,DAGGER,KNIFE->.5f;case WARHAMMER->1f*2f/3f;case BATTLEAXE->1f*.75f;case SCYTHE->vampiric?1f:sharpness?2f:4f;case CUDGEL,CLUB->.25f;};return Math.max((int)(100f*decay),1);}
 public static boolean ignoreDamage(int level,float draw){return draw<((float)level/5f)*.75f;}
 private LegacyToolWear(){}
}
