package dev.koikaster.mite;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Native block loot entry point; the block identity, rather than dimension, selects the table. */
public final class ClassicGravel {
 public static boolean isGravel(BlockState state){return state.is(Blocks.GRAVEL)||state.is(Content.NETHER_GRAVEL.get());}
 public static void apply(ObjectArrayList<ItemStack> loot,LootContext context,BlockState state){
  // R195 sand material is not reduced by the base explosion path: gravel stays itself.
  // Preserve an empty list if another loot condition/mod has already suppressed the drop.
  if(loot.isEmpty()||context.hasParam(LootContextParams.EXPLOSION_RADIUS)||!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player))return;
  var tool=context.getParamOrNull(LootContextParams.TOOL);if(tool==null)tool=ItemStack.EMPTY;
  if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0)return;
  int fortune=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,tool);
  var drop=LegacyGravel.roll(fortune,state.is(Content.NETHER_GRAVEL.get()),context.getRandom()::nextInt);
  Item item=switch(drop){
   case GRAVEL->state.getBlock().asItem();case FLINT_SHARD->Content.FLINT_SHARD.get();case FLINT->Items.FLINT;
   case COPPER->Content.nugget("copper");case SILVER->Content.nugget("silver");case GOLD->Items.GOLD_NUGGET;
   case MITHRIL->Content.nugget("mithril");case ADAMANTIUM->Content.nugget("adamantium");
   case OBSIDIAN->Content.OBSIDIAN_SHARD.get();case EMERALD->Content.EMERALD_SHARD.get();case DIAMOND->Content.DIAMOND_SHARD.get();case QUARTZ->Content.QUARTZ_SHARD.get();
  };
  SourceHarvestAchievement579.rolled(context,state,drop);loot.clear();loot.add(new ItemStack(item));
 }
 private ClassicGravel(){}
}
