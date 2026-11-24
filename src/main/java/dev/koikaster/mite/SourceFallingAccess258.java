package dev.koikaster.mite;
import net.minecraft.world.level.block.state.BlockState;
/** Typed access to the actual native state/cancellation fields. */
public interface SourceFallingAccess258 {
 void mite$fallingState258(BlockState state);
 boolean mite$fallingCancelled258();
 int mite$fallingFlags258();
 void mite$fallingFlags258(int flags);
}
