package dev.koikaster.mite.compat.jei;
import java.util.*;import dev.koikaster.mite.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;import mezz.jei.api.gui.drawable.IDrawable;import mezz.jei.api.gui.ingredient.IRecipeSlotsView;import mezz.jei.api.helpers.IGuiHelper;import mezz.jei.api.recipe.*;import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.network.chat.Component;import net.minecraft.world.item.ItemStack;
public final class JeiDrying1905 {
 public record View(ItemStack input,ItemStack output){}
 public static final RecipeType<View> TYPE=RecipeType.create(MiteReforged.ID,"drying",View.class);
 public static List<View> collect(){return FoodPreservation1905.FOODS.stream().map(f->new View(new ItemStack(FoodPreservation1905.resolve(f.raw())),new ItemStack(FoodPreservation1905.DRIED.get(f.key()).get()))).toList();}
 public static final class Category implements IRecipeCategory<View>{
  private final IDrawable icon;public Category(IGuiHelper gui){icon=gui.createDrawableItemStack(new ItemStack(FoodPreservation1905.ITEM.get()));}public RecipeType<View> getRecipeType(){return TYPE;}public Component getTitle(){return Component.translatable("block.mite_reforged.drying_rack");}public IDrawable getIcon(){return icon;}public int getWidth(){return 166;}public int getHeight(){return 48;}
  public void setRecipe(IRecipeLayoutBuilder builder,View view,IFocusGroup focus){builder.addSlot(RecipeIngredientRole.INPUT,8,5).addItemStack(view.input());builder.addSlot(RecipeIngredientRole.OUTPUT,141,5).addItemStack(view.output());}
  public void draw(View view,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY){var font=Minecraft.getInstance().font;graphics.fill(7,4,25,22,0xff555555);graphics.fill(8,5,24,21,0xffdddddd);graphics.fill(140,4,158,22,0xff555555);graphics.fill(141,5,157,21,0xffdddddd);graphics.drawString(font,"→",79,8,0xff555555,false);graphics.drawString(font,"4:00",70,27,0xff333333,false);graphics.drawString(font,Component.translatable("jei.mite_reforged.drying_conditions"),3,39,0xff333333,false);}
 }
 private JeiDrying1905(){}
}
