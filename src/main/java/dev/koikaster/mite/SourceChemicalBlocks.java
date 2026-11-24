package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.*;
/** aqz onContactWithPepsin/Acid, called by the new physical projectile. */
final class SourceChemicalBlocks {
 static boolean contact(Level level,BlockPos pos,Direction face,boolean acid,boolean showGradual){var state=level.getBlockState(pos);if(state.isAir())return false;
  if(acid&&(state.is(Blocks.DIRT)||state.is(Blocks.FARMLAND))){if(face==null||face==Direction.UP){level.setBlockAndUpdate(pos,Blocks.CLAY.defaultBlockState());particles(level,pos,true);return true;}return false;}
  int ticks=SourceChemicalMaterials.blockTicks(state,acid);if(ticks<0)return false;if(ticks==0||showGradual)particles(level,pos,acid&&ticks==0);return ticks==0&&removeNoDrop(level,pos);
 }
 static boolean contactCaptured(Level level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,Direction face,boolean acid,boolean showGradual){if(state.isAir())return false;
  if(acid&&(state.is(Blocks.DIRT)||state.is(Blocks.FARMLAND))){if(face==null||face==Direction.UP){level.setBlockAndUpdate(pos,Blocks.CLAY.defaultBlockState());particles(level,pos,true);return true;}return false;}
  int ticks=SourceChemicalMaterials.blockTicks(state,acid);if(ticks<0)return false;if(ticks==0||showGradual)particles(level,pos,acid&&ticks==0);return ticks==0&&removeNoDrop(level,pos);
 }
 static List<BlockPos> partners(Level level,BlockPos pos){var state=level.getBlockState(pos);var result=new ArrayList<BlockPos>();if(state.getBlock() instanceof DoorBlock){var other=state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER?pos.above():pos.below();if(level.getBlockState(other).is(state.getBlock()))result.add(other);}else if(state.getBlock() instanceof BedBlock){var facing=state.getValue(BedBlock.FACING);var other=pos.relative(state.getValue(BedBlock.PART)==BedPart.FOOT?facing:facing.getOpposite());if(level.getBlockState(other).is(state.getBlock()))result.add(other);}return result;}
 static boolean removeNoDrop(Level level,BlockPos pos){if(level.getBlockState(pos).isAir())return false;for(var other:partners(level,pos))level.setBlock(other,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);boolean changed=level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL|Block.UPDATE_SUPPRESS_DROPS);return changed||level.getBlockState(pos).isAir();}
 private static void particles(Level level,BlockPos pos,boolean smoke){if(level instanceof ServerLevel server)SourceChemicalFx.block(server,pos,smoke?SourceChemicalFx.BLOCK_SMOKE_STEAM:SourceChemicalFx.BLOCK_STEAM);}
 private SourceChemicalBlocks(){}
}
