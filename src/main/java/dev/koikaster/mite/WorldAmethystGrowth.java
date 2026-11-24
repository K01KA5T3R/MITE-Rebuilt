package dev.koikaster.mite;

import net.minecraft.world.level.Level;

/** Keep the original single random draw; only extras selects the slower bound. */
public final class WorldAmethystGrowth {
    public static int chanceBound(Level level) {
        return WorldContentMode.rebuilt(level) ? 100 : 5;
    }
    private WorldAmethystGrowth() {}
}
