package dev.koikaster.mite;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** aaj propagates the last tagged ingredient, in slot order. Legacy item damage
 * is a separate field; modern Damage is reset on the newly constructed product. */
public final class SourceCarrotStickRecipe extends ShapelessRecipe {
 private final ShapelessRecipe delegate;
 public SourceCarrotStickRecipe(ShapelessRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),recipe.getResultItem(RegistryAccess.EMPTY),recipe.getIngredients());delegate=recipe;}
 public boolean dismantling(){return getResultItem(RegistryAccess.EMPTY).getItem() instanceof FishingRodItem;}
 @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){var out=delegate.getResultItem(access).copy();for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.getTag()==null)continue;var tag=stack.getTag().copy();boolean damageOnly=tag.getAllKeys().size()==1&&tag.contains("Damage");tag.remove("Damage");if(!damageOnly)out.setTag(tag);}out.setDamageValue(0);return out;}
 @Override public RecipeSerializer<?> getSerializer(){return SourceCarrotSticks.RECIPE.get();}
 public static final class Serializer implements RecipeSerializer<SourceCarrotStickRecipe>{
  public SourceCarrotStickRecipe fromJson(ResourceLocation id,JsonObject json){return new SourceCarrotStickRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id,json));}
  public SourceCarrotStickRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf data){return new SourceCarrotStickRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id,data));}
  public void toNetwork(FriendlyByteBuf data,SourceCarrotStickRecipe recipe){RecipeSerializer.SHAPELESS_RECIPE.toNetwork(data,recipe.delegate);}
 }
}
