package dev.koikaster.mite;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class BandageRefillRecipe extends CustomRecipe {
    public BandageRefillRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private static ItemStack refillInput(CraftingContainer grid) {
        ItemStack bandage = ItemStack.EMPTY;
        boolean leather = false;
        for (int i = 0; i < grid.getContainerSize(); ++i) {
            var input = grid.getItem(i);
            if (input.isEmpty()) continue;
            if (input.is(Content.BANDAGE.get()) && input.getCount() == 1 && input.isDamaged() && bandage.isEmpty()) {
                bandage = input;
            } else if (input.is(Items.LEATHER) && !leather) {
                leather = true;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return leather ? bandage : ItemStack.EMPTY;
    }

    @Override public boolean matches(CraftingContainer grid, Level level) {
        return !refillInput(grid).isEmpty();
    }

    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        var input = refillInput(grid);
        if (input.isEmpty()) return ItemStack.EMPTY;
        var output = input.copy();
        output.setDamageValue(Math.min(input.getDamageValue(), input.getMaxDamage()) - 1);
        return output;
    }

    @Override public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override public RecipeSerializer<?> getSerializer() {
        return Content.BANDAGE_REFILL.get();
    }
}
