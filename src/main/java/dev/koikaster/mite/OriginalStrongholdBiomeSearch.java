package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.level.ChunkPos;

/** The acv search increments its selection counter only when it replaces the result. */
final class OriginalStrongholdBiomeSearch {
    static List<ChunkPos> rings(OriginalOverworldBiomes biomes, long seed) {
        var random = new Random(seed);
        double angle = random.nextDouble() * Math.PI * 2.0;
        var positions = new ArrayList<ChunkPos>(3);
        for (int i = 0; i < 3; i++) {
            double distance = (1.25 + random.nextDouble()) * 512.0;
            int x = (int) Math.round(Math.cos(angle) * distance);
            int z = (int) Math.round(Math.sin(angle) * distance);
            positions.add(locate(biomes, x, z, random));
            angle += Math.PI * 2.0 / 3.0;
        }
        return List.copyOf(positions);
    }

    private static ChunkPos locate(OriginalOverworldBiomes biomes, int chunkX, int chunkZ, Random random) {
        int centerX = (chunkX << 4) + 8, centerZ = (chunkZ << 4) + 8;
        int firstX = (centerX - 112) >> 2, firstZ = (centerZ - 112) >> 2;
        int lastX = (centerX + 112) >> 2, lastZ = (centerZ + 112) >> 2;
        int width = lastX - firstX + 1;
        var areas = biomes.terrain(firstX, firstZ, width, lastZ - firstZ + 1);
        int chosen = 0;
        ChunkPos result = null;
        for (int index = 0; index < areas.length; index++) {
            if (!suitable(areas[index]) || result != null && random.nextInt(chosen + 1) != 0) continue;
            result = new ChunkPos((firstX + index % width) >> 2, (firstZ + index / width) >> 2);
            chosen++;
        }
        return result == null ? new ChunkPos(chunkX, chunkZ) : result;
    }

    private static boolean suitable(int biome) {
        return switch (biome) {
            case OriginalOverworldBiomes.DESERT, OriginalOverworldBiomes.FOREST, OriginalOverworldBiomes.EXTREME_HILLS,
                 OriginalOverworldBiomes.SWAMP, OriginalOverworldBiomes.TAIGA, OriginalOverworldBiomes.ICE_PLAINS,
                 OriginalOverworldBiomes.ICE_MOUNTAINS, OriginalOverworldBiomes.DESERT_HILLS, OriginalOverworldBiomes.FOREST_HILLS,
                 OriginalOverworldBiomes.MOUNTAIN_EDGE, OriginalOverworldBiomes.JUNGLE, OriginalOverworldBiomes.JUNGLE_HILLS -> true;
            default -> false;
        };
    }

    private OriginalStrongholdBiomeSearch() {}
}
