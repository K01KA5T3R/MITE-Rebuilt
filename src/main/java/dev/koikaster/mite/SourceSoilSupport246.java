package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** aqz/arf/aog/amy/aqk support and replacement semantics. Being non-solid
 * permits an initial fall, but does not imply that the occupying block may
 * be crushed: R195 rails explicitly prohibit that separate operation. */
public final class SourceSoilSupport246 {
 public static boolean solid(BlockState state){
  var block=state.getBlock();
  if(state.isAir()||block instanceof LiquidBlock||block instanceof BubbleColumnBlock)return false;
  if(block instanceof FenceGateBlock)return !state.getValue(FenceGateBlock.OPEN);
  if(block instanceof SnowLayerBlock)return state.getValue(SnowLayerBlock.LAYERS)>1;
  if(block instanceof WaterlilyBlock||block instanceof DoorBlock||block instanceof TrapDoorBlock||block instanceof LadderBlock||block instanceof DiodeBlock||block instanceof FlowerPotBlock||block instanceof AbstractSkullBlock)return true;
  if(block instanceof BushBlock||block instanceof BaseRailBlock||block instanceof WebBlock||block instanceof TorchBlock||block instanceof BaseFireBlock||block instanceof RedStoneWireBlock||block instanceof SignBlock||block instanceof LeverBlock||block instanceof BasePressurePlateBlock||block instanceof ButtonBlock||block instanceof SugarCaneBlock||block instanceof StemBlock||block instanceof AttachedStemBlock||block instanceof VineBlock||block instanceof NetherWartBlock||block instanceof NetherPortalBlock||block instanceof EndPortalBlock||block instanceof TripWireBlock||block instanceof TripWireHookBlock||block instanceof CarpetBlock)return false;
  return state.isSolid();
 }
 public static boolean dislodged(BlockState occupied,BlockState falling,BlockGetter level,BlockPos pos){
  if(occupied.isAir())return false;
  if(occupied.getBlock() instanceof BaseRailBlock)return false;
  if(occupied.getBlock() instanceof FlowerPotBlock)return false;
  if(occupied.getBlock() instanceof AbstractSkullBlock)return true;
  if(!solid(occupied))return true;
  if(occupied.is(falling.getBlock()))return false;
  // arf permits solids to replace only a one-layer snow state; the above
  // non-solid branch already handles that state for dirt/soul sand.
  if(occupied.getBlock() instanceof SnowLayerBlock)return false;
  return occupied.getBlock() instanceof LiquidBlock||ClassicMining.hardness(occupied,level,pos)==0;
 }
 public static boolean canFallBelow(BlockState occupied,BlockState falling,BlockGetter level,BlockPos pos){return occupied.isAir()||!solid(occupied)||dislodged(occupied,falling,level,pos);}
 public static boolean glass(BlockState state){return state.getBlock() instanceof AbstractGlassBlock||state.getBlock() instanceof StainedGlassPaneBlock||state.is(Blocks.GLASS_PANE)||state.is(Blocks.GLOWSTONE)||state.is(Blocks.BEACON);}
 private SourceSoilSupport246(){}
}
