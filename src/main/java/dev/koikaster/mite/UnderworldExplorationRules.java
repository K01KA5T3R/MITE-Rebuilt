package dev.koikaster.mite;

import net.minecraft.server.level.ServerLevel;

final class UnderworldExplorationRules {
    static final int ORIGINAL_DUNGEON_ATTEMPTS = 16;
    static final int EXTRA_DUNGEON_ATTEMPTS = 12;
    static final int CORRIDORS_PER_EXTRA_SPAWNER = 36;
    static final int MAX_EXTRA_SPAWNERS = 6;
    static final int MIN_SPAWNER_HORIZONTAL_GAP = 12;

    static boolean enabled(ServerLevel level) {
        return level.dimension().equals(WorldEvents.UNDERWORLD) && WorldContentMode.rebuilt(level);
    }

    static int spawnerBudget(int corridorCount) {
        return Math.min(MAX_EXTRA_SPAWNERS,
            (corridorCount + CORRIDORS_PER_EXTRA_SPAWNER / 2) / CORRIDORS_PER_EXTRA_SPAWNER);
    }

    private UnderworldExplorationRules() {}
}
