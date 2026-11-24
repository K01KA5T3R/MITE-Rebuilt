package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** RecipesMITE stone-bucket dismantling,100 difficulty, with no duplicate native container remainder. */
public final class VesselStoneRecipe extends CustomRecipe {
 public VesselStoneRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
 private ItemStack input(CraftingContainer grid){ItemStack found=ItemStack.EMPTY;for(var stack:grid.getItems())if(!stack.isEmpty()){if(!found.isEmpty()||!ClassicVessels.handled(stack)||!ClassicVessels.fluid(stack).equals("stone"))return ItemStack.EMPTY;found=stack;}return found;}
 public boolean matches(CraftingContainer grid,Level level){return !input(grid).isEmpty();}
 public ItemStack assemble(CraftingContainer grid,RegistryAccess access) {
  var input = input(grid);
  return input.isEmpty() ? ItemStack.EMPTY : ClassicVessels.peer(input, "");
 }
 public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){return NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);}
 public boolean canCraftInDimensions(int width,int height){return width*height>=1;}
 public RecipeSerializer<?> getSerializer(){return Content.VESSEL_STONE_SERIALIZER.get();}
}
