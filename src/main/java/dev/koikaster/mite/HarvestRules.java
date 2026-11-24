package dev.koikaster.mite;
import net.minecraft.tags.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
public final class HarvestRules {
 public static final TagKey<Item> KNIVES=ItemTags.create(Content.id("knives"));
 public static final TagKey<Block> HAND_BREAKABLE=BlockTags.create(Content.id("hand_breakable")),AXE_OR_PICKAXE=BlockTags.create(Content.id("axe_or_pickaxe")),SANDSTONE_SLABS=BlockTags.create(Content.id("sandstone_slabs")),PORTABLE=BlockTags.create(Content.id("portable"));
 private static final java.util.List<TagKey<Block>> LEVELS=java.util.stream.IntStream.rangeClosed(0,6).mapToObj(i->BlockTags.create(Content.id("harvest_level_"+i))).toList();
 public static LegacyHarvest.Tool toolKind(ItemStack stack){
  if(stack.isEmpty())return LegacyHarvest.Tool.HAND;var item=stack.getItem();
  if(item instanceof Tools.Mattock)return LegacyHarvest.Tool.MATTOCK;
  if(item instanceof PickaxeItem)return LegacyHarvest.Tool.PICKAXE;if(item instanceof AxeItem)return LegacyHarvest.Tool.AXE;if(item instanceof ShovelItem)return LegacyHarvest.Tool.SHOVEL;if(item instanceof HoeItem)return LegacyHarvest.Tool.HOE;if(item instanceof SwordItem)return LegacyHarvest.Tool.SWORD;return LegacyHarvest.Tool.OTHER;
 }
 /** Harvest levels are the original material levels, separate from existing equipment balance tiers. */
 public static int harvestLevel(ItemStack stack){
  return stack.getItem() instanceof TieredItem?LegacyHarvest.material(materialName(stack)):0;
 }
 public static String materialName(ItemStack stack){if(LargeBonePickaxe.matches(stack))return "iron";if(CrabulonContent.fungalPickaxe(stack)||CrabulonContent.fungalSpear(stack))return "mithril";var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());if(key==null)return "wood";String path=key.getPath();if(path.equals("shears"))return "iron";if(path.startsWith("rusted_iron_"))return "rusted_iron";if(path.startsWith("ancient_metal_"))return "ancient_metal";if(path.startsWith("holy_steel_"))return "holy_steel";int split=path.indexOf('_');String material=split<0?path:path.substring(0,split);return material.equals("wooden")?"wood":material.equals("golden")?"gold":material;}
 public static boolean strict(Player p){return Settings.STRICT_HARVEST;}
 public static boolean portable(BlockState state){Boolean original=SourcePortable1602.portable(state);if(original!=null)return original;if(state.getBlock() instanceof SourceMetalBuildings.Strongbox)return false;if(state.is(Blocks.FURNACE)||state.is(Blocks.SMOKER)||state.is(Blocks.BLAST_FURNACE))return !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);if(state.getBlock() instanceof ThermalFurnace.BlockImpl)return !state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT);return state.is(PORTABLE)||state.getBlock() instanceof MetalAnvilBlock||state.hasBlockEntity()&&!state.is(Blocks.SPAWNER)&&state.getBlock().defaultDestroyTime()>=0;}
 public static int requiredLevel(BlockState state){
  if(state.getBlock() instanceof IteContent0416.NetherAdamantiumOreBlock)return 4;
  if(state.is(IteContent0416.VIBRANIUM_BLOCK.get()))return 5;
  if(state.getBlock() instanceof IteContent0416.ForgingTableBlock)return 3;
  if(state.getBlock() instanceof ThermalFurnace.BlockImpl furnace)return furnace.capacity>=5?5:furnace.capacity==3?3:furnace.capacity>=2?2:0;
  Integer original=OriginalBlockPhysics1566.harvestLevel(state);if(original!=null)return original;
  // Latest human override: natural deepslate is accessible with a copper pick.
  if(state.is(Blocks.DEEPSLATE))return 2;
  // The host rock changes mining time, not the ore's material requirement.
  // Both stone and deepslate variants already share the original harvest tags.
  if(state.is(HAND_BREAKABLE))return 0;
  for(int i=6;i>=1;i--)if(state.is(LEVELS.get(i)))return i;
  if(state.is(BlockTags.LOGS))return 1;
  if(state.is(BlockTags.NEEDS_DIAMOND_TOOL))return 4;if(state.is(BlockTags.NEEDS_IRON_TOOL))return 3;
  return state.is(BlockTags.MINEABLE_WITH_PICKAXE)?2:0;
 }
 public static LegacyHarvest.Family family(BlockState state){
  if(state.is(BlockTags.LOGS))return LegacyHarvest.Family.LOG;if(state.is(SANDSTONE_SLABS))return LegacyHarvest.Family.SANDSTONE_SLAB;if(state.is(AXE_OR_PICKAXE))return LegacyHarvest.Family.AXE_OR_PICKAXE;return requiredLevel(state)>0?LegacyHarvest.Family.PICKAXE:LegacyHarvest.Family.FREE;
 }
 public static int toolLevel(ItemStack s){
  if(s.isEmpty())return 0;
  if(CrabulonContent.fungalPickaxe(s)||CrabulonContent.fungalSpear(s))return Content.metalLevel("mithril");
  for(int i=6;i>=1;i--)if(s.is(ItemTags.create(Content.id("tier_"+i))))return i;
  var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());if(key==null)return 0;
  if(key.getNamespace().equals(MiteReforged.ID)){String n=key.getPath();if(n.startsWith("holy_steel_")||n.startsWith("vibranium_"))return 6;if(n.startsWith("netherite_"))return 4;for(String m:Content.METALS)if(n.startsWith(m+"_"))return Content.metalLevel(m);return 0;}
  if(!key.getNamespace().equals("minecraft"))return 0;
  String n=key.getPath();if(n.startsWith("netherite_"))return 4;if(n.startsWith("diamond_"))return 3;if(n.startsWith("iron_"))return 2;if(n.startsWith("golden_"))return 1;return 0;
 }
 public static boolean canHarvest(Player p,BlockState b,net.minecraft.world.level.BlockGetter world,net.minecraft.core.BlockPos pos){return b.getBlock() instanceof SourceMetalBuildings.Strongbox&&world.getBlockEntity(pos) instanceof SourceMetalBuildings.BoxEntity box&&box.ownedBy(p)||canHarvest(p,b);}
 public static boolean canHarvest(Player p,BlockState b){
  if(p.isCreative()||!strict(p))return true;
  if(b.getBlock().defaultDestroyTime()<0)return false;
  Boolean original=OriginalMining1587.canHarvest(LargeBonePickaxe.miningReference(CrabulonContent.mithrilReference(p.getMainHandItem())),b);if(original!=null)return portable(b)||original;
  var s=p.getMainHandItem();return LegacyHarvest.allows(toolKind(s),harvestLevel(s),family(b),requiredLevel(b),portable(b));
 }
 private HarvestRules(){}
}
