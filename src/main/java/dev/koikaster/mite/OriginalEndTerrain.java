package dev.koikaster.mite;

import java.util.Random;

/** R195 aeu's central End island. Voxels are emitted in source x,z,y order. */
final class OriginalEndTerrain {
    private static final int GRID = 3, VERTICAL = 33, HEIGHT = 128;
    private static final double HORIZONTAL_SCALE = 1368.824, VERTICAL_SCALE = 684.412;
    private final OriginalUnderworldOctaves lowNoise;
    private final OriginalUnderworldOctaves highNoise;
    private final OriginalUnderworldOctaves blendNoise;

    OriginalEndTerrain(long seed) {
        var random = new Random(seed);
        lowNoise = new OriginalUnderworldOctaves(random, 16);
        highNoise = new OriginalUnderworldOctaves(random, 16);
        blendNoise = new OriginalUnderworldOctaves(random, 8);
    }

    byte[] blocks(int chunkX, int chunkZ) {
        double[] density = density(chunkX * 2, chunkZ * 2);
        byte[] blocks = new byte[16 * 16 * HEIGHT];
        for (int x = 0; x < GRID - 1; x++) {
            for (int z = 0; z < GRID - 1; z++) {
                for (int y = 0; y < VERTICAL - 1; y++) interpolate(blocks, density, x, y, z);
            }
        }
        return blocks;
    }

    private double[] density(int originX, int originZ) {
        double[] low = lowNoise.a(null, originX, 0, originZ, GRID, VERTICAL, GRID,
            HORIZONTAL_SCALE, VERTICAL_SCALE, HORIZONTAL_SCALE);
        double[] high = highNoise.a(null, originX, 0, originZ, GRID, VERTICAL, GRID,
            HORIZONTAL_SCALE, VERTICAL_SCALE, HORIZONTAL_SCALE);
        double[] blend = blendNoise.a(null, originX, 0, originZ, GRID, VERTICAL, GRID,
            HORIZONTAL_SCALE / 80.0, VERTICAL_SCALE / 160.0, HORIZONTAL_SCALE / 80.0);
        for (int x = 0; x < GRID; x++) {
            for (int z = 0; z < GRID; z++) {
                float px = x + originX, pz = z + originZ;
                float island = 100f - (float) Math.sqrt(px * px + pz * pz) * 8f;
                island = Math.max(-100f, Math.min(80f, island));
                for (int y = 0; y < VERTICAL; y++) {
                    int at = (x * GRID + z) * VERTICAL + y;
                    double a = low[at] / 512.0, b = high[at] / 512.0;
                    double weight = (blend[at] / 10.0 + 1.0) / 2.0;
                    double value = (weight < 0.0 ? a : weight > 1.0 ? b : a + (b - a) * weight) - 8.0 + island;
                    if (y > 14) {
                        double cutoff = (float) (y - 14) / 64.0f;
                        value = value * (1.0 - cutoff) + -3000.0 * cutoff;
                    }
                    if (y < 8) {
                        double cutoff = (float) (8 - y) / 7.0f;
                        value = value * (1.0 - cutoff) + -30.0 * cutoff;
                    }
                    low[at] = value;
                }
            }
        }
        return low;
    }

    private static void interpolate(byte[] blocks, double[] density, int cellX, int cellY, int cellZ) {
        int nw = (cellX * GRID + cellZ) * VERTICAL + cellY;
        int ne = nw + VERTICAL * GRID, sw = nw + VERTICAL, se = ne + VERTICAL;
        double northWest = density[nw], northEast = density[ne], southWest = density[sw], southEast = density[se];
        double riseNw = (density[nw + 1] - northWest) * .25, riseNe = (density[ne + 1] - northEast) * .25;
        double riseSw = (density[sw + 1] - southWest) * .25, riseSe = (density[se + 1] - southEast) * .25;
        for (int y = 0; y < 4; y++) {
            int blockY = cellY * 4 + y;
            double north = northWest, south = southWest;
            double acrossNorth = (northEast - northWest) * .125, acrossSouth = (southEast - southWest) * .125;
            for (int x = 0; x < 8; x++) {
                int blockX = cellX * 8 + x;
                double value = north, acrossZ = (south - north) * .125;
                for (int z = 0; z < 8; z++) {
                    int blockZ = cellZ * 8 + z;
                    if (value > 0.0) blocks[(blockX * 16 + blockZ) * HEIGHT + blockY] = 121;
                    value += acrossZ;
                }
                north += acrossNorth;
                south += acrossSouth;
            }
            northWest += riseNw;
            northEast += riseNe;
            southWest += riseSw;
            southEast += riseSe;
        }
    }
}
