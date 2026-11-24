package dev.koikaster.mite;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

/** wd.a: a bowl becomes the result bowl, so another empty bowl is not returned.
 * Non-bowl products (dough, cake, cheese, cookies) retain the native remainders. */
public final class SourceBowlFoodRecipe extends ShapelessRecipe {
 private final ShapelessRecipe delegate;
 public SourceBowlFoodRecipe(ShapelessRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY),recipe.getIngredients());delegate=recipe;}
 public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){var remaining=delegate.getRemainingItems(grid);if(OriginalBowls.full(delegate.getResultItem(net.minecraft.core.RegistryAccess.EMPTY)))for(int i=0;i<remaining.size();i++)if(remaining.get(i).is(Items.BOWL))remaining.set(i,ItemStack.EMPTY);return remaining;}
 public RecipeSerializer<?> getSerializer(){return Content.SOURCE_BOWL_FOOD.get();}
 public static final class Serializer implements RecipeSerializer<SourceBowlFoodRecipe>{
  public SourceBowlFoodRecipe fromJson(ResourceLocation id,JsonObject json){return new SourceBowlFoodRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id,json));}
  public SourceBowlFoodRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new SourceBowlFoodRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id,buffer));}
  public void toNetwork(FriendlyByteBuf buffer,SourceBowlFoodRecipe recipe){RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer,recipe.delegate);}
 }
}
