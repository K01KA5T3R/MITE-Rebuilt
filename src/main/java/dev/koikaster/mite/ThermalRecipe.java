package dev.koikaster.mite;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
public record ThermalRecipe(ResourceLocation id,Ingredient input,ItemStack output,int heat,int time,float experience) implements Recipe<Container> {
 public ThermalRecipe {
  time=LegacyFurnace.COOK_TICKS;
  if(id==null||input==null||input.isEmpty()||output==null||output.isEmpty()||output.getCount()>output.getMaxStackSize()||heat<1||heat>4||time<1||time>72000||!Float.isFinite(experience)||experience<0||experience>1000)
   throw new IllegalArgumentException("Invalid thermal recipe: "+id);
 }
 public boolean matches(Container c,Level l){return input.test(c.getItem(0));}
 public ItemStack assemble(Container c,RegistryAccess r){return output.copy();}
 public boolean canCraftInDimensions(int w,int h){return true;}
 public ItemStack getResultItem(RegistryAccess r){return output;}
 public ResourceLocation getId(){return id;}
 public RecipeSerializer<?> getSerializer(){return Content.THERMAL_SERIALIZER.get();}
 public net.minecraft.core.NonNullList<Ingredient> getIngredients(){return net.minecraft.core.NonNullList.of(Ingredient.EMPTY,input);}
 public RecipeType<?> getType(){return Content.THERMAL.get();}
 public boolean isSpecial(){return true;}
 public static final class Serializer implements RecipeSerializer<ThermalRecipe> {
  public ThermalRecipe fromJson(ResourceLocation id,JsonObject j){return new ThermalRecipe(id,Ingredient.fromJson(j.get("ingredient")),ShapedRecipe.itemStackFromJson(j.getAsJsonObject("result")),GsonHelper.getAsInt(j,"heat",1),GsonHelper.getAsInt(j,"cookingtime",200),GsonHelper.getAsFloat(j,"experience",.1f));}
  public ThermalRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new ThermalRecipe(id,Ingredient.fromNetwork(b),b.readItem(),b.readVarInt(),b.readVarInt(),b.readFloat());}
  public void toNetwork(FriendlyByteBuf b,ThermalRecipe r){r.input.toNetwork(b);b.writeItem(r.output);b.writeVarInt(r.heat);b.writeVarInt(r.time);b.writeFloat(r.experience);}
 }
}
