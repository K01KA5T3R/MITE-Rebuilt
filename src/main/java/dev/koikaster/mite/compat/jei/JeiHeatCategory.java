package dev.koikaster.mite.compat.jei;

import java.util.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class JeiHeatCategory implements IRecipeCategory<JeiHeatRecipes.View>{
 private final JeiHeatRecipes.Device device;private final IDrawable icon;
 public JeiHeatCategory(JeiHeatRecipes.Device device,IGuiHelper gui){this.device=device;icon=gui.createDrawableItemStack(device.catalysts().get(0));}
 public RecipeType<JeiHeatRecipes.View> getRecipeType(){return device.type();}
 public Component getTitle(){return device.catalysts().get(0).getHoverName();}
 public IDrawable getIcon(){return icon;}
 public int getWidth(){return 166;}public int getHeight(){return 78;}
 public void setRecipe(IRecipeLayoutBuilder builder,JeiHeatRecipes.View view,IFocusGroup focus){
  builder.addSlot(RecipeIngredientRole.INPUT,12,12).addItemStack(view.input());
  builder.addSlot(RecipeIngredientRole.INPUT,12,43).addItemStacks(view.fuels());
  builder.addSlot(RecipeIngredientRole.OUTPUT,139,12).addItemStack(view.output());
 }
 public void draw(JeiHeatRecipes.View view,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY){
  var font=Minecraft.getInstance().font;
  for(int y:new int[]{11,42}){graphics.fill(11,y,29,y+18,0xff555555);graphics.fill(12,y+1,28,y+17,0xffdddddd);}
  graphics.fill(138,11,157,30,0xff555555);graphics.fill(139,12,156,29,0xffdddddd);
  graphics.drawString(font,"→",83,16,0xff555555,false);
  graphics.drawString(font,Component.translatable("jei.mite_reforged.heat",view.minHeat()==view.maxHeat()?""+view.minHeat():view.minHeat()+"–"+view.maxHeat()),37,44,0xff333333,false);
  graphics.drawString(font,Component.translatable("jei.mite_reforged.time",view.device().ticks()/20),37,59,0xff333333,false);
 }
}
