package dev.koikaster.mite;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
/** Standard shapeless discovery,without repairing or stripping the input bow. */
public final class TorchBowRecipe extends ShapelessRecipe {
 TorchBowRecipe(ShapelessRecipe recipe){super(recipe.getId(),recipe.getGroup(),recipe.category(),new ItemStack(TorchBowContent.BOW.get()),recipe.getIngredients());}
 @Override public ItemStack assemble(CraftingContainer input,RegistryAccess registry){var output=super.assemble(input,registry);for(int i=0;i<input.getContainerSize();i++)if(input.getItem(i).is(Items.BOW)){var bow=input.getItem(i);if(bow.hasTag())output.setTag(bow.getTag().copy());break;}return output;}
 @Override public RecipeSerializer<?> getSerializer(){return TorchBowContent.RECIPE.get();}
 public static final class Serializer implements RecipeSerializer<TorchBowRecipe> {
  @Override public TorchBowRecipe fromJson(ResourceLocation id,JsonObject json){return new TorchBowRecipe(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id,json));}
  @Override public TorchBowRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){var recipe=RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id,buffer);return recipe==null?null:new TorchBowRecipe(recipe);}
  @Override public void toNetwork(FriendlyByteBuf buffer,TorchBowRecipe recipe){RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer,recipe);}
 }
}
