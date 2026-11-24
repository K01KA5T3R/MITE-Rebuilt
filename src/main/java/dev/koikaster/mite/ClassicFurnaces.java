package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Original aab/asg/we/aok rules with explicit modern block and raw-ore counterparts. */
public final class ClassicFurnaces {
 public record Plan(ItemStack output,int requiredHeat,int consumed,float xp){boolean exists(){return !output.isEmpty();}}
 public static final Plan NONE=new Plan(ItemStack.EMPTY,1,1,0);
 public static boolean large(ItemStack stack){var item=stack.getItem();if(item instanceof ItemNameBlockItem||item instanceof SignItem||item instanceof HangingSignItem||stack.is(ItemTags.SAPLINGS))return false;if(item instanceof BlockItem bi){var b=bi.getBlock();return !(b instanceof TorchBlock||b instanceof SaplingBlock||b instanceof FlowerBlock||b instanceof TallGrassBlock||b instanceof DoublePlantBlock||b instanceof MushroomBlock||b instanceof DeadBushBlock||b instanceof WaterlilyBlock||b instanceof VineBlock||b instanceof ButtonBlock||b instanceof PinkPetalsBlock);}return item instanceof BoatItem||item instanceof BedItem||stack.is(ItemTags.DOORS);}
 public static boolean fuelAllowed(ItemStack stack,int capacity,boolean acceptsLarge){return !stack.isEmpty()&&LegacyFurnace.fuelFits(ThermalFurnace.Entity.heat(stack),capacity,large(stack),acceptsLarge);}
 static boolean emptySpongeBucket(ItemStack stack) {
  return stack.is(Items.BUCKET)
      || stack.getItem() instanceof SpecialItems.MetalBucket bucket && bucket.fluid(stack).isEmpty();
 }

 static void captureSpongeWater(Container furnace, ItemStack input, Plan plan) {
  if (!input.is(Items.WET_SPONGE) || !plan.output().is(Items.SPONGE)) return;
  ItemStack bucket = furnace.getItem(1);
  if (bucket.getCount() != 1 || !emptySpongeBucket(bucket)) return;

  ItemStack filled;
  if (bucket.is(Items.BUCKET)) {
   filled = new ItemStack(Items.WATER_BUCKET);
   if (bucket.hasTag()) filled.setTag(bucket.getTag().copy());
  } else {
   filled = bucket.copy();
   filled.getOrCreateTag().putString("Fluid", "water");
  }
  furnace.setItem(1, filled);
 }

 public static boolean sand(ItemStack stack){return stack.is(Items.SAND)||stack.is(Items.RED_SAND);}
 public static boolean sandstone(ItemStack s){return s.is(Items.SANDSTONE)||s.is(Items.CUT_SANDSTONE)||s.is(Items.CHISELED_SANDSTONE)||s.is(Items.RED_SANDSTONE)||s.is(Items.CUT_RED_SANDSTONE)||s.is(Items.CHISELED_RED_SANDSTONE)||s.getItem() instanceof SourceSacredSandstone.Item;}
 private static Plan plan(Item item,int count,int heat,int input){return new Plan(new ItemStack(item,count),heat,input,0);}
 public static Plan recipe(Level level,ItemStack input,int heat){
  var ordinary=ordinaryRecipe(level,input,heat);
  return WorldContentMode.rebuilt(level)?ScorchedFood2072.atHeat(input,heat,ordinary):ordinary;
 }
 private static Plan ordinaryRecipe(Level level,ItemStack input,int heat){
  if(input.isEmpty()||LegacyMetalOres.hidden(input)||!WorldContentEvents.itemActionAllowed(level,input))return NONE;
  if(sand(input)){var result=LegacyFurnace.sand(input.getCount(),heat);return result==LegacyFurnace.SandResult.NONE?NONE:plan(result==LegacyFurnace.SandResult.GLASS?Items.GLASS:input.is(Items.RED_SAND)?Items.RED_SANDSTONE:Items.SANDSTONE,1,result==LegacyFurnace.SandResult.GLASS?2:1,4);}
  if(sandstone(input))return plan(Items.GLASS,1,2,1);
  if(input.is(Items.CLAY_BALL))return plan(Items.BRICK,1,1,1);
  if(input.is(Items.CLAY))return plan(Items.TERRACOTTA,1,1,1);
  if(input.is(Items.CACTUS))return plan(Items.GREEN_DYE,1,1,1);
  if(input.is(ItemTags.LOGS_THAT_BURN))return plan(Items.CHARCOAL,1,1,1);
  if(input.is(Items.NETHERRACK))return plan(Items.NETHER_BRICK,1,1,1);
  if(input.is(Items.COAL_ORE)||input.is(Items.DEEPSLATE_COAL_ORE))return plan(Items.COAL,1,1,1);
  if(input.is(Items.REDSTONE_ORE)||input.is(Items.DEEPSLATE_REDSTONE_ORE))return plan(Items.REDSTONE,4,2,1);
  if(input.is(Items.LAPIS_ORE)||input.is(Items.DEEPSLATE_LAPIS_ORE))return plan(Items.LAPIS_LAZULI,4,2,1);
  if(input.is(Items.DIAMOND_ORE)||input.is(Items.DEEPSLATE_DIAMOND_ORE))return plan(Items.DIAMOND,1,2,1);
  if(input.is(Items.EMERALD_ORE)||input.is(Items.DEEPSLATE_EMERALD_ORE))return plan(Items.EMERALD,1,2,1);
  if(input.is(Items.NETHER_QUARTZ_ORE))return plan(Items.QUARTZ,1,2,1);
  if(WorldContentMode.ite(level)){
   if(input.is(Items.COAL_BLOCK))return plan(IteContent0416.DIAMOND_CHUNK.get(),1,5,1);
   if(input.is(IteContent0416.NETHER_ADAMANTIUM_ORE.get().asItem()))return plan(Content.get("adamantium_ingot"),1,4,1);
   if(input.is(Items.IRON_BLOCK))return plan(Content.get("mithril_ingot"),1,4,1);
   if(input.is(Content.get("mithril_block")))return plan(Content.get("adamantium_ingot"),1,4,1);
  }
  var inventory=new SimpleContainer(input);var recipe=level.getRecipeManager().getRecipeFor(Content.THERMAL.get(),inventory,level);
  if(recipe.isPresent()){var r=recipe.get();return new Plan(r.output().copy(),r.heat(),1,r.experience());}
  var food=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,inventory,level);if(input.getItem().isEdible()&&food.isPresent()){var r=food.get();return new Plan(r.getResultItem(level.registryAccess()).copy(),1,1,r.getExperience());}return NONE;
 }
 public static boolean inputAllowed(Level level,ItemStack stack,boolean acceptsLarge){if(stack.isEmpty()||!acceptsLarge&&large(stack)||!WorldContentEvents.itemActionAllowed(level,stack))return false;if(sand(stack))return true;return recipe(level,stack,4).exists();}
 /** Commit the entire recipe batch only when the output slot can hold it. */
 public static boolean fits(ItemStack existing, ItemStack result, int containerMax) {
  if (result.isEmpty() || (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, result))) return false;
  int limit = Math.min(containerMax, result.getMaxStackSize());
  if (existing.isEmpty()) return result.getCount() <= limit;
  return result.getCount() <= Math.min(limit, existing.getMaxStackSize()) - existing.getCount();
 }
 public static BlockPos mouth(BlockPos pos,BlockState furnace){return pos.relative(furnace.getValue(BlockStateProperties.HORIZONTAL_FACING));}
 public static boolean flooded(Level level,BlockPos pos,BlockState furnace){return level.getFluidState(mouth(pos,furnace)).is(FluidTags.WATER);}
 public static boolean smothered(Level level,BlockPos pos,BlockState furnace){var front=mouth(pos,furnace);var face=furnace.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();return Block.isFaceFull(level.getBlockState(front).getCollisionShape(level,front),face);}
 public static boolean openFront(Level level,BlockPos pos,BlockState furnace){var front=mouth(pos,furnace);return level.getBlockState(front).getCollisionShape(level,front).isEmpty();}
 /** yc.setXPReward plus xl subtype4; vq awards this only for quantities taken by a player. */
 public static int reward(ItemStack stack){if(!ClassicToolCatalogue.sourceItem(stack))return 0;var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());return switch(key.getPath()){case "diamond"->30;case "iron_ingot","copper_ingot","quartz"->10;case "gold_ingot","ancient_metal_ingot","emerald"->20;case "silver_ingot"->15;case "mithril_ingot"->40;case "adamantium_ingot"->100;case "holy_steel_ingot"->400;case "lapis_lazuli"->5;default->0;};}
 public static int cookTicks(Level level,ItemStack input,int heat){
  if(WorldContentMode.ite(level)&&heat>=4){
   var cfg=IteConfig.effective(level);
   if(input.is(Items.IRON_BLOCK))return Math.max(200,cfg.ironToMithrilCookTime());
   if(input.is(Content.get("mithril_block")))return Math.max(200,cfg.mithrilToAdamantiumCookTime());
  }
  return LegacyFurnace.COOK_TICKS;
 }
 private ClassicFurnaces(){}
}
