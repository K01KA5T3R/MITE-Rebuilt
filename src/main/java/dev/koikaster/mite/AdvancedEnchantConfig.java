package dev.koikaster.mite;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Server file only. All GUI values come from the server's menu snapshot. */
public final class AdvancedEnchantConfig {
 public enum MinimumMaterial {MITHRIL,ADAMANTIUM,HOLY_STEEL}
 private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
 private static final ForgeConfigSpec.IntValue MAX=B.defineInRange("maximumExperienceTier",50,1,50),SHELVES=B.defineInRange("maximumBookshelves",15,0,15),BASE=B.defineInRange("baseLevelCost",8,1,50),PER_RANK=B.defineInRange("perEffectRankCost",6,1,50),OTHER=B.defineInRange("perOtherEnchantmentCost",2,0,50),RARE=B.defineInRange("uncommonExtraCost",4,0,50),VERY_RARE=B.defineInRange("rareExtraCost",8,0,50),HIGHEST=B.defineInRange("veryRareExtraCost",10,0,50),MAX_RANK=B.defineInRange("maximumRankExtraCost",6,0,50),PREMIUM=B.defineInRange("minimumChoicePremiumPercent",10,1,1000),CRAFT=B.defineInRange("minimumCraftingTicks",600,25,72000);
 private static final ForgeConfigSpec.EnumValue<MinimumMaterial> MATERIAL=B.defineEnum("minimumEquipmentMaterial",MinimumMaterial.MITHRIL);
 private static final ForgeConfigSpec.BooleanValue UPGRADE=B.define("allowExistingEnchantmentUpgrade",true);
 public static final ForgeConfigSpec SPEC=B.build();
 public record Policy(int maximumTier,int maximumShelves,int base,int perRank,int perOther,int uncommon,int rare,int veryRare,int maximumRank,int premium,int minimumCraftTicks,MinimumMaterial minimum,boolean upgrades){}
 public static Policy policy(){return new Policy(MAX.get(),SHELVES.get(),BASE.get(),PER_RANK.get(),OTHER.get(),RARE.get(),VERY_RARE.get(),HIGHEST.get(),MAX_RANK.get(),PREMIUM.get(),CRAFT.get(),MATERIAL.get(),UPGRADE.get());}
 public static void register(){ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,SPEC,"mite-advanced-enchanting-server.toml");}
 private AdvancedEnchantConfig(){}
}
