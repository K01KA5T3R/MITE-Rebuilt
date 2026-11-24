package dev.koikaster.mite;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

final class SourceR196Crafting {
    static boolean allows(Level level, CraftingContainer grid, CraftingRecipe matchedRecipe) {
        if (!WorldContentMode.r196(level) || grid.getItems().stream().noneMatch(ItemStack::isDamaged)) return true;
        var recipe = recipeFor(level, grid, matchedRecipe);
        // Refilling a bandage and preserving wear during an upgrade are intentional damaged-input transactions.
        return recipe instanceof ClassicCraftRepair || recipe instanceof RepairItemRecipe
            || recipe instanceof BandageRefillRecipe || recipe instanceof SteelNetheriteRecipe;
    }

    private static CraftingRecipe recipeFor(Level level, CraftingContainer grid, CraftingRecipe matchedRecipe) {
        if (matchedRecipe != null) return matchedRecipe;
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null);
    }

    static boolean allowsWorkbench(Level level, CraftingContainer grid,
            CraftingRecipe recipe, ItemStack output, int availableHardness) {
        return !WorldContentMode.r196(level)
            || requiredHardness(grid, output, recipeFor(level, grid, recipe)) <= availableHardness;
    }

    static int requiredHardness(CraftingContainer grid, ItemStack output, Recipe<?> recipe) {
        if (output.isEmpty() || !ClassicToolCatalogue.sourceItem(output)) return 0;
        var item = output.getItem();
        if (item instanceof BlockItem block && block.getBlock() instanceof R196Workbench bench) {
            return bench.tier().predecessorHardness;
        }
        int original = R196RecipeMaterials.grade(recipe, grid, output).orElse(-1);
        if (original >= 0) return original;
        if (item instanceof BlockItem block) {
            var placed = block.getBlock();
            if (placed instanceof MithrilAutoWorkbench.BlockImpl) return R196Workbench.Tier.MITHRIL.hardness;
            if (placed.defaultBlockState().is(BlockTags.PLANKS)) return 1;
            if (placed instanceof MetalAnvilBlock anvil) return hardness(anvil.material);
            if (placed instanceof SourceMetalBuildings.MetalBars bars) return hardness(bars.material);
            if (placed instanceof SourceMetalBuildings.MetalDoor door) return hardness(door.material);
            if (placed instanceof SourceMetalBuildings.Strongbox strongbox) return hardness(strongbox.material);
            // Original storage blocks representing one known metal also need that metal's workbench.
            var key = ForgeRegistries.ITEMS.getKey(item);
            if (key != null && key.getPath().endsWith("_block")) {
                return hardness(key.getPath().substring(0, key.getPath().length() - "_block".length()));
            }
        }
        var tool = ClassicToolWear.profile(output);
        if (tool != null) return tool.kind() == LegacyToolWear.Kind.KNIFE ? 0 : hardness(tool.material());
        var armor = OriginalArmorItems.profile(output);
        if (armor != null) return hardness(armor.material());
        if (item instanceof BowItem || item instanceof FishingRodItem) {
            return hardness(ClassicToolCatalogue.material(output));
        }
        if (item instanceof SpecialItems.MetalBucket || output.is(Items.BUCKET)) {
            boolean reusedBucket = grid.getItems().stream().anyMatch(stack ->
                stack.getItem() instanceof SpecialItems.MetalBucket || stack.is(Items.BUCKET));
            return reusedBucket ? 0 : hardness(item instanceof SpecialItems.MetalBucket bucket ? bucket.material : "iron");
        }
        return 0;
    }

    private static int hardness(String material) {
        return switch (material) {
            case "copper", "silver", "gold", "rusted_iron", "bronze" -> R196Workbench.Tier.COPPER.hardness;
            case "iron", "steel" -> R196Workbench.Tier.IRON.hardness;
            case "ancient_metal" -> R196Workbench.Tier.ANCIENT_METAL.hardness;
            case "mithril" -> R196Workbench.Tier.MITHRIL.hardness;
            case "adamantium", "vibranium" -> R196Workbench.Tier.ADAMANTIUM.hardness;
            default -> 0;
        };
    }
    private SourceR196Crafting() {}
}
