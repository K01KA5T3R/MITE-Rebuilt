package dev.koikaster.mite;
/** Native chunk clock; the completion-save marker is deliberately transient. */
public interface SourceChunkClock {
 long mite$randomTickTime();void mite$randomTickTime(long value);
 boolean mite$saveAfterForward();void mite$saveAfterForward(boolean value);
}
