package dev.koikaster.mite;
import java.util.*;import net.minecraft.core.BlockPos;import net.minecraft.tags.BlockTags;import net.minecraft.world.entity.Entity;import net.minecraft.world.entity.animal.*;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.storage.loot.LootContext;import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
/** BlockPlant/ane/aqv/BlockBush drop cause. Scope is exact level, cell and state,
 * so a player's fall cannot be mistaken for harvesting in native loot context. */
public final class SourcePlantTrampling619 {
 private record Scope(Level level,BlockPos pos,BlockState state){}
 private static final ThreadLocal<ArrayDeque<Scope>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
 public static boolean trampled(LootContext context,BlockState state){var stack=ACTIVE.get();if(stack.isEmpty()){ACTIVE.remove();return false;}var at=stack.peek();var origin=context.getParamOrNull(LootContextParams.ORIGIN);return context.getLevel()==at.level&&origin!=null&&BlockPos.containing(origin).equals(at.pos)&&state.equals(at.state);}
 public static void trample(Level level,BlockPos pos,Entity entity){
  var state=level.getBlockState(pos);var block=state.getBlock();if(level.isClientSide||!(block instanceof BushBlock))return;
  int kind=block instanceof SourceBlueberryBush?3:block instanceof TallGrassBlock||block instanceof DoublePlantBlock&&!(block instanceof TallFlowerBlock)?2:block instanceof FlowerBlock||state.is(BlockTags.FLOWERS)?1:0;
  boolean drop=SourceFarmFall619.drops(kind,entity instanceof Cow,entity instanceof Pig,entity instanceof Sheep);
  var stack=ACTIVE.get();stack.push(new Scope(level,pos.immutable(),state));try{level.destroyBlock(pos,drop,entity);}finally{stack.pop();if(stack.isEmpty())ACTIVE.remove();}
 }
 private SourcePlantTrampling619(){}
}
