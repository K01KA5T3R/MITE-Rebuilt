package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

public final class ChunkHolderUpdates {
    private ChunkHolderUpdates() {}

    public static <T> void apply(Set<T> pending, Consumer<? super T> update) {
        var batch = new ArrayList<>(pending);
        // Completing a chunk future can reenter the distance manager and enqueue more work.
        pending.clear();
        int index = 0;
        try {
            for (; index < batch.size(); index++) {
                update.accept(batch.get(index));
            }
        } finally {
            for (; index < batch.size(); index++) {
                pending.add(batch.get(index));
            }
        }
    }
}