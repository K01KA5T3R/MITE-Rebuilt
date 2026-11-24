package dev.koikaster.mite;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
/** Called by a frame-preserving coremod hook on both the server and client light engines. */
public final class LegacyOcclusion {
 public static int adjust(Object state,int vanilla){
  if(state instanceof BlockState block&&(block.getBlock() instanceof SlabBlock||block.getBlock() instanceof StairBlock))return 15;
  return vanilla;
 }
 private LegacyOcclusion(){}
}
