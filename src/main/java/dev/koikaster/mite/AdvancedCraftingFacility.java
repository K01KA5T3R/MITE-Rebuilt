package dev.koikaster.mite;

import java.util.*;
import net.minecraft.world.inventory.*;

/** This checkout has no general bench-tier system. Use its existing adamantium
 * (or higher) anvil beside the timed 3x3 table as the high-grade facility. */
public final class AdvancedCraftingFacility {
 private static final Map<CraftingContainer,Context> CONTEXTS=Collections.synchronizedMap(new WeakHashMap<>());
 private static final class Context {final ContainerLevelAccess access;boolean last;Context(ContainerLevelAccess access){this.access=access;last=valid();}boolean valid(){return access.evaluate((level,pos)->{if(!R196Workbench.canOpen(level.getBlockState(pos),level))return false;for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var adjacent=pos.relative(direction);if(level.hasChunkAt(adjacent)&&level.getBlockState(adjacent).getBlock() instanceof MetalAnvilBlock anvil&&LegacyHarvest.material(anvil.material)>=5)return true;}return false;},false);}}
 public static void bind(CraftingContainer grid,ContainerLevelAccess access){CONTEXTS.put(grid,new Context(access));}
 public static boolean allows(CraftingContainer grid){var context=CONTEXTS.get(grid);return context!=null&&context.valid();}
 public static boolean environmentChanged(CraftingContainer grid){var context=CONTEXTS.get(grid);if(context==null)return false;boolean now=context.valid(),changed=now!=context.last;context.last=now;return changed&&context.access.evaluate((level,pos)->level.getRecipeManager().byKey(Content.id("advanced_enchanting_table")).filter(AdvancedTableRecipe.class::isInstance).map(AdvancedTableRecipe.class::cast).map(recipe->recipe.matchesIngredients(grid,level)).orElse(false),false);}
 public static boolean ready(net.minecraft.world.item.crafting.CraftingRecipe recipe,CraftingContainer grid){return !(recipe instanceof AdvancedTableRecipe)||allows(grid);}
 private AdvancedCraftingFacility(){}
}
