package dev.koikaster.mite;
/** Staged direct numeric branches from ItemCoin/wr/EnumEquipmentMaterial/amv/va/aaf; not activated yet. */
public final class LegacyProduction {
 public enum Metal {
  COPPER(4),SILVER(4),GOLD(3),IRON(8),ANCIENT_METAL(16),MITHRIL(64),ADAMANTIUM(256);
  public final float durability;Metal(float durability){this.durability=durability;}
 }
 public static int coinValue(Metal material){return switch(material){case ANCIENT_METAL->500;case MITHRIL->2500;case ADAMANTIUM->10000;case COPPER->5;case SILVER->25;case GOLD->100;default->0;};}
 public static float meltingChance(Metal material){return material==Metal.ADAMANTIUM?0.0f:material==Metal.GOLD?0.2f:0.01f*(Metal.MITHRIL.durability/material.durability);}
 public static boolean melts(Metal material,float random){return random<meltingChance(material);}
 public static boolean placeSource(boolean hasPlayer,boolean creative,boolean ctrl,int experience){if(!hasPlayer)return false;if(creative)return true;return ctrl&&experience>=100;}
 public static boolean removeCollectedSource(boolean creative,boolean ctrl){return creative||ctrl;}
 public static int placedSourceExperienceCost(boolean creative,boolean source){return !creative&&source?100:0;}
 public static boolean repairMaterialFits(Metal tool,Metal nugget,Metal anvil){if(tool!=nugget)return false;Metal hardest=tool.durability>nugget.durability?tool:nugget;return hardest.durability<=anvil.durability;}
 public static int toolRepairCost(int durabilityComponents){return durabilityComponents*2;}
 public record Repair(int remainingDamage,int consumed){}
 /** va stops consuming once the remaining partial repair is smaller than the initial repair amount. */
 public static Repair repair(int damage,int maxDamage,int cost,int available,boolean chain){if(damage<=0||maxDamage<=0||cost<=0||available<=0)return new Repair(damage,0);int amount=Math.min(damage,chain?maxDamage*2/cost:maxDamage/cost);if(amount<=0)return new Repair(damage,0);int initial=amount,used;for(used=0;amount>0&&amount==initial&&used<available;used++){damage-=amount;amount=Math.min(damage,chain?maxDamage*2/cost:maxDamage/cost);}return new Repair(damage,used);}
 public static int combinedDamage(int maxDamage,int firstDamage,int secondDamage){int firstRemaining=maxDamage-firstDamage;int secondRemaining=maxDamage-secondDamage;int resultingDurability=firstRemaining+secondRemaining;if(resultingDurability>maxDamage)resultingDurability=maxDamage;return maxDamage-resultingDurability;}
 private LegacyProduction(){}
}
