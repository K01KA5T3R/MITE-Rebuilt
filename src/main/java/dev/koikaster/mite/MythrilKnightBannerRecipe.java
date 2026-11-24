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

final class MythrilKnightBannerRecipe extends CustomRecipe {
    MythrilKnightBannerRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        if (!WorldContentMode.rebuiltBosses(level)) return false;
        boolean emerald = false;
        boolean banner = false;
        for (int slot = 0; slot < grid.getContainerSize(); ++slot) {
            var stack = grid.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!emerald && stack.is(Items.EMERALD)) emerald = true;
            else if (!banner && (BloodBannerEvents.ominous(stack)
                    || !WorldContentMode.rebuilt(level) && stack.is(Items.WHITE_BANNER))) banner = true;
            else return false;
        }
        return emerald && banner;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return new ItemStack(MythrilKnightContent.BANNER.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MythrilKnightContent.BANNER_RECIPE.get();
    }
}
