package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** R195 ahq's three stronghold candidates, adapted to the active biome source. */
public final class ClassicStrongholds {
    public static CompletableFuture<List<ChunkPos>> positions(ChunkGeneratorStructureState state,
                                                               Holder<StructureSet> structureSet, BiomeSource source) {
        if (!WorldContentMode.classicTerrain(state) || !structureSet.is(BuiltinStructureSets.STRONGHOLDS)) return null;
        if (source instanceof ClassicOverworldBiomeSource classic) {
            return CompletableFuture.completedFuture(OriginalStrongholdBiomeSearch.rings(classic.layers(), state.getLevelSeed()));
        }
        var random = RandomSource.create(state.getLevelSeed());
        double angle = random.nextDouble() * Math.PI * 2.0;
        var positions = new ArrayList<ChunkPos>(3);
        for (int index = 0; index < 3; index++) {
            double radius = (1.25 + random.nextDouble()) * 512.0;
            int x = (int) Math.round(Math.cos(angle) * radius);
            int z = (int) Math.round(Math.sin(angle) * radius);
            var match = source.findBiomeHorizontal((x << 4) + 8, 64, (z << 4) + 8, 112,
                ClassicStrongholds::suitableBiome, random, state.randomState().sampler());
            positions.add(match == null ? new ChunkPos(x, z) : new ChunkPos(match.getFirst()));
            angle += Math.PI * 2.0 / 3.0;
        }
        return CompletableFuture.completedFuture(List.copyOf(positions));
    }

    private static boolean suitableBiome(Holder<Biome> biome) {
        return biome.is(Biomes.DESERT) || biome.is(Biomes.FOREST) || biome.is(Biomes.SWAMP)
            || biome.is(Biomes.TAIGA) || biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.SNOWY_SLOPES)
            || biome.is(Biomes.WINDSWEPT_HILLS) || biome.is(Biomes.WINDSWEPT_FOREST)
            || biome.is(Biomes.JUNGLE) || biome.is(Biomes.SPARSE_JUNGLE);
    }

    private ClassicStrongholds() {}
}
