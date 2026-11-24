package dev.koikaster.mite;

import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

/** Preserves encounters previously allowed by independent group settings. */
final class LegacyBossArchive {
    static boolean archiveIfInactive(Entity entity, ServerLevel level, WorldExtraSettings settings) {
        if (!RebuiltContent.wasStandaloneBoss(entity, settings)) return false;
        if (!entity.getType().canSerialize()) return true;
        var actor = new CompoundTag();
        if (!entity.saveAsPassenger(actor)) {
            throw new IllegalStateException("Unable to serialize disabled legacy boss " + entity.getUUID());
        }

        var snapshot = new CompoundTag();
        NbtUtils.addCurrentDataVersion(snapshot);
        snapshot.putString("Dimension", level.dimension().location().toString());
        snapshot.put("Entity", actor);
        var root = level.getServer().getWorldPath(LevelResource.ROOT);
        var file = DimensionType.getStorageFolder(level.dimension(), root)
            .resolve("data/mite-disabled-bosses").resolve(entity.getUUID() + ".dat");
        try {
            AtomicGraveFile207.write(file, snapshot);
        } catch (IOException error) {
            // Leave cancellation pending if the archive could not be committed.
            throw new UncheckedIOException("Unable to retain disabled legacy boss " + entity.getUUID(), error);
        }
        return true;
    }

    private LegacyBossArchive() {}
}
