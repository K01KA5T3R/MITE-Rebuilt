package dev.koikaster.mite.compat.jei;

import dev.koikaster.mite.ClassicMixedPlanksRecipe;
import dev.koikaster.mite.Content;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Client display layouts for a shaped recipe whose plank slots have a shared constraint. */
public final class JeiMixedPlankCraft extends ShapedRecipe {
    final ClassicMixedPlanksRecipe actual;

    private JeiMixedPlankCraft(ClassicMixedPlanksRecipe actual, RegistryAccess access,
            NonNullList<Ingredient> ingredients, int index) {
        super(Content.id("jei/mixed_planks/" + actual.getId().getNamespace() + "/" + actual.getId().getPath() + "/" + index),
            actual.getGroup(), actual.category(), actual.getWidth(), actual.getHeight(), ingredients, actual.getResultItem(access).copy());
        this.actual = actual;
    }

    static void addViews(List<CraftingRecipe> views, ClassicMixedPlanksRecipe recipe, RegistryAccess access) {
        var mixed = recipe.mixedIngredients();
        var original = recipe.getIngredients();
        int first = -1;
        int second = -1;
        for (int i = 0; i < mixed.size(); i++) {
            if (mixed.get(i) == original.get(i)) continue;
            if (first == -1) first = i;
            else {
                second = i;
                break;
            }
        }
        if (second == -1) return;
        var planks = mixed.get(first).getItems();
        // Every pair has a lower and a higher index. The two slots have disjoint choices,
        // so every cycling display and every transfer leaves at least two different woods in the grid.
        for (int i = 0; i < planks.length - 1; i++) {
            var ingredients = NonNullList.of(Ingredient.EMPTY, mixed.toArray(Ingredient[]::new));
            ingredients.set(first, Ingredient.of(planks[i]));
            ingredients.set(second, Ingredient.of(Arrays.stream(planks, i + 1, planks.length)));
            views.add(new JeiMixedPlankCraft(recipe, access, ingredients, i));
        }
    }
}
