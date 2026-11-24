package dev.koikaster.mite;

/** Native scheduled callbacks run once,after historical catch-up closes. */
public interface SourcePendingTickAccess {
 void mite$runPendingBlockUpdates();
}
