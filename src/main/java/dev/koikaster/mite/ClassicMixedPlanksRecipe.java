package dev.koikaster.mite;

import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public final class ClassicMixedPlanksRecipe extends ShapedRecipe {
    private final ShapedRecipe mixedPattern;
    private final Ingredient legacyPlanks;

    private ClassicMixedPlanksRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
            base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY), base.showNotification());

        // Keep the oak layout for recipe-book placement. The other single-wood layouts have their own recipes.
        legacyPlanks = Ingredient.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS);
        var oak = new ItemStack(Items.OAK_PLANKS);
        var ingredients = NonNullList.withSize(base.getIngredients().size(), Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            var ingredient = base.getIngredients().get(i);
            ingredients.set(i, ingredient.test(oak) ? legacyPlanks : ingredient);
        }
        mixedPattern = new ShapedRecipe(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
            ingredients, base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }

    public List<Ingredient> mixedIngredients() {
        return List.copyOf(mixedPattern.getIngredients());
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        if (super.matches(grid, level)) return true;
        if (!mixedPattern.matches(grid, level)) return false;

        Item firstPlank = null;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            var stack = grid.getItem(i);
            if (!legacyPlanks.test(stack)) continue;
            var plank = stack.getItem();
            if (firstPlank != null && plank != firstPlank) return true;
            firstPlank = plank;
        }
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Content.CLASSIC_MIXED_PLANKS_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<ClassicMixedPlanksRecipe> {
        private final ShapedRecipe.Serializer shaped = new ShapedRecipe.Serializer();

        @Override
        public ClassicMixedPlanksRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ClassicMixedPlanksRecipe(shaped.fromJson(id, json));
        }

        @Override
        public ClassicMixedPlanksRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ClassicMixedPlanksRecipe(shaped.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ClassicMixedPlanksRecipe recipe) {
            shaped.toNetwork(buffer, recipe);
        }
    }
}
