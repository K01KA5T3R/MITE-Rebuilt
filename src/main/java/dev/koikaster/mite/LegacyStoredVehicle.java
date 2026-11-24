package dev.koikaster.mite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Marks the exact entity being restored from the player's vanilla RootVehicle record. */
public final class LegacyStoredVehicle {
    private static final ThreadLocal<Entity> RESTORING = new ThreadLocal<>();

    public static boolean restore(ServerLevel level, Entity entity) {
        Entity previous = RESTORING.get();
        RESTORING.set(entity);
        try {
            return level.addWithUUID(entity);
        } finally {
            if (previous == null) RESTORING.remove();
            else RESTORING.set(previous);
        }
    }

    static boolean restoring(Entity entity) {
        return RESTORING.get() == entity;
    }

    private LegacyStoredVehicle() {}
}
