package dev.koikaster.mite;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Stateless ITE Golden Apple recipe selector integrated into WorldContentEvents#restrictRecipes.
 * Replaces 3x3 ring of nuggets/ingots around apple with:
 *   ### / I X I / ###  (# = gold_nugget, I = gold_ingot, X = apple) when WorldContentMode.ite is enabled.
 */
public final class IteGoldenAppleRecipes {
    private static final ResourceLocation TARGET = new ResourceLocation("minecraft", "golden_apple");

    public static Recipe<?> select(Recipe<?> effectiveBaseline, RegistryAccess registries, boolean ite) {
        if (!ite || !effectiveBaseline.getId().equals(TARGET)
                || effectiveBaseline.getClass() != ShapedRecipe.class) return effectiveBaseline;
        var shaped = (ShapedRecipe) effectiveBaseline;
        var output = shaped.getResultItem(registries);
        if (!output.is(Items.GOLDEN_APPLE) || output.getCount() != 1 || output.hasTag()
                || shaped.getWidth() != 3 || shaped.getHeight() != 3 || shaped.getIngredients().size() != 9) {
            return effectiveBaseline;
        }
        if (!ordinaryRing(shaped, Items.GOLD_NUGGET) && !ordinaryRing(shaped, Items.GOLD_INGOT)) {
            return effectiveBaseline;
        }
        var nugget = Ingredient.of(Items.GOLD_NUGGET);
        var ingot = Ingredient.of(Items.GOLD_INGOT);
        var apple = Ingredient.of(Items.APPLE);
        var inputs = NonNullList.of(Ingredient.EMPTY,
            nugget, nugget, nugget,
            ingot, apple, ingot,
            nugget, nugget, nugget);
        return new ShapedRecipe(shaped.getId(), shaped.getGroup(), shaped.category(), 3, 3,
            inputs, output.copy(), shaped.showNotification());
    }

    private static boolean ordinaryRing(ShapedRecipe recipe, Item gold) {
        for (int index = 0; index < 9; index++) {
            var ingredient = recipe.getIngredients().get(index);
            if (ingredient.getClass() != Ingredient.class) return false;
            var json = ingredient.toJson();
            if (!json.isJsonObject()) return false;
            var value = json.getAsJsonObject();
            var item = value.get("item");
            if (value.size() != 1 || item == null || !item.isJsonPrimitive()
                    || !item.getAsJsonPrimitive().isString()
                    || !BuiltInRegistries.ITEM.getKey(index == 4 ? Items.APPLE : gold).toString().equals(item.getAsString())) {
                return false;
            }
        }
        return true;
    }

    private IteGoldenAppleRecipes() {}
}
