package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;

/** R195 ahh grid. Each structure checks its own active biome at the source candidate. */
public final class ClassicTemplePlacement extends RandomSpreadStructurePlacement {
    static final Codec<ClassicTemplePlacement> SERIALIZER = Codec.unit(ClassicTemplePlacement::new);
    private static final int SPACING = 40, MIN_SEPARATION = 20, SALT = 14357617;

    ClassicTemplePlacement() {
        super(SPACING, MIN_SEPARATION, RandomSpreadType.LINEAR, SALT);
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, SPACING);
        int cellZ = Math.floorDiv(z, SPACING);
        var random = new Random(cellX * 341873128712L + cellZ * 132897987541L + seed + SALT);
        return new ChunkPos(cellX * SPACING + random.nextInt(SPACING - MIN_SEPARATION),
            cellZ * SPACING + random.nextInt(SPACING - MIN_SEPARATION));
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        var candidate = getPotentialStructureChunk(state.getLevelSeed(), x, z);
        return candidate.x == x && candidate.z == z;
    }

    public static boolean allows(net.minecraft.world.level.chunk.ChunkGenerator generator,
                                 net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry structure,
                                 ChunkPos chunk) {
        return !(generator instanceof ClassicOverworldChunkGenerator classic)
            || classic.permitsStructure(structure.structure(), chunk);
    }

    static boolean permits(Holder<Structure> structure, ServerLevel level, ChunkPos chunk, ClassicOverworldBiomeSource biomeSource) {
        int type = biomeSource.layers().blocks(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), 1, 1)[0];
        if (structure.is(BuiltinStructures.SWAMP_HUT)) return type == OriginalOverworldBiomes.SWAMP;
        boolean possible = structure.is(BuiltinStructures.DESERT_PYRAMID)
            && (type == OriginalOverworldBiomes.DESERT || type == OriginalOverworldBiomes.DESERT_HILLS)
            || structure.is(BuiltinStructures.JUNGLE_TEMPLE)
            && (type == OriginalOverworldBiomes.JUNGLE || type == OriginalOverworldBiomes.JUNGLE_HILLS);
        if (!possible) return false;
        // ChunkMap can ask for starts while the default world spawn is being established.
        // Use the primary world's current vanilla spawn record, which is final outside initialization.
        var spawn = level == null ? net.minecraft.core.BlockPos.ZERO : level.getSharedSpawnPos();
        double dx = (double) chunk.getMinBlockX() - spawn.getX();
        double dz = (double) chunk.getMinBlockZ() - spawn.getZ();
        return dx * dx + dz * dz >= 4_000_000.0;
    }

    @Override public StructurePlacementType<?> type() { return ClassicTerrainAdapters.TEMPLE_PLACEMENT.get(); }
}
