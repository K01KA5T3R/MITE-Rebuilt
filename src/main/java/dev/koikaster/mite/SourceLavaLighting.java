package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

// R195 lava emits 13. Older chunk light arrays are rebuilt by the native engine.
public final class SourceLavaLighting {
    // Keep the existing save key. Revision 2 invalidates both layers left over from block replacement.
    private static final String REVISION_KEY = "MiteBlockLightRevision";
    private static final int REVISION = 2;
    private static final String BLOCK_LIGHT = "BlockLight";
    private static final String SKY_LIGHT = "SkyLight";

    public static CompoundTag loadTag(Object raw) {
        var original = (CompoundTag) raw;
        if (original.getInt(REVISION_KEY) >= REVISION) return original;
        var sections = original.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.getCompound(i);
            if (section.contains(BLOCK_LIGHT, Tag.TAG_BYTE_ARRAY) || section.contains(SKY_LIGHT, Tag.TAG_BYTE_ARRAY)) {
                return withoutStoredLight(original);
            }
        }
        return original;
    }

    private static CompoundTag withoutStoredLight(CompoundTag original) {
        var copy = original.copy();
        copy.putBoolean("isLightOn", false);
        copy.putBoolean("shouldSave", true);
        var sections = copy.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.getCompound(i);
            if (section.contains(BLOCK_LIGHT, Tag.TAG_BYTE_ARRAY)) section.remove(BLOCK_LIGHT);
            if (section.contains(SKY_LIGHT, Tag.TAG_BYTE_ARRAY)) section.remove(SKY_LIGHT);
        }
        return copy;
    }

    public static CompoundTag saveTag(Object raw, Object rawChunk) {
        var tag = (CompoundTag)raw;
        var chunk = (ChunkAccess)rawChunk;
        if (chunk.getStatus().isOrAfter(ChunkStatus.LIGHT) && chunk.isLightCorrect())
            tag.putInt(REVISION_KEY, REVISION);
        else tag.remove(REVISION_KEY);
        return tag;
    }

    private SourceLavaLighting() {}
}
