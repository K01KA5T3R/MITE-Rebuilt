package dev.koikaster.mite;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.JsonParser;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** akc/y c material susceptibility. Tools and arrows include their wooden
 * component; material immunity of an effective metal is not whole-item immunity.
 * Block classes below explicitly adapt original families to modern counterparts. */
public final class SourceChemicalMaterials {
 private static final Set<String> PEPSIN_FOOD=pepsinFoods();
 private static Set<String> pepsinFoods(){try(var in=SourceChemicalMaterials.class.getResourceAsStream("/data/mite_reforged/source_chemical_food_materials_039.json")){var root=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();var result=new HashSet<String>();for(var row:root.getAsJsonObject("mapped_foods").entrySet())if(row.getValue().getAsJsonObject().get("pepsin").getAsBoolean())result.add(row.getKey());return Set.copyOf(result);}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 static String blockName(BlockState state){return ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath();}
 static boolean protectedMaterial(String material){return !SourceChemicalCatalogue.material(material,true);}
 public static boolean materialAcid(BlockState state){
  var block=state.getBlock();if(block instanceof MetalAnvilBlock anvil)return !protectedMaterial(anvil.material);
  var exact=SourceChemicalCatalogue.entry("blocks",ForgeRegistries.BLOCKS.getKey(block).toString(),true);if(exact!=null)return exact;
  if(block instanceof SnowLayerBlock||block instanceof PowderSnowBlock||block instanceof AbstractGlassBlock||block instanceof StainedGlassPaneBlock)return false;
  if(block instanceof BasePressurePlateBlock){if(state.is(BlockTags.WOODEN_PRESSURE_PLATES))return true;if(block instanceof WeightedPressurePlateBlock)return !state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);return false;}
  return true; // akc default; unlisted newer objects do not gain immunity from their names.
 }
 public static boolean item(ItemStack stack,boolean acid){
  if(stack.isEmpty())return false;
  var object=stack.getItem();String key=String.valueOf(ForgeRegistries.ITEMS.getKey(object));
  var armor=OriginalArmorItems.profile(stack);
  if(armor!=null)return acid?!protectedMaterial(armor.material()):armor.material().equals("leather");
  var tool=ClassicToolWear.profile(stack);
  if(tool!=null)return acid&&(tool.kind()!=LegacyToolWear.Kind.SHEARS||!protectedMaterial(tool.material()));
  if(SourceCarrotSticks.handles(stack)||object instanceof ArrowItem||object instanceof BowItem||object instanceof FishingRodItem)return acid;
  // Source vessels combine body and contents; water/lava alone do not imply acid susceptibility.
  if(ClassicVessels.handled(stack)){String fluid=ClassicVessels.fluid(stack);return SourceChemicalCatalogue.material(ClassicVessels.material(stack),acid)||!fluid.isEmpty()&&SourceChemicalCatalogue.material(fluid,acid);}
  if(object instanceof OriginalArmorItems.HorseArmor horse)return SourceChemicalCatalogue.material(horse.material,acid);
  var exact=SourceChemicalCatalogue.entry("items",key,acid);if(exact!=null)return exact;
  if(object.isEdible())return acid||PEPSIN_FOOD.contains(key)||OriginalMeatItems.includes(stack);
  if(object instanceof BlockItem block)return acid?materialAcid(block.getBlock().defaultBlockState()):pepsinBlock(block.getBlock().defaultBlockState());
  return acid||PEPSIN_FOOD.contains(key)||OriginalMeatItems.includes(stack);
 }
 static boolean pepsinBlock(BlockState state){return state.is(BlockTags.WOOL)||state.is(BlockTags.WOOL_CARPETS)||state.getBlock() instanceof CakeBlock||state.getBlock() instanceof BedBlock;}
 public static int blockTicks(BlockState state,boolean acid){return OriginalBlockDissolvePolicy.ticks(new BlockFacts(state),acid);}
 private record BlockFacts(BlockState state) implements OriginalBlockDissolvePolicy.Facts {
  private Block block(){return state.getBlock();}
  public boolean tripwire(){return block() instanceof TripWireBlock;}
  public boolean materialHarmedByPepsin(){return pepsinBlock(state);}
  public boolean materialHarmedByAcid(){return materialAcid(state);}
  public boolean cloth(){return state.is(BlockTags.WOOL)||block() instanceof BedBlock;}
  public boolean carpet(){return state.is(BlockTags.WOOL_CARPETS);}
  public boolean cake(){return block() instanceof CakeBlock;}
  public boolean earlyMaterialGatedFamily(){var b=block();String name=blockName(state);return b instanceof DoorBlock||b instanceof ChestBlock||b instanceof BarrelBlock||b instanceof BasePressurePlateBlock||b instanceof IronBarsBlock||Set.of("iron_block","gold_block","diamond_block","emerald_block","lapis_block","redstone_block","coal_block","copper_block","exposed_copper","weathered_copper","oxidized_copper","netherite_block","mithril_block","adamantium_block","ancient_metal_block","silver_block","bronze_block","steel_block").contains(name);}
  public boolean anvil(){return block() instanceof AnvilBlock||block() instanceof MetalAnvilBlock;}
  public boolean anvilMetalHarmedByAcid(){return materialAcid(state);}
  public boolean alwaysGradualAcidFamily(){var b=block();return b instanceof BaseRailBlock||b instanceof ButtonBlock&&state.is(BlockTags.WOODEN_BUTTONS)||b instanceof LeverBlock||b instanceof SignBlock||b instanceof LadderBlock||b instanceof BedBlock||b instanceof PistonBaseBlock||b instanceof PistonHeadBlock||b instanceof MovingPistonBlock||b instanceof CactusBlock||b instanceof MelonBlock||b instanceof PumpkinBlock||b instanceof DiodeBlock||b instanceof TrapDoorBlock||b instanceof EnchantmentTableBlock||b instanceof AbstractSkullBlock||b instanceof DaylightDetectorBlock||b instanceof HopperBlock||b instanceof HayBlock;}
  public boolean acidProtectedCircuitFamily(){return block() instanceof RedStoneWireBlock||block() instanceof ButtonBlock&&!state.is(BlockTags.WOODEN_BUTTONS);}
  public boolean gradualFenceGateCauldronCocoaFamily(){return block() instanceof FenceBlock||block() instanceof FenceGateBlock||block() instanceof AbstractCauldronBlock||block() instanceof CocoaBlock;}
  public boolean lilyOrLeaves(){return block() instanceof WaterlilyBlock||block() instanceof LeavesBlock;}
  public boolean sourceSolidAtMetadataZero(){if(block() instanceof SnowLayerBlock)return false;if(block() instanceof FenceGateBlock)return true;return SourcePhaseNativeWorld.solid(state);}
  public boolean tripwireHook(){return block() instanceof TripWireHookBlock;}
 }
 private SourceChemicalMaterials(){}
}
