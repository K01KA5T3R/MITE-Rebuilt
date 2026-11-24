package dev.koikaster.mite;

import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** A normal shaped recipe whose availability belongs to the world. */
public final class R196BenchRecipe extends ShapedRecipe {
    private static final String LASHING_PREFIX = "r196_tools/";
    private static final Set<String> UNLASHED_R195 = Set.of(
        "flint_knife", "flint_hatchet", "flint_shovel", "flint_axe",
        "obsidian_knife", "obsidian_hatchet", "obsidian_shovel", "obsidian_axe");
    private final int lashingSlot;
    R196BenchRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
            base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY));
        lashingSlot = getId().getPath().startsWith(LASHING_PREFIX) ? recipeLashingSlot() : -1;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        if (!WorldContentMode.r196(level)) return false;
        if (lashingSlot < 0) return super.matches(grid, level);
        int looseTie = -1;
        var sinew = Content.get("sinew");
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            var actual = grid.getItem(slot);
            if (!actual.is(Items.STRING) && !actual.is(sinew)) continue;
            if (looseTie >= 0) return false;
            looseTie = slot;
        }
        if (looseTie < 0) return false;
        for (int x = 0; x <= grid.getWidth() - getWidth(); x++) {
            for (int y = 0; y <= grid.getHeight() - getHeight(); y++) {
                if (matchesUntied(grid, x, y, false, looseTie) || matchesUntied(grid, x, y, true, looseTie)) return true;
            }
        }
        return false;
    }

    private boolean matchesUntied(CraftingContainer grid, int offsetX, int offsetY, boolean mirrored, int looseTie) {
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                int cellX = x - offsetX;
                int cellY = y - offsetY;
                Ingredient expected = Ingredient.EMPTY;
                if (cellX >= 0 && cellY >= 0 && cellX < getWidth() && cellY < getHeight()) {
                    int recipeX = mirrored ? getWidth() - 1 - cellX : cellX;
                    int index = cellY * getWidth() + recipeX;
                    if (index != lashingSlot) expected = getIngredients().get(index);
                }
                int slot = y * grid.getWidth() + x;
                if (!expected.test(slot == looseTie ? ItemStack.EMPTY : grid.getItem(slot))) return false;
            }
        }
        return true;
    }

    private int recipeLashingSlot() {
        var string = new ItemStack(Items.STRING);
        var sinew = new ItemStack(Content.get("sinew"));
        int found = -1;
        for (int index = 0; index < getIngredients().size(); index++) {
            var ingredient = getIngredients().get(index);
            if (!ingredient.test(string) || !ingredient.test(sinew)) continue;
            if (found >= 0) throw new IllegalArgumentException("Multiple lashing ingredients in " + getId());
            found = index;
        }
        if (found < 0) throw new IllegalArgumentException("Missing lashing ingredient in " + getId());
        return found;
    }

    boolean usesLooseLashing() {
        return lashingSlot >= 0;
    }

    static boolean originalUnlashedTool(ResourceLocation id) {
        return id.getNamespace().equals(MiteReforged.ID) && UNLASHED_R195.contains(id.getPath());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        var remaining = super.getRemainingItems(grid);
        if (!dismantling()) return remaining;
        for (int index = 0; index < grid.getContainerSize(); index++) {
            if (grid.getItem(index).getItem() instanceof BlockItem item
                && item.getBlock() instanceof R196Workbench bench && bench.dismantledLog() != null) {
                remaining.set(index, new ItemStack(bench.dismantledLog()));
            }
        }
        return remaining;
    }

    boolean dismantling() {
        return getId().getPath().startsWith("r196_workbench/dismantle/");
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return R196Workbench.RECIPE.get();
    }

    public static final class Serializer implements RecipeSerializer<R196BenchRecipe> {
        private final ShapedRecipe.Serializer delegate = new ShapedRecipe.Serializer();

        @Override
        public R196BenchRecipe fromJson(ResourceLocation id, JsonObject data) {
            return new R196BenchRecipe(delegate.fromJson(id, data));
        }

        @Override
        public R196BenchRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new R196BenchRecipe(delegate.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, R196BenchRecipe recipe) {
            delegate.toNetwork(buffer, recipe);
        }
    }
}
