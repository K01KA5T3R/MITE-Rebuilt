package dev.koikaster.mite;

import java.util.Random;

/** R195 aet base density and surface replacement. */
final class OriginalOverworldDensity {
    static final int HEIGHT = 128, SEA_LEVEL = 63;
    static final byte AIR = 0, STONE = 1, GRASS = 2, DIRT = 3, BEDROCK = 7, WATER = 9, SAND = 12,
        SANDSTONE = 24, ICE = 79;
    private static final int WIDTH = 5;
    private static final int SAMPLES_Y = 17;
    private static final int BIOME_WIDTH = 10;
    private static final double SCALE = 684.412;
    private static final float[] DEPTH = {
        -1, .1f, .1f, .3f, .1f, .1f, -.2f, -.5f, .1f, .1f, -1, -.5f, .1f,
        .3f, .2f, -1, 0, .3f, .3f, .3f, .2f, .2f, 1.8f, -.5f, -.5f, -.5f
    };
    private static final float[] VARIATION = {
        .4f, .3f, .2f, 1.5f, .3f, .4f, .1f, 0, .3f, .3f, .5f, 0, .3f,
        1.3f, 1, .1f, .1f, .8f, .7f, .8f, .8f, .4f, .5f, 0, 0, 0
    };
    private static final float[] WEIGHTS = new float[25];
    static {
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                WEIGHTS[(z + 2) * 5 + x + 2] = 10.0f / (float) Math.sqrt(x * x + z * z + .2f);
            }
        }
    }

    private final OriginalUnderworldOctaves lower;
    private final OriginalUnderworldOctaves upper;
    private final OriginalUnderworldOctaves selector;
    private final OriginalUnderworldOctaves depth;
    private final OriginalUnderworldOctaves surface;
    private final OriginalUnderworldOctaves stoneExposure;

    OriginalOverworldDensity(long seed) {
        var random = new Random(seed);
        lower = new OriginalUnderworldOctaves(random, 16);
        upper = new OriginalUnderworldOctaves(random, 16);
        selector = new OriginalUnderworldOctaves(random, 8);
        surface = new OriginalUnderworldOctaves(random, 4);
        new OriginalUnderworldOctaves(random, 10); // aet also samples this unused two-dimensional array.
        depth = new OriginalUnderworldOctaves(random, 16);
        new OriginalUnderworldOctaves(random, 8); // Later generation uses this sampler.
        stoneExposure = new OriginalUnderworldOctaves(random, 4);
    }

    double[] sample(int chunkX, int chunkZ, OriginalOverworldBiomes biomes) {
        int x = chunkX * 4, z = chunkZ * 4;
        int[] climates = biomes.terrain(x - 2, z - 2, BIOME_WIDTH, BIOME_WIDTH);
        double[] depthMap = depth.a(null, x, z, WIDTH, WIDTH, 200.0, 200.0, .5);
        double[] blend = selector.a(null, x, 0, z, WIDTH, SAMPLES_Y, WIDTH, SCALE / 80, SCALE / 160, SCALE / 80);
        double[] minimum = lower.a(null, x, 0, z, WIDTH, SAMPLES_Y, WIDTH, SCALE, SCALE, SCALE);
        double[] maximum = upper.a(null, x, 0, z, WIDTH, SAMPLES_Y, WIDTH, SCALE, SCALE, SCALE);
        for (int localX = 0; localX < WIDTH; localX++) {
            for (int localZ = 0; localZ < WIDTH; localZ++) {
                int biomeIndex = (localZ + 2) * BIOME_WIDTH + localX + 2;
                float centerDepth = DEPTH[climates[biomeIndex]];
                float variation = 0, baseHeight = 0, totalWeight = 0;
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int biome = climates[biomeIndex + dz * BIOME_WIDTH + dx];
                        float weight = WEIGHTS[(dz + 2) * 5 + dx + 2] / (DEPTH[biome] + 2.0f);
                        if (DEPTH[biome] > centerDepth) weight /= 2.0f;
                        variation += VARIATION[biome] * weight;
                        baseHeight += DEPTH[biome] * weight;
                        totalWeight += weight;
                    }
                }
                variation = variation / totalWeight * .9f + .1f;
                baseHeight = (baseHeight / totalWeight * 4.0f - 1.0f) / 8.0f;
                int column = localX * WIDTH + localZ;
                double relief = relief(depthMap[column]);
                double baseLevel = SAMPLES_Y / 2.0 + (baseHeight + relief * .2) * SAMPLES_Y / 16.0 * 4.0;
                for (int y = 0; y < SAMPLES_Y; y++) {
                    int index = column * SAMPLES_Y + y;
                    double falloff = (y - baseLevel) * 12.0 * 128.0 / 128.0 / variation;
                    if (falloff < 0.0) falloff *= 4.0;
                    double mixing = (blend[index] / 10.0 + 1.0) / 2.0;
                    double low = minimum[index] / 512.0, high = maximum[index] / 512.0;
                    double density = (mixing < 0.0 ? low : mixing > 1.0 ? high : low + (high - low) * mixing) - falloff;
                    if (y > SAMPLES_Y - 4) {
                        double topFalloff = (float) (y - (SAMPLES_Y - 4)) / 3.0f;
                        density = density * (1.0 - topFalloff) + -10.0 * topFalloff;
                    }
                    minimum[index] = density;
                }
            }
        }
        return minimum;
    }

    byte[] blocks(int chunkX, int chunkZ, OriginalOverworldBiomes biomes) {
        double[] density = sample(chunkX, chunkZ, biomes);
        byte[] blocks = new byte[16 * 16 * HEIGHT];
        for (int x = 0; x < WIDTH - 1; x++) {
            for (int z = 0; z < WIDTH - 1; z++) {
                for (int y = 0; y < SAMPLES_Y - 1; y++) {
                    sampleCell(blocks, density, x, z, y);
                }
            }
        }
        return blocks;
    }

    byte[] surfaceBlocks(int chunkX, int chunkZ, OriginalOverworldBiomes biomes) {
        return surfaceBlocks(chunkX, chunkZ, biomes, surfaceRandom(chunkX, chunkZ));
    }

    static Random surfaceRandom(int chunkX, int chunkZ) {
        return new Random(chunkX * 341873128712L + chunkZ * 132897987541L);
    }

    byte[] surfaceBlocks(int chunkX, int chunkZ, OriginalOverworldBiomes biomes, Random random) {
        byte[] blocks = blocks(chunkX, chunkZ, biomes);
        int worldX = chunkX * 16, worldZ = chunkZ * 16;
        int[] climate = biomes.blocks(worldX, worldZ, 16, 16);
        double[] soil = surface.a(null, worldX, 0, worldZ, 16, 1, 16, .0625, .0625, .0625);
        double[] thinning = surface.a(null, worldX, 0, worldZ, 16, 1, 16, .5, .5, .5);
        double[] bare = stoneExposure.a(null, worldX, 0, worldZ, 16, 1, 16, 1, 1, 1);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int originalIndex = x + z * 16;
                int biome = climate[z + x * 16];
                double thickness = soil[originalIndex] / 3.0 + 3.0 - .5;
                int threshold = (int) (thinning[originalIndex] / 3.0 + 1.0);
                applySurfaceColumn(blocks, originalIndex * HEIGHT, biome, thickness, threshold, bare[originalIndex], random);
            }
        }
        return blocks;
    }

    private static void applySurfaceColumn(byte[] blocks, int base, int biome, double thickness,
                                           int threshold, double stoneNoise, Random random) {
        byte originalTop = GRASS, originalFiller = DIRT;
        if (biome == OriginalOverworldBiomes.DESERT || biome == OriginalOverworldBiomes.DESERT_HILLS
                || biome == OriginalOverworldBiomes.BEACH) {
            originalTop = SAND;
            originalFiller = SAND;
        }
        byte top = originalTop, filler = originalFiller;
        int remaining = -1, depth = (int) thickness;
        boolean freezes = biome == OriginalOverworldBiomes.FROZEN_OCEAN || biome == OriginalOverworldBiomes.FROZEN_RIVER
            || biome == OriginalOverworldBiomes.ICE_PLAINS || biome == OriginalOverworldBiomes.ICE_MOUNTAINS
            || biome == OriginalOverworldBiomes.TAIGA || biome == OriginalOverworldBiomes.TAIGA_HILLS;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            int cell = base + y;
            if (y <= random.nextInt(5)) {
                blocks[cell] = BEDROCK;
                continue;
            }
            if (blocks[cell] == AIR) {
                remaining = -1;
                continue;
            }
            if (blocks[cell] != STONE) continue;
            if (remaining == -1) {
                boolean aboveSea = y >= SEA_LEVEL;
                if (depth <= 0) {
                    top = thickness >= .25 && threshold <= 0 && aboveSea ? originalTop : AIR;
                    if (top == AIR && aboveSea && thickness > .7) {
                        if (thickness > .95) top = originalTop;
                        else if (stoneNoise < 1.0) top = STONE;
                    }
                    filler = STONE;
                } else if (y >= SEA_LEVEL - 4 && y <= SEA_LEVEL + 1) {
                    top = originalTop;
                    filler = originalFiller;
                }
                if (!aboveSea && top == AIR) top = freezes ? ICE : WATER;
                remaining = depth;
                blocks[cell] = y >= SEA_LEVEL - 1 ? top : filler;
            } else if (remaining > 0) {
                remaining--;
                blocks[cell] = filler;
                if (remaining == 0 && filler == SAND) {
                    remaining = random.nextInt(4);
                    filler = SANDSTONE;
                }
            }
        }
    }

    private static void sampleCell(byte[] blocks, double[] density, int cellX, int cellZ, int cellY) {
        int nw = (cellX * WIDTH + cellZ) * SAMPLES_Y + cellY;
        int ne = nw + SAMPLES_Y * WIDTH, sw = nw + SAMPLES_Y, se = ne + SAMPLES_Y;
        double northWest = density[nw], northEast = density[ne], southWest = density[sw], southEast = density[se];
        double riseNw = (density[nw + 1] - northWest) * .125, riseNe = (density[ne + 1] - northEast) * .125;
        double riseSw = (density[sw + 1] - southWest) * .125, riseSe = (density[se + 1] - southEast) * .125;
        for (int stepY = 0; stepY < 8; stepY++) {
            int y = cellY * 8 + stepY;
            double north = northWest, south = southWest;
            double acrossNorth = (northEast - northWest) * .25, acrossSouth = (southEast - southWest) * .25;
            for (int stepX = 0; stepX < 4; stepX++) {
                int x = cellX * 4 + stepX;
                double acrossZ = (south - north) * .25;
                double value = north - acrossZ;
                for (int stepZ = 0; stepZ < 4; stepZ++) {
                    int z = cellZ * 4 + stepZ;
                    value += acrossZ;
                    blocks[(x * 16 + z) * HEIGHT + y] = value > 0.0 ? STONE : y < SEA_LEVEL ? WATER : AIR;
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

    private static double relief(double rawDepth) {
        double value = rawDepth / 8000.0;
        if (value < 0.0) value = -value * .3;
        value = value * 3.0 - 2.0;
        return value < 0.0 ? Math.max(value / 2.0, -1.0) / 1.4 / 2.0 : Math.min(value, 1.0) / 8.0;
    }
}
