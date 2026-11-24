package dev.koikaster.mite;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Original shapeless coin declarations require a real player result transaction, including on automation entry. */
public final class CurrencyRecipe extends ShapelessRecipe {
 private CurrencyRecipe(ShapelessRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),recipe.getResultItem(RegistryAccess.EMPTY).copy(),recipe.getIngredients());}
 public boolean matches(CraftingContainer grid,Level level){return ClassicCoins.owner(grid)!=null&&super.matches(grid,level);}
 public RecipeSerializer<?> getSerializer(){return Content.CURRENCY_SERIALIZER.get();}
 public static final class Serializer implements RecipeSerializer<CurrencyRecipe>{
  private final ShapelessRecipe.Serializer source=new ShapelessRecipe.Serializer();
  public CurrencyRecipe fromJson(ResourceLocation id,JsonObject json){return new CurrencyRecipe(source.fromJson(id,json));}
  public CurrencyRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new CurrencyRecipe(source.fromNetwork(id,buffer));}
  public void toNetwork(FriendlyByteBuf buffer,CurrencyRecipe recipe){source.toNetwork(buffer,recipe);}
 }
}
