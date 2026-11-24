package dev.koikaster.mite.compat.jei;

import dev.koikaster.mite.*;
import java.util.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;

public final class JeiTimedCraftExtension implements ICraftingCategoryExtension {
 private final CraftingRecipe recipe;
 public JeiTimedCraftExtension(CraftingRecipe recipe){this.recipe=recipe;}
 public int getWidth(){return recipe instanceof ShapedRecipe shape?shape.getWidth():0;}
 public int getHeight(){return recipe instanceof ShapedRecipe shape?shape.getHeight():0;}
 public ResourceLocation getRegistryName(){return recipe instanceof JeiDisplayCraft display?display.actual.getId():recipe.getId();}
 public void setRecipe(IRecipeLayoutBuilder builder,ICraftingGridHelper grid,IFocusGroup focuses){
  grid.createAndSetIngredients(builder,recipe.getIngredients(),getWidth(),getHeight());
  var level=Minecraft.getInstance().level;if(level!=null)grid.createAndSetOutputs(builder,List.of(recipe.getResultItem(level.registryAccess()).copy()));
 }
 public List<Component> getTooltipStrings(double x,double y){
  if(x<58||x>92||y<12||y>42)return List.of();
  var result=new ArrayList<Component>();
  var player=Minecraft.getInstance().player;
  if(player!=null){
   boolean automatic=player.containerMenu instanceof MithrilAutoWorkbench.Menu;
   boolean table=automatic||player.containerMenu instanceof TimedCrafting.Menu||!recipe.canCraftInDimensions(2,2);int width=table?3:2;var grid=JeiDisplayCraft.grid(width);
   int rowWidth=recipe instanceof ShapedRecipe shape?shape.getWidth():width;
   for(int i=0;i<recipe.getIngredients().size();i++){var choices=recipe.getIngredients().get(i).getItems();if(choices.length>0)grid.setItem(i/rowWidth*width+i%rowWidth,choices[0].copyWithCount(1));}
   CraftingRecipe actual=recipe;
   if(recipe instanceof JeiDisplayCraft display)actual=display.actual;
   if(recipe instanceof JeiMixedPlankCraft display)actual=display.actual;
   var output=actual.getResultItem(player.level().registryAccess());
   boolean r196=WorldContentMode.r196(player.level());
   int ticks;
   if(automatic&&r196)ticks=CraftingDifficulty.automaticR196Period(player,grid,actual,output);
   else if(automatic)ticks=(int)Math.max(1,Math.min(Integer.MAX_VALUE-1d,LegacyCrafting.period(CraftingDifficulty.difficulty(player,grid,actual),0,true,false)*Settings.CRAFT_TIME));
   else ticks=CraftingDifficulty.period(player,grid,actual,table);
   if(r196)R196Workbench.requiredWorkbenchHint(grid,output,actual).ifPresent(result::add);
   result.add(Component.translatable("jei.mite_reforged.estimate",String.format(Locale.ROOT,"%.2f",ticks/20d)));
   int cost=ClassicCoins.mintCost(recipe.getResultItem(player.level().registryAccess()));if(cost>0)result.add(Component.translatable("jei.mite_reforged.coin_cost",cost));
  }
  return result;
 }
}
