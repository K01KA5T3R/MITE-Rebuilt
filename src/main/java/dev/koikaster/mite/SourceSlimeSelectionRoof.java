package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**  js.getSuitableCreature's native-green ts branch rejects a
 * stone first non-air roof. Non-green gelatinous owners use the opposite roof
 * condition and retain their already accepted implementations.
 */
public final class SourceSlimeSelectionRoof {
 public static boolean accepts(ServerLevelAccessor level,BlockPos pos){
  for(int y=pos.getY()+1;y<level.getMaxBuildHeight();y++){
   var state=level.getBlockState(new BlockPos(pos.getX(),y,pos.getZ()));
   if(state.isAir())continue;
   // DEEPSLATE is the existing1.20 source-stone counterpart, not a new material rule.
   return !state.is(Blocks.STONE)&&!state.is(Blocks.DEEPSLATE);
  }
  return true;
 }
 private SourceSlimeSelectionRoof(){}
}
