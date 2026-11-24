package dev.koikaster.mite;

import java.util.Set;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.registries.RegistryObject;

/** X001 independent expansion. Source rusted gear stats and repair stay source-owned. */
public final class RustedIronContent {
 public static final RegistryObject<Item> NUGGET=Content.item("rusted_iron_nugget",()->new Item(new Item.Properties().stacksTo(64)));
 public static final RegistryObject<Item> INGOT=Content.item("rusted_iron_ingot",()->new Item(new Item.Properties().stacksTo(8)));
 public static final RegistryObject<RecipeSerializer<Reclaim>> RECLAIM=Content.SERIALIZERS.register("rusted_iron_reclaim",()->new SimpleCraftingRecipeSerializer<>(Reclaim::new));
 private static final Set<String> CACHES=Set.of("chests/abandoned_mineshaft","chests/simple_dungeon","chests/underwater_ruin_small","chests/underwater_ruin_big");
 public static void register(){}
 /** Chest loot is rolled once by the actual finite container. No entity/fishing/bartering source. */
 static void loot(ObjectArrayList<ItemStack> loot,LootContext context){var id=context.getQueriedLootTableId();if(WorldContentMode.rebuilt(context.getLevel())&&(UnderworldExplorationRewards.MINESHAFT_CART.equals(id)||(id.getNamespace().equals("minecraft")&&CACHES.contains(id.getPath())))&&context.getRandom().nextInt(2)==0)loot.add(new ItemStack(NUGGET.get(),3+context.getRandom().nextInt(7)));}
 public static int reclaim(ItemStack stack){
  if(stack.getCount()!=1||!stack.isDamageableItem()||MineralSupply.source(stack)==MineralSupply.GENERATED)return 0;
  var p=ClassicRepair.profile(stack);if(p==null||!p.material().equals("rusted_iron"))return 0;
  long original=(long)p.components()*(p.chain()?4:9),remaining=Math.max(0,stack.getMaxDamage()-stack.getDamageValue());
  return (int)(original*remaining/(2L*stack.getMaxDamage()));
 }
 public static final class Reclaim extends CustomRecipe {
  public Reclaim(ResourceLocation id,CraftingBookCategory category){super(id,category);}
  private static ItemStack input(CraftingContainer grid){ItemStack found=ItemStack.EMPTY;for(int i=0;i<grid.getContainerSize();i++){var s=grid.getItem(i);if(s.isEmpty())continue;if(!found.isEmpty())return ItemStack.EMPTY;found=s;}return found;}
  public boolean matches(CraftingContainer grid,Level level){return WorldContentMode.rebuilt(level)&&reclaim(input(grid))>0;}
  public ItemStack assemble(CraftingContainer grid,RegistryAccess access){int n=reclaim(input(grid));return n==0?ItemStack.EMPTY:new ItemStack(NUGGET.get(),n);}
  public boolean canCraftInDimensions(int w,int h){return w*h>=1;}
  public RecipeSerializer<?> getSerializer(){return RECLAIM.get();}
 }
 private RustedIronContent(){}
}
