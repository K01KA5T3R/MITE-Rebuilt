package dev.koikaster.mite;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 fuel values for the mapped catalogue. Modern-only fallback and unfinished furnace production rules remain explicit. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicFuel {
 public record Fuel(int ticks,int heat,String source){}
 private static Fuel source(LegacyFuel.Kind kind){return new Fuel(LegacyFuel.duration(kind),LegacyFuel.heat(kind),"R195 "+kind);}
 public static Fuel resolve(ItemStack stack,int modernTicks){
  if(stack.isEmpty())return source(LegacyFuel.Kind.NONE);
  if(!ClassicToolCatalogue.sourceItem(stack))return new Fuel(modernTicks,modernTicks>0?1:0,"other mod fuel");
  var item=stack.getItem();var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);String path=id.getPath();
  if(stack.is(Items.COAL))return source(LegacyFuel.Kind.COAL);
  if(stack.is(Items.CHARCOAL))return source(LegacyFuel.Kind.CHARCOAL);
  if(stack.is(Items.COAL_BLOCK))return source(LegacyFuel.Kind.COAL_BLOCK);
  if(stack.is(Items.LAVA_BUCKET)||item instanceof SpecialItems.MetalBucket bucket&&bucket.fluid(stack).equals("lava"))return source(LegacyFuel.Kind.LAVA_BUCKET);
  if(item instanceof SpecialItems.MetalBucket)return source(LegacyFuel.Kind.NONE);
  if(stack.is(Items.BLAZE_ROD))return source(LegacyFuel.Kind.BLAZE_ROD);
  if(stack.is(Items.PAPER))return source(LegacyFuel.Kind.PAPER);
  if(stack.is(Content.MANURE.get()))return source(LegacyFuel.Kind.MANURE);
  if(stack.is(Items.STICK)||item instanceof ArrowItem)return source(LegacyFuel.Kind.STICK_OR_ARROW);
  if(stack.is(Items.BOOK)||stack.is(Items.WRITABLE_BOOK)||item instanceof WrittenBookItem||stack.is(Items.ENCHANTED_BOOK))return source(LegacyFuel.Kind.BOOK);
  if(stack.is(Items.MAP)||stack.is(Items.FILLED_MAP))return source(LegacyFuel.Kind.OTHER_PAPER);
  if(stack.is(Items.BOWL))return source(LegacyFuel.Kind.EMPTY_BOWL);
  if(item instanceof SpecialItems.BowlMeal||OriginalBowls.water(stack)||stack.is(Items.MUSHROOM_STEW)||stack.is(Items.RABBIT_STEW)||stack.is(Items.BEETROOT_SOUP)||stack.is(Items.SUSPICIOUS_STEW))return source(LegacyFuel.Kind.FILLED_BOWL);
  if(item instanceof BoatItem)return source(LegacyFuel.Kind.BOAT);
  var profile=ClassicToolWear.profile(stack);
  if(profile!=null)return source(profile.kind()==LegacyToolWear.Kind.SHEARS?LegacyFuel.Kind.HANDLELESS_TOOL:profile.material().equals("wood")?LegacyFuel.Kind.WOOD_TOOL:LegacyFuel.Kind.HANDLED_TOOL);
  if(item instanceof BlockItem&&(path.startsWith("crimson_")||path.startsWith("warped_")))return source(LegacyFuel.Kind.NONE); // explicit modern noncombustible wood mapping
  if(SourceCarrotSticks.handles(stack)||item instanceof BowItem||item instanceof FishingRodItem||item instanceof SignItem||item instanceof HangingSignItem||item instanceof BedItem||stack.is(Items.CHEST_MINECART)||stack.is(Items.PAINTING)||stack.is(Items.ITEM_FRAME)||stack.is(Items.GLOW_ITEM_FRAME)||stack.is(Items.CARROT_ON_A_STICK)||stack.is(Items.WARPED_FUNGUS_ON_A_STICK))return source(LegacyFuel.Kind.WOOD_ITEM);
  if(stack.is(ItemTags.WOODEN_DOORS))return source(LegacyFuel.Kind.WOOD_DOOR);
  if(item instanceof BlockItem bi){var block=bi.getBlock();
   if(stack.is(ItemTags.LOGS_THAT_BURN))return source(LegacyFuel.Kind.LOG);
   if(stack.is(ItemTags.PLANKS)||stack.is(ItemTags.WOODEN_BUTTONS))return source(LegacyFuel.Kind.PLANK_OR_DOUBLE_SLAB_OR_BUTTON);
   if(stack.is(ItemTags.WOODEN_SLABS)||stack.is(ItemTags.SAPLINGS)||block==Blocks.DEAD_BUSH)return source(LegacyFuel.Kind.SAPLING_OR_SLAB_OR_DEAD_BUSH);
   if(block instanceof TorchBlock)return source(LegacyFuel.Kind.TORCH);
   if(stack.is(ItemTags.WOOL)||stack.is(ItemTags.WOOL_CARPETS)||stack.is(Items.SPONGE)||stack.is(Items.WET_SPONGE)||block==Blocks.VINE||stack.is(ItemTags.LEAVES))return source(LegacyFuel.Kind.NONE);
   if(stack.is(ItemTags.WOODEN_STAIRS)||stack.is(ItemTags.WOODEN_FENCES)||stack.is(ItemTags.WOODEN_TRAPDOORS)||stack.is(ItemTags.WOODEN_PRESSURE_PLATES)||stack.is(ItemTags.FENCE_GATES)||block==Blocks.CHEST||block==Blocks.TRAPPED_CHEST||block==Blocks.CRAFTING_TABLE||block instanceof R196Workbench||block==Blocks.JUKEBOX||block==Blocks.NOTE_BLOCK||block==Blocks.BOOKSHELF||block==Blocks.LADDER||block==Blocks.DAYLIGHT_DETECTOR||block==Blocks.BARREL||block==Blocks.LOOM||block==Blocks.LECTERN||block==Blocks.CARTOGRAPHY_TABLE||block==Blocks.FLETCHING_TABLE||block==Blocks.SMITHING_TABLE||block==Blocks.COMPOSTER||block==Blocks.BEEHIVE||block==Blocks.BEE_NEST||block==Blocks.CHISELED_BOOKSHELF||block==Blocks.MANGROVE_ROOTS)return source(LegacyFuel.Kind.WOOD_BLOCK);
  }
  if(stack.is(IteContent0416.BLAZE_COAL_POWDER.get()))return new Fuel(6800,5,"ITE blaze_coal_powder");
  if(stack.is(Content.BLAZING_COAL.get()))return source(LegacyFuel.Kind.NONE); // no original counterpart or human-selected fuel exception
  return new Fuel(modernTicks,modernTicks>0?1:0,"unmapped original or modern-only item; pending catalogue audit");
 }
 @SubscribeEvent public static void burn(FurnaceFuelBurnTimeEvent e){e.setBurnTime(resolve(e.getItemStack(),e.getBurnTime()).ticks());}
 private ClassicFuel(){}
}
