package dev.koikaster.mite;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import net.minecraft.world.item.crafting.Recipe;

/**
 * 0.4.17 Original MITE R195/R196 (uf/wd) quality crafting selection,
 * min/max tier derivation, XP cost, and unskilled POOR-tier cap.
 */
public final class ClassicQualityCrafting {
    public static boolean hasSkill(Player player, Recipe<?> recipe, CraftingContainer grid, ItemStack output) {
        return player == null || SourceSkillCrafting1710.hasSkill(player, recipe, grid, output);
    }

    public static boolean isExemptRecipe(Recipe<?> recipe) {
        return recipe instanceof ClassicCraftRepair
            || recipe instanceof ArmorDyeRecipe
            || recipe instanceof SteelNetheriteRecipe
            || recipe instanceof BandageRefillRecipe
            || recipe instanceof DisenchantingBottle.Recipe;
    }

    public static boolean supportsQuality(Recipe<?> recipe, ItemStack output) {
        return recipe != null && !output.isEmpty() && ClassicQuality.eligible(output) && !isExemptRecipe(recipe);
    }

    public static boolean hasClumsiness(Player player) {
        return player != null && OriginalCurseEffects.has(player, 8, false);
    }

    public static int availableXp(Player player) {
        return player == null ? 0 : PlayerState.data(player).getInt("Xp");
    }

    public static ClassicQuality.Level minQuality(Player player, ItemStack output, boolean hasSkill) {
        ClassicQuality.Level cap = ClassicQuality.maxQuality(output);
        if (cap == null) return null;
        int avg = ClassicQuality.Level.AVERAGE.ordinal();
        int effectiveLevel = (player == null ? 0 : PlayerState.level(player)) - (hasClumsiness(player) ? 20 : 0);
        int ord = avg + (effectiveLevel / 10);
        if (!hasSkill) {
            ord = Math.min(ord, ClassicQuality.Level.POOR.ordinal());
        }
        int upper = Math.min(cap.ordinal(), avg);
        return ClassicQuality.Level.byOrdinal(Math.max(0, Math.min(upper, ord)));
    }

    public static int xpCost(Player player, float baseDifficulty, ClassicQuality.Level quality, boolean exempt) {
        if (exempt || quality == null || quality.isAverageOrLower()) return 0;
        if (player != null && player.isCreative()) return 0;
        float adjusted = Math.max(0.0f, baseDifficulty) * quality.craftingDifficultyMultiplier();
        int cost = Math.round(adjusted / 5.0f);
        if (hasClumsiness(player)) cost *= 2;
        return Math.max(0, cost);
    }

    public static ClassicQuality.Level maxQuality(Player player, float baseDifficulty, ItemStack output, boolean hasSkill, boolean exempt) {
        ClassicQuality.Level cap = ClassicQuality.maxQuality(output);
        if (cap == null) return null;
        ClassicQuality.Level min = minQuality(player, output, hasSkill);
        if (player != null && player.isCreative() && hasSkill) return cap;
        if (!hasSkill || exempt) return min;
        int xp = availableXp(player);
        if (xp <= 0) return min;
        ClassicQuality.Level best = min;
        for (int i = min.ordinal() + 1; i <= cap.ordinal(); i++) {
            ClassicQuality.Level candidate = ClassicQuality.Level.byOrdinal(i);
            if (xpCost(player, baseDifficulty, candidate, false) <= xp) {
                best = candidate;
            } else {
                break;
            }
        }
        return best;
    }

    public static ClassicQuality.Level clampToRange(int selectedOrdinal, ClassicQuality.Level min, ClassicQuality.Level max) {
        if (min == null || max == null) return null;
        int lo = min.ordinal();
        int hi = Math.max(lo, max.ordinal());
        if (selectedOrdinal < lo || selectedOrdinal > hi) return min;
        return ClassicQuality.Level.byOrdinal(selectedOrdinal);
    }

    public static ClassicQuality.Level cycleNext(int currentOrdinal, ClassicQuality.Level min, ClassicQuality.Level max) {
        if (min == null || max == null) return null;
        int lo = min.ordinal();
        int hi = Math.max(lo, max.ordinal());
        if (lo == hi) return min;
        int cur = (currentOrdinal < lo || currentOrdinal > hi) ? lo : currentOrdinal;
        int next = cur + 1;
        return next > hi ? min : ClassicQuality.Level.byOrdinal(next);
    }

    private ClassicQualityCrafting() {}
}
