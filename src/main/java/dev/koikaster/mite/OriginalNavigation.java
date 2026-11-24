package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.*;
/** rf/alg wooden-portal role adapted to native pathfinding; ordinary fences and other species are unchanged. */
public final class OriginalNavigation {
 public static BlockPathTypes portalType(BlockPathTypes original,Object evaluator,Object level,int x,int y,int z){var node=(NodeEvaluator)evaluator;var mob=((SourcePathOwner)node).mite$pathMob();if(!(mob instanceof Mobs.Undead)||!node.canOpenDoors())return original;var block=((BlockGetter)level).getBlockState(new BlockPos(x,y,z));if((block.getBlock() instanceof FenceGateBlock||block.is(BlockTags.WOODEN_TRAPDOORS))&&!block.getValue(BlockStateProperties.OPEN))return BlockPathTypes.DOOR_WOOD_CLOSED;return original;}
 private OriginalNavigation(){}
}
