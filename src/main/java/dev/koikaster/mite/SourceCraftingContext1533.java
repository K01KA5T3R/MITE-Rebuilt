package dev.koikaster.mite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** uf.isUpperBodyInWeb: query the actual upper bounding volume, not a saved contact flag. */
public final class SourceCraftingContext1533 {
 public static boolean upperBodyInWeb(Player player){
  if(player==null)return false;var box=player.getBoundingBox();double minY=player.getY()+1;
  if(box.maxY<=minY)return false;
  return player.level().getBlockStates(new AABB(box.minX,minY,box.minZ,box.maxX,box.maxY,box.maxZ)).anyMatch(state->state.is(Blocks.COBWEB));
 }
 private SourceCraftingContext1533(){}
}
