package dev.koikaster.mite;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class AdvancedTableRecipe extends ShapedRecipe {
 public AdvancedTableRecipe(ShapedRecipe base){super(base.getId(),base.getGroup(),base.category(),base.getWidth(),base.getHeight(),base.getIngredients(),base.getResultItem(RegistryAccess.EMPTY));}
 @Override public boolean matches(CraftingContainer grid,Level level){return AdvancedCraftingFacility.allows(grid)&&super.matches(grid,level);}
 public boolean matchesIngredients(CraftingContainer grid,Level level){return super.matches(grid,level);}
 @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registries){return AdvancedCraftingFacility.allows(grid)?super.assemble(grid,registries):ItemStack.EMPTY;}
 @Override public RecipeSerializer<?> getSerializer(){return AdvancedEnchantContent.RECIPE.get();}
 public static final class Serializer implements RecipeSerializer<AdvancedTableRecipe> {
  private final ShapedRecipe.Serializer delegate=new ShapedRecipe.Serializer();
  public AdvancedTableRecipe fromJson(ResourceLocation id,JsonObject json){return new AdvancedTableRecipe(delegate.fromJson(id,json));}
  public AdvancedTableRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new AdvancedTableRecipe(delegate.fromNetwork(id,buffer));}
  public void toNetwork(FriendlyByteBuf buffer,AdvancedTableRecipe recipe){delegate.toNetwork(buffer,recipe);}
 }
}
