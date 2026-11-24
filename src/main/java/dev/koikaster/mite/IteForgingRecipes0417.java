package dev.koikaster.mite;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * MITE-ITE 2.0.30 10-grade Forging Table recipe catalogue, enhance stones,
 * hammer/axe bonus fail reduction, quality rewards, and failure feedback (0.4.17).
 */
public final class IteForgingRecipes0417 {
    public enum StoneType {
        IRON("iron_enhance_stone", 35),
        MITHRIL("mithril_enhance_stone", 55),
        ADAMANTIUM("adamantium_enhance_stone", 65),
        UNIVERSAL("universal_enhance_stone", 50);

        public final String itemId;
        public final int failChance;

        StoneType(String itemId, int failChance) {
            this.itemId = itemId;
            this.failChance = failChance;
        }
    }

    public record MaterialReq(String itemPath, int count) {}

    public record ForgingRecipe(
        int targetGrade,
        int minTableTier, // 0=iron, 1=mithril, 2=adamantium, 3=vibranium
        int baseFailChance,
        int axeCost,
        int hammerCost,
        int timeReqTicks,
        int downgradeOnFail,
        int damagePercentOnFail,
        @Nullable String qualityReward,
        List<MaterialReq> materials
    ) {
        public int effectiveFailChance(int hammerHarvestLevel, int axeHarvestLevel) {
            int ironLevel = 3;
            int bonus = Math.max((hammerHarvestLevel - ironLevel) * 3, 0)
                + Math.max((axeHarvestLevel - ironLevel) * 2, 0);
            return Math.max(0, baseFailChance - bonus);
        }
    }

    // Standard progression per ForgingTableRecipes (grades 0->1 .. 9->10)
    public static final List<ForgingRecipe> RECIPES = List.of(
        new ForgingRecipe(0, 0, StoneType.IRON.failChance, 2048, 2048, 400, 0, 5, null,
            List.of(new MaterialReq("minecraft:gold_ingot", 1), new MaterialReq("minecraft:iron_ingot", 1), new MaterialReq("mite_reforged:iron_enhance_stone", 1))),
        new ForgingRecipe(1, 0, StoneType.IRON.failChance, 2048, 3072, 600, 0, 5, "fine",
            List.of(new MaterialReq("minecraft:gold_ingot", 1), new MaterialReq("minecraft:iron_ingot", 2), new MaterialReq("mite_reforged:iron_enhance_stone", 1))),
        new ForgingRecipe(2, 0, StoneType.IRON.failChance, 2048, 3072, 800, 1, 5, "fine",
            List.of(new MaterialReq("minecraft:gold_ingot", 2), new MaterialReq("minecraft:iron_ingot", 2), new MaterialReq("mite_reforged:iron_enhance_stone", 1))),
        new ForgingRecipe(3, 0, StoneType.IRON.failChance, 3072, 4096, 1000, 1, 5, "excellent",
            List.of(new MaterialReq("minecraft:gold_ingot", 2), new MaterialReq("minecraft:iron_ingot", 2), new MaterialReq("mite_reforged:iron_enhance_stone", 2))),
        new ForgingRecipe(4, 1, StoneType.MITHRIL.failChance, 4096, 4096, 1200, 1, 5, "excellent",
            List.of(new MaterialReq("minecraft:gold_ingot", 1), new MaterialReq("mite_reforged:mithril_ingot", 1), new MaterialReq("minecraft:redstone", 1), new MaterialReq("mite_reforged:mithril_enhance_stone", 1))),
        new ForgingRecipe(5, 1, StoneType.MITHRIL.failChance, 4096, 5120, 1400, 1, 5, "superb",
            List.of(new MaterialReq("minecraft:gold_ingot", 2), new MaterialReq("mite_reforged:mithril_ingot", 1), new MaterialReq("minecraft:redstone", 2), new MaterialReq("mite_reforged:mithril_enhance_stone", 1))),
        new ForgingRecipe(6, 1, StoneType.MITHRIL.failChance, 6144, 6144, 1600, 1, 5, "superb",
            List.of(new MaterialReq("minecraft:gold_ingot", 2), new MaterialReq("mite_reforged:mithril_ingot", 2), new MaterialReq("minecraft:redstone_block", 1), new MaterialReq("mite_reforged:mithril_enhance_stone", 1))),
        new ForgingRecipe(7, 3, StoneType.ADAMANTIUM.failChance, 6144, 8192, 1800, 1, 10, "masterwork",
            List.of(new MaterialReq("minecraft:ghast_tear", 1), new MaterialReq("mite_reforged:adamantium_ingot", 1), new MaterialReq("mite_reforged:mithril_ingot", 2), new MaterialReq("mite_reforged:adamantium_enhance_stone", 1))),
        new ForgingRecipe(8, 3, StoneType.ADAMANTIUM.failChance, 8192, 10240, 2000, 1, 10, "masterwork",
            List.of(new MaterialReq("minecraft:ghast_tear", 2), new MaterialReq("mite_reforged:adamantium_ingot", 1), new MaterialReq("mite_reforged:mithril_ingot", 2), new MaterialReq("mite_reforged:adamantium_enhance_stone", 1))),
        new ForgingRecipe(9, 3, StoneType.ADAMANTIUM.failChance, 8192, 10240, 2200, 1, 10, "legendary",
            List.of(new MaterialReq("minecraft:ghast_tear", 2), new MaterialReq("mite_reforged:adamantium_ingot", 2), new MaterialReq("mite_reforged:mithril_ingot", 2), new MaterialReq("mite_reforged:adamantium_enhance_stone", 1)))
    );

    public static int maxGradeForMaterial(String material) {
        return switch (material) {
            case "copper", "silver", "gold", "rusted_iron", "bronze" -> 3;
            case "iron", "ancient_metal", "steel" -> 4;
            case "mithril", "netherite" -> 6;
            case "adamantium" -> 7;
            case "vibranium", "holy_steel" -> 9;
            default -> -1;
        };
    }

    @Nullable
    public static ForgingRecipe recipeFor(ItemStack target, int tableTier) {
        if (target.isEmpty()) return null;
        String material = materialOf(target);
        int currentGrade = IteModifierTypes0417.forgingGrade(target);
        if (currentGrade > maxGradeForMaterial(material) || currentGrade < 0 || currentGrade >= RECIPES.size()) return null;
        ForgingRecipe r = RECIPES.get(currentGrade);
        return tableTier >= r.minTableTier ? r : null;
    }

    public static String materialOf(ItemStack stack) {
        var tool = ClassicToolWear.profile(stack);
        if (tool != null) return tool.material();
        var armor = OriginalArmorItems.profile(stack);
        if (armor != null) return armor.material();
        return ClassicToolCatalogue.material(stack);
    }

    public record ForgeOutcome(boolean succeeded, int newGrade, int roll, int failThreshold) {}

    public static ForgeOutcome executeForge(ItemStack target, ForgingRecipe recipe, int hammerLevel, int axeLevel, RandomSource rand) {
        int threshold = recipe.effectiveFailChance(hammerLevel, axeLevel);
        int roll = rand.nextInt(100);
        var tag = target.getOrCreateTag();
        int prevGrade = IteModifierTypes0417.forgingGrade(target);
        if (roll < threshold) {
            int nextGrade = Math.max(0, prevGrade - recipe.downgradeOnFail);
            tag.putInt("forging_grade", nextGrade);
            if (target.isDamageableItem() && recipe.damagePercentOnFail > 0) {
                int addDmg = Math.max(1, target.getMaxDamage() * recipe.damagePercentOnFail / 100);
                target.setDamageValue(Math.min(target.getMaxDamage() - 1, target.getDamageValue() + addDmg));
            }
            return new ForgeOutcome(false, nextGrade, roll, threshold);
        }
        int nextGrade = prevGrade + 1;
        tag.putInt("forging_grade", nextGrade);
        if (recipe.qualityReward != null) {
            tag.putString("ite_quality", recipe.qualityReward);
            try {
                ClassicQuality.set(target, ClassicQuality.Level.valueOf(recipe.qualityReward.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        return new ForgeOutcome(true, nextGrade, roll, threshold);
    }

    private IteForgingRecipes0417() {}
}
