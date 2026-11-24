package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

/** aee's saved historical random-tick time and adr's completion save request. */
public final class SourceFastForwardClock {
 private static final String KEY="MiteR195RandomTickTime",VERSION="MiteR195RandomTickVersion";
 private static SourceChunkClock clock(Object chunk){return (SourceChunkClock)chunk;}
 public static ProtoChunk loadTag(Object chunk,Object raw){var value=clock(chunk);var tag=(CompoundTag)raw;value.mite$randomTickTime(tag.getInt(VERSION)==2?Math.max(0,tag.getLong(KEY)):0);value.mite$saveAfterForward(false);return (ProtoChunk)chunk;}
 public static CompoundTag saveTag(Object raw,Object chunk){var tag=(CompoundTag)raw;tag.putInt(VERSION,2);tag.putLong(KEY,clock(chunk).mite$randomTickTime());return tag;}
 public static void promoted(Object chunk,Object proto){var value=clock(chunk);value.mite$randomTickTime(clock(proto).mite$randomTickTime());value.mite$saveAfterForward(false);}
 public static void normalTick(Object rawLevel,Object rawChunk){var level=(ServerLevel)rawLevel;var chunk=(ChunkAccess)rawChunk;if(!SourceDimensionClock.chunkAllowed(level))return;var value=clock(chunk);long now=level.getGameTime(),last=value.mite$randomTickTime();value.mite$randomTickTime(last==0?now:last+1);if(value.mite$saveAfterForward()&&now-value.mite$randomTickTime()<100)chunk.setUnsaved(true);}
 private SourceFastForwardClock(){}
}
