package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
/** Explicit V054/V072/V096/V101 consumers missed by the existing crop event.
 * Existing crop growth, legacy flora, coral and native growth/fill bodies stay
 * in their owners. All new admission gates reuse fixed Settings.GROWTH. */
public final class ModernPlantProduction434 {
 public static boolean limited(Object block){return block instanceof BambooStalkBlock||block instanceof BambooSaplingBlock||block instanceof SweetBerryBushBlock||block instanceof KelpBlock||block instanceof CaveVinesBlock||block instanceof CaveVinesPlantBlock||block instanceof SeagrassBlock||block instanceof MossBlock||block instanceof AzaleaBlock;}
 public static boolean allow(Object block, Level level, RandomSource random) {
  return !WorldContentMode.rebuilt(level) || !limited(block) || random.nextDouble() <= Settings.GROWTH;
 }
 public static boolean waterPlant(Level level, BlockPos pos, BlockState state, int flags) {
  if (state.is(Blocks.SEAGRASS) && !allow(Blocks.SEAGRASS, level, level.getRandom())) return false;
  return level.setBlock(pos, state, flags);
 }
 public static void scheduleDrip(ServerLevel level, BlockPos pos, Block block, int delay) {
  if (!WorldContentMode.rebuilt(level) || level.getRandom().nextDouble() <= Settings.GROWTH) {
   level.scheduleTick(pos, block, delay);
  }
 }
 private ModernPlantProduction434(){}
}
