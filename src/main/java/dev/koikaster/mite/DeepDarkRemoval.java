package dev.koikaster.mite;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Keeps native keys readable while removing their Deep Dark gameplay and world generation. */
public final class DeepDarkRemoval {
    private static final Set<Block> BLOCKS = Set.of(Blocks.SCULK, Blocks.SCULK_VEIN, Blocks.SCULK_CATALYST,
        Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.REINFORCED_DEEPSLATE);
    private static final Set<String> BLOCK_ENTITIES = Set.of("sculk_catalyst", "sculk_sensor", "calibrated_sculk_sensor", "sculk_shrieker");
    private static final int SURFACE_QUART = QuartPos.fromBlock(64);

    static boolean replaces(Block block) {
        return BLOCKS.contains(block);
    }

    public static Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> climate(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
        return row -> {
            if (!row.getSecond().equals(Biomes.DEEP_DARK)) output.accept(row);
        };
    }

    public static BiomeResolver forGeneration(ChunkAccess chunk, BiomeResolver original) {
        return (x, y, z, sampler) -> {
            var chosen = original.getNoiseBiome(x, y, z, sampler);
            if (!chosen.is(Biomes.DEEP_DARK)) return chosen;
            if (y != SURFACE_QUART) {
                var surface = original.getNoiseBiome(x, SURFACE_QUART, z, sampler);
                if (!surface.is(Biomes.DEEP_DARK)) return surface;
            }
            return plains(chunk);
        };
    }

    static boolean migrateBiomes(ChunkAccess chunk) {
        var sections = chunk.getSections();
        if (Arrays.stream(sections).noneMatch(section -> section != null
                && section.getBiomes().maybeHas(holder -> holder.is(Biomes.DEEP_DARK)))) return false;

        var above = new ArrayList<Holder<Biome>>(Collections.nCopies(16, null));
        boolean changed = false;
        for (int sectionIndex = sections.length - 1; sectionIndex >= 0; sectionIndex--) {
            var section = sections[sectionIndex];
            if (section == null) continue;
            var migrated = new HashMap<Integer, Holder<Biome>>();
            boolean possiblyDark = section.getBiomes().maybeHas(holder -> holder.is(Biomes.DEEP_DARK));
            for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) {
                int column = cell(x, 0, z);
                if (!possiblyDark) {
                    above.set(column, section.getNoiseBiome(x, 0, z));
                    continue;
                }
                for (int y = 3; y >= 0; y--) {
                    var biome = section.getNoiseBiome(x, y, z);
                    if (biome.is(Biomes.DEEP_DARK)) {
                        var replacement = above.get(column);
                        if (replacement == null) {
                            replacement = plains(chunk);
                            above.set(column, replacement);
                        }
                        migrated.put(cell(x, y, z), replacement);
                    } else {
                        above.set(column, biome);
                    }
                }
            }
            if (migrated.isEmpty()) continue;
            section.fillBiomesFromNoise((x, y, z, sampler) -> {
                var biome = migrated.get(cell(x, y, z));
                return biome != null ? biome : section.getNoiseBiome(x, y, z);
            }, null, 0, 0, 0);
            changed = true;
        }
        return changed;
    }

    private static int cell(int x, int y, int z) {
        return x | z << 2 | y << 4;
    }

    private static Holder<Biome> plains(ChunkAccess chunk) {
        var level = chunk.getWorldForge();
        var registries = level != null ? level.registryAccess() : ServerLifecycleHooks.getCurrentServer().registryAccess();
        return registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
    }

    static boolean removeOldBlockEntities(ChunkAccess chunk, CompoundTag data) {
        var entries = data.getList("block_entities", Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int index = entries.size() - 1; index >= 0; index--) {
            var entry = entries.getCompound(index);
            var id = ResourceLocation.tryParse(entry.getString("id"));
            if (id == null || !id.getNamespace().equals("minecraft") || !BLOCK_ENTITIES.contains(id.getPath())) continue;
            var position = BlockEntity.getPosFromTag(entry);
            if (!new ChunkPos(position).equals(chunk.getPos()) || !chunk.getBlockState(position).is(Blocks.STONE)) continue;
            // Full chunks still share this exact list with their delayed post-load processor.
            entries.remove(index);
            chunk.removeBlockEntity(position);
            changed = true;
        }
        return changed;
    }

    private DeepDarkRemoval() {}
}
