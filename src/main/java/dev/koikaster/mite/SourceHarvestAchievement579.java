package dev.koikaster.mite;
import java.util.*;import net.minecraft.core.*;import net.minecraft.server.level.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.entity.BlockEntity;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.storage.loot.LootContext;import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
/** Original aoo achievement point inside actual jw player harvest, before spawning its result. */
public final class SourceHarvestAchievement579 {
 private record Frame(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState state){}
 private static final ThreadLocal<Deque<Frame>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
 public static void harvest(Block block,Level level,Player player,BlockPos pos,BlockState state,BlockEntity entity,ItemStack tool){
  if(!(level instanceof ServerLevel server)||!(player instanceof ServerPlayer actor)){block.playerDestroy(level,player,pos,state,entity,tool);return;}
  var frames=ACTIVE.get();var frame=new Frame(server,actor,pos.immutable(),state);frames.push(frame);
  try{block.playerDestroy(level,player,pos,state,entity,tool);}finally{if(frames.pop()!=frame)throw new IllegalStateException("Player harvest scope");if(frames.isEmpty())ACTIVE.remove();}
 }
 public static void rolled(LootContext context,BlockState state,LegacyGravel.Drop drop){
  if(drop!=LegacyGravel.Drop.FLINT&&drop!=LegacyGravel.Drop.FLINT_SHARD)return;var frames=ACTIVE.get();var frame=frames.peek();if(frame==null){ACTIVE.remove();return;}
  var origin=context.getParamOrNull(LootContextParams.ORIGIN);if(origin==null||context.getLevel()!=frame.level||context.getParamOrNull(LootContextParams.THIS_ENTITY)!=frame.player||!BlockPos.containing(origin).equals(frame.pos)||state.getBlock()!=frame.state.getBlock())return;
  SourceAchievementService570.grant(frame.player,"flintFinder");
 }
 /** removeBlock's actual result is independent of destroyBlock's unconditional final true. */
 public static boolean removed(boolean result,ServerLevel level,ServerPlayer player,BlockPos pos){if(result&&level.dimension()==WorldEvents.UNDERWORLD&&pos.getY()<6)for(var direction:Direction.values())if(level.getBlockState(pos.relative(direction)).getBlock() instanceof SourceMantleBlock){SourceAchievementService570.grant(player,"portalToNether");break;}return result;}

 /** Only the final loot list within the committed player harvest may change a standing. */
 public static void scoreHarvest(List<ItemStack> drops, BlockState state, Level level, BlockPos pos,
                                 net.minecraft.world.entity.Entity entity, ItemStack tool) {
  if (!(entity instanceof ServerPlayer player) || drops.isEmpty() || !SourceTournamentStanding.active(player)
      || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) return;
  var frames=ACTIVE.get();
  var frame=frames.peek();
  if(frame==null){ACTIVE.remove();return;}
  if(level!=frame.level || player!=frame.player || !pos.equals(frame.pos)
      || state.getBlock()!=frame.state.getBlock()) return;
  if (ClassicGravel.isGravel(state)) {
   for (var drop : drops) {
    var nugget = switch (net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(drop.getItem()).toString()) {
     case "mite_reforged:copper_nugget" -> SourceTournamentStanding.Harvest.COPPER_NUGGET;
     case "mite_reforged:silver_nugget" -> SourceTournamentStanding.Harvest.SILVER_NUGGET;
     case "minecraft:gold_nugget" -> SourceTournamentStanding.Harvest.GOLD_NUGGET;
     case "mite_reforged:mithril_nugget" -> SourceTournamentStanding.Harvest.MITHRIL_NUGGET;
     case "mite_reforged:adamantium_nugget" -> SourceTournamentStanding.Harvest.ADAMANTIUM_NUGGET;
     default -> null;
    };
    if (nugget != null) {
     SourceTournamentStanding.harvest(player, nugget);
     return;
    }
   }
   return;
  }
  if (!(state.getBlock() instanceof SourceTournamentOre) || state.getValue(SourceTournamentOre.PLACED)
      || drops.stream().noneMatch(drop -> drop.is(state.getBlock().asItem()))) return;
  if (SourceSkills1685.enabled(level) && !SourceSkills1685.has(player, SourceSkillRules1685.MINING)
      && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
          net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, tool) == 0) return;
  var ore = switch (net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString()) {
   case "minecraft:copper_ore" -> SourceTournamentStanding.Harvest.COPPER_ORE;
   case "mite_reforged:silver_ore" -> SourceTournamentStanding.Harvest.SILVER_ORE;
   case "minecraft:gold_ore" -> SourceTournamentStanding.Harvest.GOLD_ORE;
   case "minecraft:iron_ore" -> SourceTournamentStanding.Harvest.IRON_ORE;
   case "mite_reforged:mithril_ore" -> SourceTournamentStanding.Harvest.MITHRIL_ORE;
   case "mite_reforged:adamantium_ore" -> SourceTournamentStanding.Harvest.ADAMANTIUM_ORE;
   default -> null;
  };
  if (ore != null) SourceTournamentStanding.harvest(player, ore);
 }
 private SourceHarvestAchievement579(){}
}
