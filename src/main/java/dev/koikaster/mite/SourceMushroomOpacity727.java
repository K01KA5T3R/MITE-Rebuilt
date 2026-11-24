package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

/** R195 aqz.t exceptions; later families retain their existing native fallback. */
public final class SourceMushroomOpacity727 {
 public static boolean opaque(BlockState state,BlockGetter level){
  var block=state.getBlock();
  if(block instanceof SoulSandBlock)return true;
  if(block instanceof PistonBaseBlock||block instanceof SnowLayerBlock)return false;
  return state.isSolidRender(level,BlockPos.ZERO)&&state.isCollisionShapeFullBlock(level,BlockPos.ZERO);
 }
 private SourceMushroomOpacity727(){}
}
