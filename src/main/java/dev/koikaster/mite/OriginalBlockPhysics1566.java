package dev.koikaster.mite;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import com.google.gson.*;import net.minecraft.tags.BlockTags;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.block.state.properties.SlabType;import net.minecraftforge.registries.ForgeRegistries;
/** Actual R195 registered block facts; unknown modern/third-party objects keep their adapters. */
public final class OriginalBlockPhysics1566 {
 private record Facts(float hardness,int level,String material,boolean restoreHardness,boolean restoreLevel){}
 private static final Map<String,Facts> DATA=load();
 private static Map<String,Facts> load(){try(var in=OriginalBlockPhysics1566.class.getResourceAsStream("/data/mite_reforged/original_block_physics1566.json")){if(in==null)throw new IOException("missing original block facts");var rows=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("blocks");var result=new HashMap<String,Facts>();for(var entry:rows.entrySet()){var v=entry.getValue().getAsJsonObject();result.put(entry.getKey(),new Facts(v.get("hardness").getAsFloat(),v.get("harvest_level").getAsInt(),v.get("material").getAsString(),v.get("restore_hardness").getAsBoolean(),v.get("restore_level").getAsBoolean()));}return Map.copyOf(result);}catch(IOException e){throw new ExceptionInInitializerError(e);}}
 private static Facts facts(BlockState s){var key=ForgeRegistries.BLOCKS.getKey(s.getBlock());if(key==null)return null;var exact=DATA.get(key.toString());if(exact!=null)return exact;if(s.getBlock() instanceof R196Workbench)return DATA.get("minecraft:crafting_table");if(!key.getNamespace().equals("minecraft"))return null;
  String counterpart=null;
  if(s.is(BlockTags.WOODEN_DOORS))counterpart="oak_door";else if(s.is(BlockTags.WOODEN_TRAPDOORS))counterpart="oak_trapdoor";else if(s.is(BlockTags.WOODEN_FENCES))counterpart="oak_fence";
  else if(s.getBlock() instanceof SaplingBlock)counterpart="oak_sapling";else if(s.getBlock() instanceof WallSignBlock)counterpart="oak_wall_sign";else if(s.getBlock() instanceof StandingSignBlock)counterpart="oak_sign";
  else if(s.getBlock() instanceof InfestedBlock)counterpart="infested_stone";else if(s.is(BlockTags.TERRACOTTA))counterpart="terracotta";
  else if(s.getBlock() instanceof AnvilBlock)counterpart="anvil";
  return counterpart==null?null:DATA.get("minecraft:"+counterpart);
 }
 public static Float hardness(BlockState s){var f=facts(s);if(f==null||!f.restoreHardness)return null;return s.getBlock() instanceof SlabBlock&&s.getValue(SlabBlock.TYPE)==SlabType.DOUBLE?f.hardness*2:f.hardness;}
 public static Integer harvestLevel(BlockState s){var f=facts(s);return f==null||!f.restoreLevel?null:f.level;}
 public static String material(BlockState s){if(s.getBlock() instanceof MetalAnvilBlock||s.getBlock() instanceof AnvilBlock)return "anvil";var f=facts(s);if(f!=null)return f.material;
  if(s.is(BlockTags.LOGS)||s.is(BlockTags.PLANKS)||s.is(BlockTags.WOODEN_STAIRS)||s.is(BlockTags.WOODEN_SLABS)||s.getBlock() instanceof FenceGateBlock)return "wood";
  if(s.is(BlockTags.WOOL))return "cloth";if(s.is(BlockTags.WOOL_CARPETS))return "carpet";if(s.getBlock() instanceof LeavesBlock)return "tree_leaves";if(s.getBlock() instanceof BedBlock)return "cloth";
  if(s.getBlock() instanceof AbstractGlassBlock||s.getBlock() instanceof StainedGlassPaneBlock)return "glass";
  if(s.getBlock() instanceof BushBlock)return "plants";
  var key=ForgeRegistries.BLOCKS.getKey(s.getBlock());if(key!=null&&key.getNamespace().equals("minecraft")&&(s.is(BlockTags.STONE_BRICKS)||s.getBlock() instanceof SlabBlock||s.getBlock() instanceof StairBlock||s.getBlock() instanceof WallBlock))return "stone";
  return null;
 }
 private OriginalBlockPhysics1566(){}
}
