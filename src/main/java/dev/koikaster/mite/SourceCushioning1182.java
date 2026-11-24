package dev.koikaster.mite;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
/** R195 constructor cushioning values and arf's metadata-dependent snow depth. */
public final class SourceCushioning1182 {
 public static float get(Block block,int metadata){
  if(block instanceof SnowLayerBlock)return .2f*(float)((metadata&7)+1);
  if(block==Blocks.SNOW_BLOCK||block instanceof LeavesBlock||block.defaultBlockState().is(BlockTags.WOOL))return .8f;
  if(block==Blocks.SPONGE||block==Blocks.WET_SPONGE)return 1f;
  if(block instanceof BedBlock)return .6f;
  if(block instanceof SandBlock||block instanceof FarmBlock||block instanceof HayBlock||block instanceof SoulSandBlock||block instanceof HugeMushroomBlock)return .4f;
  if(block==Blocks.DIRT||block==Blocks.GRASS_BLOCK||block instanceof CropBlock||block instanceof SaplingBlock||block instanceof DeadBushBlock||block instanceof TallGrassBlock||block instanceof SourceWitherwoodContent1120.Bush)return .2f;
  return 0f;
 }
 private SourceCushioning1182(){}
}
