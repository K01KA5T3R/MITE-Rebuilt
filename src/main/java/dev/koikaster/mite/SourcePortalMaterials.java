package dev.koikaster.mite;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** acj tests Material.isSolid (akc.a), not Block.isSolid or full-top support.
 * akc defaults true, including its web material akd. Only aka (air/fire),
 * akb (liquids), akf (portals), and ajz (plants/vines/circuits/carpet/snow)
 * return false. Modern plant/circuit counterparts keep those material roles.
 * In particular signs, pressure plates, open wooden gates, slabs, cake and
 * cobwebs must not inherit the native collision-size approximation. */
final class SourcePortalMaterials {
 static boolean solid(BlockState state){
  if(state.isAir())return false;Block block=state.getBlock();
  return !(block instanceof LiquidBlock||block instanceof BaseFireBlock
   ||block instanceof NetherPortalBlock||block instanceof EndPortalBlock
   ||block instanceof BushBlock||block instanceof VineBlock||block instanceof MultifaceBlock
   ||block instanceof SugarCaneBlock||block instanceof CocoaBlock||block instanceof GrowingPlantBlock
   ||block instanceof BambooStalkBlock||block instanceof ChorusPlantBlock||block instanceof ChorusFlowerBlock
   ||block instanceof BigDripleafBlock||block instanceof BigDripleafStemBlock||block instanceof SporeBlossomBlock
   ||block instanceof SnowLayerBlock||block instanceof PowderSnowBlock||block instanceof CarpetBlock
   ||block instanceof TorchBlock||block instanceof ButtonBlock||block instanceof LeverBlock
   ||block instanceof BaseRailBlock||block instanceof RedStoneWireBlock||block instanceof DiodeBlock
   ||block instanceof TripWireBlock||block instanceof TripWireHookBlock||block instanceof LadderBlock
   ||block instanceof FlowerPotBlock||block instanceof AbstractSkullBlock||block instanceof EndRodBlock
   ||block instanceof StructureVoidBlock||block instanceof LightBlock);
 }
 private SourcePortalMaterials(){}
}
