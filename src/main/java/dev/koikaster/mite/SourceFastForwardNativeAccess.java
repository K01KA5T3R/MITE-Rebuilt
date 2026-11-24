package dev.koikaster.mite;
import net.minecraft.world.level.chunk.LevelChunk;
/** Narrow native reads used by the historical-tick transport. */
public final class SourceFastForwardNativeAccess {
 public interface Cache {boolean mite$fastForwardEligible(LevelChunk chunk);}
 public interface Pair {LevelChunk mite$fastForwardChunk();}
 public interface Sample {int mite$nextBlockSample();}
 private SourceFastForwardNativeAccess(){}
}
