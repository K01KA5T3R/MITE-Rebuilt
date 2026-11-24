package dev.koikaster.mite;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraftforge.registries.ForgeRegistries;

/** The native block progress entry point, including blocks with modern hardness zero. */
public final class ClassicMining {
 private static final Map<String,Float> HARDNESS=new HashMap<>();
 static{try(var in=ClassicMining.class.getResourceAsStream("/data/mite_reforged/mining_hardness.json")){if(in==null)throw new IOException("missing mining hardness");var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();root.getAsJsonObject("hardness").entrySet().forEach(e->HARDNESS.put(e.getKey(),e.getValue().getAsFloat()));}catch(IOException error){throw new ExceptionInInitializerError(error);}}
 private static String miningKey(BlockState state){if(state.getBlock() instanceof R196Workbench)return "minecraft:crafting_table";var key=ForgeRegistries.BLOCKS.getKey(state.getBlock());return key==null?null:key.toString();}
 /** aqz.cG is fixed block hardness, independent of crop age or mining pose. */
 public static float sourceFixedHardness(BlockState state){Float original=OriginalMining1587.explosionResistance(state);if(original!=null)return original;if(state.is(Blocks.LILY_PAD))return .02f;var key=miningKey(state);var value=key==null?null:HARDNESS.get(key);return value==null?state.getBlock().defaultDestroyTime():value;}
 public static float hardness(BlockState state,BlockGetter level,BlockPos pos){
  Float gold=R196Gold.hardness(state,level);if(gold!=null)return gold;
  if(state.is(Blocks.SNOW))return sourceFixedHardness(state)*state.getValue(SnowLayerBlock.LAYERS);
  Float original=OriginalBlockPhysics1566.hardness(state);if(original!=null)return original;
  if(state.is(Blocks.LILY_PAD))return .02f;
  if(state.getBlock() instanceof InfestedBlock||state.getBlock() instanceof SourceInfestation.CopperStone)return 0;
  if(state.getBlock().defaultDestroyTime()<0)return -1;
  if(state.getBlock() instanceof CropBlock crop&&(Agriculture.supports(state)||state.getBlock() instanceof Agriculture.DeadCrop)){
   int age=crop.getAge(state);return age==0?0f:.02f*(state.getBlock() instanceof Agriculture.DeadCrop?1:crop.isMaxAge(state)?2:1);
  }
  if(state.is(BlockTags.LOGS))return 1.2f;if(state.is(BlockTags.PLANKS)||state.is(BlockTags.WOODEN_STAIRS))return 1.2f*2f/3f;
  if(state.is(BlockTags.WOODEN_SLABS))return state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE?.8f:.4f;
  var key=miningKey(state);var value=key==null?null:HARDNESS.get(key);if(value!=null){if(state.getBlock() instanceof SlabBlock&&state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE)return value*2f;return value;}
  return state.getDestroySpeed(level,pos);
 }
 private static boolean rootCrop(BlockState state){return state.is(Blocks.CARROTS)||state.is(Blocks.POTATOES)||state.is(Content.ONIONS.get());}
 private static boolean softSoil(BlockState state){return state.is(BlockTags.MINEABLE_WITH_SHOVEL)&&!state.is(Blocks.CLAY);}
 public static float baseStrength(Player p,BlockState state){
  ItemStack held=p.getMainHandItem();
  ItemStack reference=CrabulonContent.mithrilReference(held);
  float strength=referenceStrength(p,LargeBonePickaxe.miningReference(reference),state);
  return reference!=held&&strength>1f?strength*CrabulonContent.PICKAXE_SPEED:strength;
 }
 private static float referenceStrength(Player player,ItemStack tool,BlockState state){
  Float original=OriginalMining1587.playerStrength(tool,state);
  if(original!=null)return original;
  float strength=toolStrength(tool,state,HarvestRules.canHarvest(player,state));
  return strength>0?strength:HarvestRules.requiredLevel(state)==0?1f:0f;
 }
 /** xj returns0 for a wrong tool; the player's empty-hand fallback is deliberately separate. */
 public static float npcToolStrength(ItemStack stack,BlockState state){return toolStrength(stack,state,LegacyHarvest.allows(HarvestRules.toolKind(stack),HarvestRules.harvestLevel(stack),HarvestRules.family(state),HarvestRules.requiredLevel(state),false));}
 private static float toolStrength(ItemStack stack,BlockState state,boolean mayHarvest){Float original=OriginalMining1587.strength(stack,state);if(original!=null)return original;
  var item=stack.getItem();var kind=HarvestRules.toolKind(stack);var key=ForgeRegistries.ITEMS.getKey(item);String name=key==null?"":key.getPath();boolean effective=false;float base=4;
  boolean clayGlassIce=state.is(HarvestRules.AXE_OR_PICKAXE)||state.is(Blocks.CLAY)||state.getBlock() instanceof AbstractGlassBlock||state.getBlock() instanceof StainedGlassPaneBlock||state.is(Blocks.GLASS_PANE)||state.is(Blocks.GLOWSTONE)||state.is(Blocks.SEA_LANTERN)||state.is(Blocks.ICE)||state.is(Blocks.PACKED_ICE)||state.is(Blocks.BLUE_ICE);
  if(item instanceof ShearsItem){base=4;effective=ClassicToolTraits.shearEffective(state);}
  else if(name.endsWith("_cudgel")||name.endsWith("_club")){base=2;effective=ClassicToolTraits.cudgelEffective(state);}
  else if(kind==LegacyHarvest.Tool.PICKAXE){effective=state.is(BlockTags.MINEABLE_WITH_PICKAXE)||clayGlassIce||state.is(Blocks.CAKE);if(name.endsWith("_warhammer")){base=3;effective|=state.is(Blocks.PUMPKIN)||state.is(Blocks.CARVED_PUMPKIN)||state.is(Blocks.MELON);}}
  else if(kind==LegacyHarvest.Tool.AXE){effective=state.is(BlockTags.MINEABLE_WITH_AXE)||clayGlassIce||state.is(HarvestRules.SANDSTONE_SLABS)||state.is(Blocks.CACTUS)||state.is(Blocks.SUGAR_CANE);if(name.endsWith("_hatchet"))base=2;if(name.endsWith("_battleaxe"))base*=.75f;if(state.is(Blocks.SANDSTONE)||state.is(Blocks.RED_SANDSTONE))base*=.5f;}
  else if(kind==LegacyHarvest.Tool.SHOVEL||kind==LegacyHarvest.Tool.MATTOCK){effective=state.is(BlockTags.MINEABLE_WITH_SHOVEL)||rootCrop(state)||state.is(Blocks.CAKE);if(kind==LegacyHarvest.Tool.MATTOCK)base=3;}
  else if(kind==LegacyHarvest.Tool.HOE){base=2;effective=softSoil(state)||rootCrop(state)||state.is(Blocks.CAKE);if(name.endsWith("_scythe")){base=4;effective=state.is(Blocks.WHEAT)||state.is(Blocks.GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.LARGE_FERN);}}
  else if(kind==LegacyHarvest.Tool.SWORD){base=2;effective=state.is(BlockTags.WOOL)||state.is(BlockTags.WOOL_CARPETS)||state.getBlock() instanceof LeavesBlock||state.getBlock() instanceof BushBlock||state.getBlock() instanceof VineBlock||state.is(Blocks.COBWEB)||state.is(Blocks.PUMPKIN)||state.is(Blocks.CARVED_PUMPKIN)||state.is(Blocks.MELON);}
  if(state.is(Blocks.REDSTONE_LAMP))effective=false;
  if(effective&&mayHarvest){
   String material=HarvestRules.materialName(stack);
   // Copper's slower deep-rock mining includes the ores in that host rock.
   var blockId=ForgeRegistries.BLOCKS.getKey(state.getBlock());
   boolean deepRock=state.is(Blocks.DEEPSLATE)||blockId!=null&&blockId.getPath().startsWith("deepslate_")&&blockId.getPath().endsWith("_ore");
   float deepslate=deepRock&&kind==LegacyHarvest.Tool.PICKAXE&&material.equals("copper")?.75f:1f;
   return base*LegacyMining.materialEfficiency(material)*deepslate;
  }
  return 0;
 }
 public static float destroyProgress(Object rawState,Object rawPlayer,Object rawLevel,Object rawPos){
  var state=(BlockState)rawState;var player=(Player)rawPlayer;var level=(BlockGetter)rawLevel;var pos=(BlockPos)rawPos;
  if(player.isCreative()||!HarvestRules.strict(player))return -1;
  boolean portable=state.getBlock() instanceof SourceMetalBuildings.Strongbox?SourceMetalBuildings.portable(state,player.level(),pos,player):HarvestRules.portable(state);
  if(!portable&&!HarvestRules.canHarvest(player,state))return 0;
  var stack=player.getMainHandItem();var reference=CrabulonContent.mithrilReference(stack);
  float base=referenceStrength(player,LargeBonePickaxe.miningReference(reference),state);
  // Wrong tools use the empty-hand fallback and do not confer an efficiency enchantment.
  int efficiency=base>1?EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY,stack):0;
  int haste=player.hasEffect(MobEffects.DIG_SPEED)?player.getEffect(MobEffects.DIG_SPEED).getAmplifier():-1;
  int fatigue=player.hasEffect(MobEffects.DIG_SLOWDOWN)?player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier():-1;
  float progress=LegacyMining.damage(hardness(state,level,pos),base,portable,efficiency,PlayerState.level(player),haste,fatigue,player.isEyeInFluid(FluidTags.WATER),EnchantmentHelper.hasAquaAffinity(player),player.onGround(),SurvivalFood.hasFoodEnergy(player));
  return reference!=stack&&!portable&&base>1f?Math.min(1f,progress*CrabulonContent.PICKAXE_SPEED):progress;
 }
 private ClassicMining(){}
}
