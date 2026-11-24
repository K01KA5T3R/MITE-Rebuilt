package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** BlockFalling parent entries for the exact retained original families. */
public final class SourceFallingParent285 {
 public static boolean tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(!SoilGravity.ordinary(state))return false;SourceSoilControl271.ordinaryFall(level,pos);return true;}
 public static boolean animate(BlockState state){return SoilGravity.ordinary(state);}
 public static boolean walked(BlockState state,Level level,BlockPos pos,Entity actor){if(!SoilGravity.ordinary(state))return false;if(level instanceof ServerLevel server)SourceSoilControl271.ordinaryFall(server,pos);return true;}
 public static boolean collided(BlockState state,Level level,BlockPos pos,Entity actor){return walked(state,level,pos,actor);}
 /** Native move calls stepOn unconditionally at ground contact. Source sr
  * opts out of ordinary walking and explicitly steps on support only after
  * a successful landing. Preserve that single source callback location. */
 public static void nativeMoveStep(Block block,Level level,BlockPos pos,BlockState state,Entity actor){if(actor instanceof FallingBlockEntity falling&&SourceFallingTransport258.eligible(falling.getBlockState()))return;block.stepOn(level,pos,state,actor);}
 private SourceFallingParent285(){}
}
