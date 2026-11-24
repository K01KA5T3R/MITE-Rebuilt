package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/** The R195 agn location lottery; native generation still owns the fortress pieces. */
final class ClassicFortressPlacement extends RandomSpreadStructurePlacement {
    static final Codec<ClassicFortressPlacement> SERIALIZER = Codec.unit(ClassicFortressPlacement::new);
    private static final int REGION_SIZE = 16;

    ClassicFortressPlacement() {
        super(REGION_SIZE, 8, RandomSpreadType.LINEAR, 0);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        var cell = candidates(state.getLevelSeed(), chunkX, chunkZ);
        return cell.allowed() && cell.position().x == chunkX && cell.position().z == chunkZ;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        return candidates(seed, chunkX, chunkZ).position();
    }

    @Override
    public StructurePlacementType<?> type() {
        return ClassicTerrainAdapters.FORTRESS_PLACEMENT.get();
    }

    private record Cell(boolean allowed, ChunkPos position) {}

    private static Cell candidates(long seed, int chunkX, int chunkZ) {
        int regionX = chunkX >> 4;
        int regionZ = chunkZ >> 4;
        var random = new Random((long) (regionX ^ regionZ << 4) ^ seed);
        random.nextInt();
        boolean allowed = random.nextInt(3) == 0;
        int x = (regionX << 4) + 4 + random.nextInt(8);
        int z = (regionZ << 4) + 4 + random.nextInt(8);
        return new Cell(allowed, new ChunkPos(x, z));
    }
}
