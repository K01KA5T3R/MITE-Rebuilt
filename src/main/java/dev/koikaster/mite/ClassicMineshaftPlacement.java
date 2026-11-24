package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/** R195 aer, ain, and agg location lottery. Native structure pieces are used after a valid start. */
final class ClassicMineshaftPlacement extends RandomSpreadStructurePlacement {
    static final Codec<ClassicMineshaftPlacement> SERIALIZER = Codec.unit(ClassicMineshaftPlacement::new);

    ClassicMineshaftPlacement() {
        super(1, 0, RandomSpreadType.LINEAR, 0);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return candidate(state.getLevelSeed(), x, z);
    }

    static boolean candidate(long seed, int x, int z) {
        var random = new Random(seed);
        long saltX = random.nextLong();
        long saltZ = random.nextLong();
        random.setSeed(seed ^ x * saltX ^ z * saltZ);
        random.nextInt();
        return random.nextFloat() < .005f && random.nextInt(80) < Math.max(Math.abs(x), Math.abs(z));
    }

    static boolean allowsBiome(ClassicOverworldBiomeSource biomes, int x, int z) {
        int original = biomes.layers().blocks(x, z, 1, 1)[0];
        return switch (original) {
            case OriginalOverworldBiomes.EXTREME_HILLS, OriginalOverworldBiomes.ICE_MOUNTAINS,
                OriginalOverworldBiomes.DESERT_HILLS, OriginalOverworldBiomes.FOREST_HILLS,
                OriginalOverworldBiomes.TAIGA_HILLS, OriginalOverworldBiomes.MOUNTAIN_EDGE,
                OriginalOverworldBiomes.JUNGLE_HILLS -> true;
            default -> false;
        };
    }

    @Override
    public StructurePlacementType<?> type() {
        return ClassicTerrainAdapters.MINESHAFT_PLACEMENT.get();
    }
}
