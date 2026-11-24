package dev.koikaster.mite;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** R195 leaves adapted to modern species, biome tags and native loot/random-tick entry points. */
public final class ClassicLeaves {
 public static boolean supported(BlockState state){var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());return state.getBlock() instanceof LeavesBlock&&id!=null&&id.getNamespace().equals("minecraft");}
 private static boolean swamp(ServerLevel level,BlockPos pos){var b=level.getBiome(pos);return b.is(Biomes.SWAMP)||b.is(Biomes.MANGROVE_SWAMP);}
 private static LegacyLeaves.Drop fruit(BlockState state,ServerLevel level,BlockPos pos){if(state.is(Blocks.JUNGLE_LEAVES))return LegacyLeaves.Drop.BANANA;if(state.is(Blocks.BIRCH_LEAVES))return LegacyLeaves.Drop.APPLE;if(state.is(Blocks.OAK_LEAVES)||state.is(Blocks.DARK_OAK_LEAVES))return level.getBiome(pos).is(BiomeTags.IS_JUNGLE)?LegacyLeaves.Drop.ORANGE:LegacyLeaves.Drop.APPLE;return LegacyLeaves.Drop.NONE;}
 private static Item sapling(BlockState s){if(s.is(Blocks.OAK_LEAVES))return Items.OAK_SAPLING;if(s.is(Blocks.SPRUCE_LEAVES))return Items.SPRUCE_SAPLING;if(s.is(Blocks.BIRCH_LEAVES))return Items.BIRCH_SAPLING;if(s.is(Blocks.JUNGLE_LEAVES))return Items.JUNGLE_SAPLING;if(s.is(Blocks.ACACIA_LEAVES))return Items.ACACIA_SAPLING;if(s.is(Blocks.DARK_OAK_LEAVES))return Items.DARK_OAK_SAPLING;if(s.is(Blocks.MANGROVE_LEAVES))return Items.MANGROVE_PROPAGULE;if(s.is(Blocks.CHERRY_LEAVES))return Items.CHERRY_SAPLING;if(s.is(Blocks.FLOWERING_AZALEA_LEAVES))return Items.FLOWERING_AZALEA;return Items.AZALEA;}
 private static Item item(LegacyLeaves.Drop drop,BlockState state){return switch(drop){case NONE->Items.AIR;case SAPLING->sapling(state);case STICK->Items.STICK;case APPLE->Items.APPLE;case ORANGE->Content.ORANGE.get();case BANANA->Content.BANANA.get();};}
 private static Item result(LegacyLeaves.Drop drop,BlockState state,RandomSource random,ServerLevel level){
  Item fallback=item(drop,state);
  if(!WorldContentMode.rebuilt(level))return fallback;
  return switch(drop){case APPLE,ORANGE,BANANA->FruitForaging.leaves(state,random,fallback);default->fallback;};
 }
 public static void apply(ObjectArrayList<ItemStack> loot,LootContext context,BlockState state){
  if(!supported(state))return;loot.clear();var tool=context.getParamOrNull(LootContextParams.TOOL);if(tool==null)tool=ItemStack.EMPTY;
  // Original silk dispatch bypasses aoz's placed-leaf resource suppression.
  if(tool.getItem() instanceof ShearsItem||EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0){loot.add(new ItemStack(state.getBlock()));return;}
  var origin=context.getParamOrNull(LootContextParams.ORIGIN);if(origin==null)return;var pos=BlockPos.containing(origin);var level=context.getLevel();var random=context.getRandom();
  var drop=LegacyLeaves.broken(state.getValue(LeavesBlock.PERSISTENT),swamp(level,pos),fruit(state,level,pos),random::nextInt,()->random.nextFloat());
  if(drop!=LegacyLeaves.Drop.NONE)loot.add(new ItemStack(result(drop,state,random,level)));
 }
 // Called while BlockState's tick flag is cached during bootstrap: do not consult registries here.
 public static boolean randomTicks(Object rawState){return !((BlockState)rawState).getValue(LeavesBlock.PERSISTENT);}
 public static void randomTick(Object rawState,Object rawLevel,Object rawPos,Object rawRandom){
  var state=(BlockState)rawState;if(!supported(state)||state.getValue(LeavesBlock.PERSISTENT)||state.getValue(LeavesBlock.DISTANCE)==7)return;
  var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;var random=(RandomSource)rawRandom;
  var drop=LegacyLeaves.windfall(false,swamp(level,pos),fruit(state,level,pos),false,random::nextInt);
  if(drop!=LegacyLeaves.Drop.NONE&&level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)){
   var entity=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(result(drop,state,random,level)));entity.setDeltaMovement(Vec3.ZERO);entity.setDefaultPickUpDelay();level.addFreshEntity(entity);
  }
 }
 private ClassicLeaves(){}
}
