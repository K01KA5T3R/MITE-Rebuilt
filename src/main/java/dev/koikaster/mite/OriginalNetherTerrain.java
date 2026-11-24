package dev.koikaster.mite;

import java.util.Random;

/** R195 aep's 128-block Nether. Portals, structure generation and ores are separate. */
final class OriginalNetherTerrain {
    static final byte AIR = 0, NETHERRACK = 87, LAVA = 11, GRAVEL = 13, SOUL_SAND = 88, CORE = 95;
    private static final int HEIGHT = 128, GRID = 5, VERTICAL = 17;
    private static final double HORIZONTAL_SCALE = 684.412, VERTICAL_SCALE = 2053.236;
    private final OriginalUnderworldOctaves lower;
    private final OriginalUnderworldOctaves upper;
    private final OriginalUnderworldOctaves blend;
    private final OriginalUnderworldOctaves floor;
    private final OriginalUnderworldOctaves depth;

    OriginalNetherTerrain(long seed) {
        var random = new Random(seed);
        lower = new OriginalUnderworldOctaves(random, 16);
        upper = new OriginalUnderworldOctaves(random, 16);
        blend = new OriginalUnderworldOctaves(random, 8);
        floor = new OriginalUnderworldOctaves(random, 4);
        depth = new OriginalUnderworldOctaves(random, 4);
    }

    byte[] blocks(int chunkX, int chunkZ) {
        int x = chunkX * 4, z = chunkZ * 4;
        double[] density = density(x, z);
        byte[] blocks = new byte[16 * 16 * HEIGHT];
        for (int cellX = 0; cellX < GRID - 1; cellX++) {
            for (int cellZ = 0; cellZ < GRID - 1; cellZ++) {
                for (int y = 0; y < VERTICAL - 1; y++) interpolate(blocks, density, cellX, y, cellZ);
            }
        }
        return blocks;
    }

    Random decorationRandom(int chunkX, int chunkZ) {
        var random = OriginalOverworldDensity.surfaceRandom(chunkX, chunkZ);
        // Surface consumes three doubles and one bounded integer per height in each column.
        for (int i = 0; i < 16 * 16; i++) {
            random.nextDouble();
            random.nextDouble();
            random.nextDouble();
            for (int y = HEIGHT - 1; y >= 0; y--) random.nextInt(5);
        }
        return random;
    }

    byte[] surface(int chunkX, int chunkZ) {
        return surface(chunkX, chunkZ, OriginalOverworldDensity.surfaceRandom(chunkX, chunkZ));
    }

    byte[] surface(int chunkX, int chunkZ, Random random) {
        byte[] blocks = blocks(chunkX, chunkZ);
        int x = chunkX * 16, z = chunkZ * 16;
        double scale = .03125;
        double[] soul = floor.a(null, x, z, 0, 16, 16, 1, scale, scale, 1);
        double[] gravel = floor.a(null, x, 109, z, 16, 1, 16, scale, 1, scale);
        double[] thickness = depth.a(null, x, z, 0, 16, 16, 1, scale * 2, scale * 2, scale * 2);
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int index = localZ * 16 + localX;
                boolean hasSoul = soul[index] + random.nextDouble() * .2 > 0;
                boolean hasGravel = gravel[index] + random.nextDouble() * .2 > 0;
                int size = (int) (thickness[index] / 3.0 + 3 + random.nextDouble() * .25);
                surfaceColumn(blocks, index * HEIGHT, hasSoul, hasGravel, size, random);
            }
        }
        return blocks;
    }

    private static void surfaceColumn(byte[] blocks, int base, boolean hasSoul, boolean hasGravel, int size, Random random) {
        int remaining = -1;
        byte top = NETHERRACK, filler = NETHERRACK;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            int index = base + y;
            if (y >= HEIGHT - 1 - random.nextInt(5) || y == 0) {
                blocks[index] = CORE;
                continue;
            }
            if (blocks[index] == AIR) {
                remaining = -1;
                continue;
            }
            if (blocks[index] != NETHERRACK) continue;
            if (remaining == -1) {
                if (size <= 0) {
                    top = AIR;
                    filler = NETHERRACK;
                } else if (y >= 60 && y <= 65) {
                    top = hasSoul ? SOUL_SAND : hasGravel ? GRAVEL : NETHERRACK;
                    filler = hasSoul ? SOUL_SAND : NETHERRACK;
                }
                if (y < 64 && top == AIR) top = LAVA;
                remaining = size;
                blocks[index] = y >= 63 ? top : filler;
            } else if (remaining > 0) {
                remaining--;
                blocks[index] = filler;
            }
        }
    }

    private double[] density(int originX, int originZ) {
        double[] low = lower.a(null, originX, 0, originZ, GRID, VERTICAL, GRID, HORIZONTAL_SCALE, VERTICAL_SCALE, HORIZONTAL_SCALE);
        double[] high = upper.a(null, originX, 0, originZ, GRID, VERTICAL, GRID, HORIZONTAL_SCALE, VERTICAL_SCALE, HORIZONTAL_SCALE);
        double[] selector = blend.a(null, originX, 0, originZ, GRID, VERTICAL, GRID,
            HORIZONTAL_SCALE / 80, VERTICAL_SCALE / 60, HORIZONTAL_SCALE / 80);
        double[] falloff = new double[VERTICAL];
        for (int y = 0; y < VERTICAL; y++) {
            int edge = Math.min(y, VERTICAL - 1 - y);
            double fade = Math.max(0, 4 - edge);
            falloff[y] = Math.cos(y * Math.PI * 6.0 / VERTICAL) * 2.0 - fade * fade * fade * 10.0;
        }
        for (int index = 0; index < low.length; index++) {
            int y = index % VERTICAL;
            double a = low[index] / 512.0, b = high[index] / 512.0;
            double weight = (selector[index] / 10.0 + 1.0) / 2.0;
            double value = (weight < 0 ? a : weight > 1 ? b : a + (b - a) * weight) - falloff[y];
            if (y > VERTICAL - 4) {
                double cutoff = (float) (y - (VERTICAL - 4)) / 3.0f;
                value = value * (1 - cutoff) + -10.0 * cutoff;
            }
            low[index] = value;
        }
        return low;
    }

    private static void interpolate(byte[] blocks, double[] density, int cellX, int cellY, int cellZ) {
        int nw = (cellX * GRID + cellZ) * VERTICAL + cellY;
        int ne = nw + VERTICAL * GRID, sw = nw + VERTICAL, se = ne + VERTICAL;
        double northWest = density[nw], northEast = density[ne], southWest = density[sw], southEast = density[se];
        double riseNw = (density[nw + 1] - northWest) * .125, riseNe = (density[ne + 1] - northEast) * .125;
        double riseSw = (density[sw + 1] - southWest) * .125, riseSe = (density[se + 1] - southEast) * .125;
        for (int y = 0; y < 8; y++) {
            int blockY = cellY * 8 + y;
            double north = northWest, south = southWest;
            double acrossNorth = (northEast - northWest) * .25, acrossSouth = (southEast - southWest) * .25;
            for (int x = 0; x < 4; x++) {
                int blockX = cellX * 4 + x;
                double value = north, acrossZ = (south - north) * .25;
                for (int z = 0; z < 4; z++) {
                    int blockZ = cellZ * 4 + z;
                    blocks[(blockX * 16 + blockZ) * HEIGHT + blockY] = value > 0.0 ? NETHERRACK : blockY < 32 ? LAVA : AIR;
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
