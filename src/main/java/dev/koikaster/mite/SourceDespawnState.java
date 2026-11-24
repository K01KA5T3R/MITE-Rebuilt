package dev.koikaster.mite;

/** Injected entity field bridge; separate from vanilla idle/AI timers and not a player setting. */
public interface SourceDespawnState {
 int mite$getDespawnCounter();
 void mite$setDespawnCounter(int value);
}
