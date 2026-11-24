package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** RecipesMITE: one bucket ->1..4 bowls, or four bowls ->one matching empty
 * bucket. R195 discards unused liquid on partial decanting; it has no partial bucket. */
public final class SourceBowlTransferRecipe extends CustomRecipe {
 private record Plan(int vessel,String fluid,int bowls,boolean filling){}
 public SourceBowlTransferRecipe(ResourceLocation id,CraftingBookCategory category){super(id,category);}
 private Plan plan(CraftingContainer grid){int vessel=-1,bowls=0;String fluid="";Boolean filling=null;
  for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(stack.isEmpty())continue;
   if(ClassicVessels.handled(stack)){if(vessel!=-1)return null;vessel=i;String value=ClassicVessels.fluid(stack);if(!value.isEmpty()&&!value.equals("milk")&&!value.equals("water"))return null;boolean direction=value.isEmpty();if(filling!=null&&filling!=direction)return null;filling=direction;if(!value.isEmpty()){if(!fluid.isEmpty()&&!fluid.equals(value))return null;fluid=value;}continue;}
   if(stack.is(Items.BOWL)){if(filling!=null&&filling)return null;filling=false;bowls++;continue;}
   String value=OriginalBowls.water(stack)?"water":stack.is(Content.get("milk_bowl"))?"milk":"";if(value.isEmpty()||filling!=null&&!filling||!fluid.isEmpty()&&!fluid.equals(value))return null;filling=true;fluid=value;bowls++;
  }
  if(vessel<0||filling==null||fluid.isEmpty()||bowls<1||bowls>4||filling&&bowls!=4)return null;
  var bucket=grid.getItem(vessel);if(filling!=ClassicVessels.fluid(bucket).isEmpty())return null;return new Plan(vessel,fluid,bowls,filling);
 }
 public boolean matches(CraftingContainer grid,Level level){return plan(grid)!=null;}
 public ItemStack assemble(CraftingContainer grid,RegistryAccess access){var plan=plan(grid);if(plan==null)return ItemStack.EMPTY;return plan.filling?ClassicVessels.peer(grid.getItem(plan.vessel),plan.fluid):new ItemStack(Content.get(plan.fluid+"_bowl"),plan.bowls);}
 public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid){var remaining=NonNullList.withSize(grid.getContainerSize(),ItemStack.EMPTY);var plan=plan(grid);if(plan==null)return remaining;for(int i=0;i<grid.getContainerSize();i++)if(!grid.getItem(i).isEmpty()){if(i==plan.vessel){if(!plan.filling)remaining.set(i,ClassicVessels.peer(grid.getItem(i),""));}else if(plan.filling)remaining.set(i,new ItemStack(Items.BOWL));}return remaining;}
 public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
 public RecipeSerializer<?> getSerializer(){return Content.SOURCE_BOWL_TRANSFER.get();}
}
