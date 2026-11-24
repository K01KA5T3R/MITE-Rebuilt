package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
/** Compatibility surface while source callbacks own soil physics. Generic
 * neighbour notifications must not collapse every natural overhang. */
public final class SoilGravity {
 public static boolean affected(BlockState s){return s.is(Blocks.DIRT)||s.is(Blocks.COARSE_DIRT)||s.is(Blocks.SOUL_SAND);}
 public static boolean ordinary(BlockState state){return state.is(Blocks.SAND)||state.is(Blocks.RED_SAND)||state.is(Blocks.GRAVEL)||state.is(Content.NETHER_GRAVEL.get())||state.is(Content.CRUSHED_GRAVEL.get());}
 public static boolean falls(BlockState state){return affected(state)||ordinary(state);}
 public static void enqueue(ServerLevel level,BlockPos pos){}
 public static void tick(MinecraftServer server){}
 private SoilGravity(){}
}
