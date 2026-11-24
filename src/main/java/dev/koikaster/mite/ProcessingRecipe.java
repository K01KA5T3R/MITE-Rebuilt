package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.tags.ItemTags;
/** Retired invented log+axe recipe. Keep its serializer ID so old datapacks fail closed. */
public final class ProcessingRecipe extends CustomRecipe {
 public ProcessingRecipe(ResourceLocation id,CraftingBookCategory c){super(id,c);}
 public boolean matches(CraftingContainer c,Level l){return false;}
 public ItemStack assemble(CraftingContainer c,RegistryAccess r){return ItemStack.EMPTY;}
 public NonNullList<ItemStack> getRemainingItems(CraftingContainer c){return NonNullList.withSize(c.getContainerSize(),ItemStack.EMPTY);}
 public boolean canCraftInDimensions(int w,int h){return false;}
 public ItemStack getResultItem(RegistryAccess registries){return ItemStack.EMPTY;}
 public NonNullList<Ingredient> getIngredients(){return NonNullList.create();}
 public RecipeSerializer<?> getSerializer(){return Content.PROCESSING.get();}
}
