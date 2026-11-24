package dev.koikaster.mite;

import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** V123: normal recipe-book entries with per-input NBT and relative wear preservation. */
public final class SteelNetheriteRecipe extends ShapelessRecipe {
 private static final Set<String> EQUIPMENT=Set.of("sword","pickaxe","axe","shovel","hoe","helmet","chestplate","leggings","boots","warhammer","mattock","battleaxe","scythe","dagger","knife","hatchet","shears");
 private final String piece;
 private static String validate(String piece){if(!EQUIPMENT.contains(piece))throw new IllegalArgumentException("Not a steel/netherite equipment piece: "+piece);return piece;}
 private static ItemStack output(String piece){var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(NetheriteEquipment.EXTRA.contains(validate(piece))?MiteReforged.ID:"minecraft","netherite_"+piece));if(item==null||item==Items.AIR)throw new IllegalArgumentException("Missing netherite equipment: "+piece);return new ItemStack(item);}
 private static NonNullList<Ingredient> ingredients(String piece){return NonNullList.of(Ingredient.EMPTY,Ingredient.of(Content.get("steel_"+validate(piece))),Ingredient.of(Items.NETHERITE_INGOT),Ingredient.of(Items.NETHERITE_INGOT));}
 public SteelNetheriteRecipe(ResourceLocation id,String piece){super(id,"",CraftingBookCategory.EQUIPMENT,output(piece),ingredients(piece));this.piece=piece;}
 public boolean matches(CraftingContainer grid,Level level){return canCraftInDimensions(grid.getWidth(),grid.getHeight())&&super.matches(grid,level);}
 public ItemStack assemble(CraftingContainer grid,RegistryAccess access){
  ItemStack base=ItemStack.EMPTY;int ingots=0;
  for(int i=0;i<grid.getContainerSize();i++){
   var stack=grid.getItem(i);if(stack.isEmpty())continue;
   if(stack.is(Items.NETHERITE_INGOT)){ingots++;continue;}
   if(!base.isEmpty()||stack.getCount()!=1||!stack.is(Content.get("steel_"+piece)))return ItemStack.EMPTY;base=stack;
  }
  if(base.isEmpty()||ingots!=2||!canCraftInDimensions(grid.getWidth(),grid.getHeight()))return ItemStack.EMPTY;
  var result=getResultItem(access).copy();if(base.hasTag())result.setTag(base.getTag().copy());
  if(base.isDamageableItem())result.setDamageValue((int)Math.min(result.getMaxDamage()-1L,Math.round(base.getDamageValue()*(double)result.getMaxDamage()/Math.max(1,base.getMaxDamage()))));
  return result;
 }
 public boolean canCraftInDimensions(int width,int height){return width>=3&&height>=3;}
 public RecipeSerializer<?> getSerializer(){return Content.STEEL_NETHERITE_SERIALIZER.get();}
 public static final class Serializer implements RecipeSerializer<SteelNetheriteRecipe>{
  public SteelNetheriteRecipe fromJson(ResourceLocation id,JsonObject json){return new SteelNetheriteRecipe(id,GsonHelper.getAsString(json,"piece"));}
  public SteelNetheriteRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return new SteelNetheriteRecipe(id,buffer.readUtf(20));}
  public void toNetwork(FriendlyByteBuf buffer,SteelNetheriteRecipe recipe){buffer.writeUtf(recipe.piece,20);}
 }
}
