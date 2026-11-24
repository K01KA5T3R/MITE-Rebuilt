package dev.koikaster.mite.visual;

import dev.koikaster.mite.SourceMoonWeather;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Add only already-initialized weather metadata to an outgoing packet copy. */
public final class ClassicFogMetadata {
    public static void append(CompoundTag data, ServerLevel overworld) {
        var creation = SourceMoonWeather.visualCreationTime(overworld);
        if (creation.isPresent()) data.putLong("ClassicFogCreation", creation.getAsLong());
    }

    private ClassicFogMetadata() {}
}
