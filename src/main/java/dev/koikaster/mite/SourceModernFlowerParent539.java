package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
/** V001: retained1.7 flowers share the R195 plant root; tall halves retain native pairing. */
public final class SourceModernFlowerParent539 {
 static boolean small(Block b){return b!=null&&(b==Blocks.AZURE_BLUET||b==Blocks.RED_TULIP||b==Blocks.WHITE_TULIP);}
 static boolean tall(Block b){return b!=null&&(b==Blocks.SUNFLOWER||b==Blocks.LILAC||b==Blocks.ROSE_BUSH||b==Blocks.PEONY);}
 static boolean managed(Block b){return small(b)||tall(b);}
 public static boolean survives(boolean original,Object rawState,Object rawLevel,Object rawPos){
  var state=(BlockState)rawState;if(!tall(state.getBlock()))return original;
  boolean upper=state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER;
  return (!upper||original)&&SourceFlora.site((LevelReader)rawLevel,upper?((BlockPos)rawPos).below():(BlockPos)rawPos);
 }
 private SourceModernFlowerParent539(){}
}
