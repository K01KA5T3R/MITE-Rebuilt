package dev.koikaster.mite.compat.jei;

import dev.koikaster.mite.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** JEI-only examples of special recipes. They are not registered into the
 * game's RecipeManager and cannot become another production/acquisition path. */
public final class JeiDisplayCraft extends ShapelessRecipe {
 public final CraftingRecipe actual;
 private JeiDisplayCraft(ResourceLocation id,CraftingRecipe actual,List<ItemStack> inputs,ItemStack output){
  super(id,"",CraftingBookCategory.MISC,output,ingredients(inputs));this.actual=actual;
 }
 private static NonNullList<Ingredient> ingredients(List<ItemStack> inputs){var list=NonNullList.<Ingredient>create();for(var input:inputs)list.add(Ingredient.of(input.copy()));return list;}
 static CraftingContainer grid(int width){return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(Player p){return true;}},width,width);}
 private static void example(List<CraftingRecipe> rows,Level level,CraftingRecipe actual,String id,List<ItemStack> inputs){
  var grid=grid(3);for(int i=0;i<inputs.size();i++)grid.setItem(i,inputs.get(i).copy());
  var player=net.minecraft.client.Minecraft.getInstance().player;if(player!=null)ClassicCoins.bind(grid,player);
  if(actual.matches(grid,level)){var output=actual.assemble(grid,level.registryAccess());if(!output.isEmpty()&&!JeiHeatRecipes.hidden(output))rows.add(new JeiDisplayCraft(Content.id("jei/"+id),actual,inputs,output));}
 }
 public static List<CraftingRecipe> collect(Level level){
  var result=new ArrayList<CraftingRecipe>();var buckets=new ArrayList<ItemStack>();buckets.add(new ItemStack(Items.BUCKET));
  for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues())if(item instanceof SpecialItems.MetalBucket)buckets.add(new ItemStack(item));
  for(var recipe:level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)){
   if(recipe instanceof ClassicMixedPlanksRecipe mixed)JeiMixedPlankCraft.addViews(result,mixed,level.registryAccess());
   if(recipe instanceof RustedIronContent.Reclaim)for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()){
    var input=new ItemStack(item);if(RustedIronContent.reclaim(input)>0)example(result,level,recipe,"rusted_reclaim/"+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).getPath(),List.of(input));
   }
   if(recipe instanceof SourceBowlTransferRecipe)for(var bucket:buckets){
    String metal=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(bucket.getItem()).getPath();
    for(String fluid:List.of("water","milk")){
     for(int bowls=1;bowls<=4;bowls++){var input=new ArrayList<ItemStack>();input.add(ClassicVessels.peer(bucket,fluid));for(int n=0;n<bowls;n++)input.add(new ItemStack(Items.BOWL));example(result,level,recipe,"decant/"+metal+"/"+fluid+"/"+bowls,input);}
     var input=new ArrayList<ItemStack>();input.add(bucket.copy());for(int n=0;n<4;n++)input.add(new ItemStack(Content.get(fluid+"_bowl")));example(result,level,recipe,"fill/"+metal+"/"+fluid,input);
    }
   }
   if(recipe instanceof VesselStoneRecipe)for(var bucket:buckets)example(result,level,recipe,"stone_bucket/"+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(bucket.getItem()).getPath(),List.of(ClassicVessels.peer(bucket,"stone")));
   if(recipe instanceof DisenchantingBottle.Recipe)example(result,level,recipe,"disenchanting_bottle",List.of(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION),net.minecraft.world.item.alchemy.Potions.WATER),new ItemStack(Items.NETHER_WART),new ItemStack(Items.COAL)));
   if(recipe instanceof ClassicCraftRepair)for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()){
    var worn=new ItemStack(item);if(JeiHeatRecipes.hidden(worn)||!ClassicToolCatalogue.sourceItem(worn)||!worn.isDamageableItem()||worn.getMaxDamage()<2)continue;
    worn.setDamageValue(worn.getMaxDamage()-1);var other=worn.copy();other.setDamageValue(Math.max(1,worn.getMaxDamage()/2));
    String key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString().replace(':','/');
    example(result,level,recipe,"repair/pair/"+key,List.of(worn,other));
    if(item instanceof ArmorItem armor&&armor.getMaterial()==ArmorMaterials.LEATHER)for(var thread:List.of(Items.STRING,Content.get("sinew")))example(result,level,recipe,"repair/leather/"+key+"/"+net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(thread).getPath(),List.of(worn,new ItemStack(thread)));
   }
  }
  return List.copyOf(result);
 }
}
