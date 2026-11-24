package dev.koikaster.mite;
import java.util.*;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
/** Runs the source outer pass once,before all admitted normal chunk ticks. */
public final class SourceFastForwardBatch {
 public static void before(Object rawCache,Object rawRecords){var cache=(ServerChunkCache)rawCache;var gate=(SourceFastForwardNativeAccess.Cache)rawCache;var chunks=new ArrayList<LevelChunk>();for(var record:(List<?>)rawRecords){var chunk=((SourceFastForwardNativeAccess.Pair)record).mite$fastForwardChunk();if(gate.mite$fastForwardEligible(chunk))chunks.add(chunk);}SourceFastForwardWorld.run(cache.level,chunks);((SourcePendingTickAccess)(Object)cache.level).mite$runPendingBlockUpdates();}
 private SourceFastForwardBatch(){}
}
