package dev.koikaster.mite;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/** V051 replacement for the removed phantom membrane; preserve all three native bottle forms. */
public final class ModernBrewing implements IBrewingRecipe {
 public boolean isInput(ItemStack stack){return stack.getItem() instanceof PotionItem&&PotionUtils.getPotion(stack)==Potions.AWKWARD;}
 public boolean isIngredient(ItemStack stack){return stack.is(Items.SHULKER_SHELL);}
 public ItemStack getOutput(ItemStack input,ItemStack ingredient){return isInput(input)&&isIngredient(ingredient)?PotionUtils.setPotion(new ItemStack(input.getItem()),Potions.SLOW_FALLING):ItemStack.EMPTY;}
 public static void register(){BrewingRecipeRegistry.addRecipe(new ModernBrewing());}
 private ModernBrewing(){}
}
