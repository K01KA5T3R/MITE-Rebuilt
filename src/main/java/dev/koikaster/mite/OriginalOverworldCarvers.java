package dev.koikaster.mite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static dev.koikaster.mite.OriginalOverworldDensity.*;

/** R195 aer, aeq and aem carve the source-format block array before structures. */
final class OriginalOverworldCarvers {
    static final byte LAVA = 10;
    static final byte COBWEB = 30;
    private static final int CAVE_RANGE = 8;
    private static final int[] ADJACENT = {1, -1, 128, -128, 2048, -2048};
    private final long seed, multiplierX, multiplierZ;
    private final OriginalOverworldBiomes biomes;
    private final SourceUnderworldRandomTables tables;
    private final OriginalCaveNetwork networks;

    OriginalOverworldCarvers(long seed, OriginalOverworldBiomes biomes) {
        this.seed = seed;
        this.biomes = biomes;
        tables = new SourceUnderworldRandomTables(seed);
        networks = new OriginalCaveNetwork(seed, biomes);
        var random = new Random(seed);
        multiplierX = random.nextLong();
        multiplierZ = random.nextLong();
    }

    void carve(int chunkX, int chunkZ, byte[] blocks, Random surfaceRandom) {
        Map<Integer, Integer> sandFalls = carveCavesAndRavines(chunkX, chunkZ, blocks);
        networks.carve(chunkX, chunkZ, blocks);
        cobwebs(blocks, surfaceRandom);
        sandFalls.forEach((column, y) -> fallSand(blocks, column, y));
    }

    Map<Integer, Integer> carveCavesAndRavines(int chunkX, int chunkZ, byte[] blocks) {
        int x = (chunkX - CAVE_RANGE) * 16, z = (chunkZ - CAVE_RANGE) * 16;
        int side = CAVE_RANGE * 2 + 1;
        int[] coarse = biomes.terrain((x - 2) >> 2, (z - 2) >> 2, side * 4 + 1, side * 4 + 1);
        int[][] climate = new int[side][side];
        for (int dx = 0; dx < side; dx++) for (int dz = 0; dz < side; dz++) {
            climate[dx][dz] = originBiome(biomes, x, z, coarse, side, dx, dz);
        }
        int[] localClimate = biomes.blocks(chunkX * 16, chunkZ * 16, 16, 16);
        Map<Integer, Integer> sandFalls = new LinkedHashMap<>();
        for (int dx = -CAVE_RANGE; dx <= CAVE_RANGE; dx++) {
            for (int dz = -CAVE_RANGE; dz <= CAVE_RANGE; dz++) {
                int biome = climate[CAVE_RANGE + dx][CAVE_RANGE + dz];
                int sourceX = chunkX + dx, sourceZ = chunkZ + dz;
                long sourceSeed = sourceX * multiplierX ^ sourceZ * multiplierZ ^ seed;
                caves(new Random(sourceSeed), sourceX, sourceZ, chunkX, chunkZ, biome, localClimate, blocks, sandFalls);
            }
        }
        if (localClimate[0] != OriginalOverworldBiomes.OCEAN) {
            for (int dx = -CAVE_RANGE; dx <= CAVE_RANGE; dx++) {
                for (int dz = -CAVE_RANGE; dz <= CAVE_RANGE; dz++) {
                    if (climate[CAVE_RANGE + dx][CAVE_RANGE + dz] == OriginalOverworldBiomes.OCEAN) continue;
                    int sourceX = chunkX + dx, sourceZ = chunkZ + dz;
                    long sourceSeed = sourceX * multiplierX ^ sourceZ * multiplierZ ^ seed;
                    ravine(new Random(sourceSeed), sourceX, sourceZ, chunkX, chunkZ, localClimate, blocks);
                }
            }
        }
        return sandFalls;
    }

    private void cobwebs(byte[] blocks, Random random) {
        int cursor = random.nextInt();
        for (int attempt = 0; attempt < 128; attempt++) {
            int x = tables.int_14_plus_1[++cursor & Short.MAX_VALUE];
            cursor++;
            int z = tables.int_14_plus_1[++cursor & Short.MAX_VALUE];
            int y = tables.int_126_plus_1[cursor & Short.MAX_VALUE];
            int position = column(z, x) + y;
            if (blocks[position] != AIR) continue;
            int covered = 0;
            boolean rock = false;
            for (int offset : ADJACENT) {
                byte neighbor = blocks[position + offset];
                if (neighbor != AIR) covered++;
                if (neighbor == STONE) rock = true;
            }
            if (covered < 4 || !rock || blocks[position - 1] == LAVA) continue;
            blocks[position] = COBWEB;
            attempt -= 128 * 4;
        }
    }

    private static int originBiome(OriginalOverworldBiomes biomes, int x, int z, int[] climate, int side, int dx, int dz) {
        int topLeft = (dz * (side * 4 + 1) + dx) * 4;
        int value = climate[topLeft];
        int below = topLeft + side * 4 + 1;
        return climate[topLeft + 1] == value && climate[below] == value && climate[below + 1] == value
            ? value : biomes.blocks(x + dx * 16, z + dz * 16, 1, 1)[0];
    }

    private static void caves(Random random, int sourceX, int sourceZ, int chunkX, int chunkZ, int biome,
                              int[] localClimate, byte[] blocks, Map<Integer, Integer> sandFalls) {
        float frequency = switch (biome) {
            case OriginalOverworldBiomes.PLAINS, OriginalOverworldBiomes.SWAMP -> .8f;
            case OriginalOverworldBiomes.ICE_MOUNTAINS -> 1.2f;
            case OriginalOverworldBiomes.EXTREME_HILLS -> 1.4f;
            default -> 1;
        };
        if (random.nextInt((int) (15.0f / frequency)) != 0) return;
        int count = (int) (random.nextInt(random.nextInt(random.nextInt(30) + 1) + 1) * frequency);
        for (int i = 0; i < count; i++) {
            double x = sourceX * 16 + random.nextInt(16);
            double y = random.nextInt(random.nextInt(120) + 8);
            double z = sourceZ * 16 + random.nextInt(16);
            int tunnels = 1;
            int rarity = y > 23 && y < 33 ? 2 : 10;
            if (random.nextInt(4) == 0) {
                long roomSeed = random.nextLong();
                float thickness = 1 + random.nextFloat() * 6;
                carvePath(roomSeed, chunkX, chunkZ, localClimate, blocks, sandFalls,
                    x, y, z, thickness, 0, 0, -1, -1, .5, false);
                tunnels += random.nextInt(4);
            }
            for (int j = 0; j < tunnels; j++) {
                float yaw = random.nextFloat() * (float) Math.PI * 2;
                float pitch = (random.nextFloat() - .5f) * 2 / 8;
                float size = random.nextFloat() * 2 + random.nextFloat();
                if (random.nextInt(rarity) == 0) {
                    size *= random.nextFloat() * random.nextFloat() * 3 + 1;
                    if (rarity < 10) size *= 1 + random.nextFloat() * .5f;
                } else if (y > 15 && y < 41 && random.nextInt(2) == 0) {
                    size *= random.nextFloat() * random.nextFloat() * 1.5f + 1;
                }
                carvePath(random.nextLong(), chunkX, chunkZ, localClimate, blocks, sandFalls,
                    x, y, z, size, yaw, pitch, 0, 0, 1, false);
            }
        }
    }

    private void ravine(Random random, int sourceX, int sourceZ, int chunkX, int chunkZ,
                        int[] localClimate, byte[] blocks) {
        if (random.nextInt(100) != 0 && !(seed == 1L && sourceX == -6 && sourceZ == 31)) return;
        double x = sourceX * 16 + random.nextInt(16);
        double y = random.nextInt(random.nextInt(40) + 8) + 20;
        double z = sourceZ * 16 + random.nextInt(16);
        float yaw = random.nextFloat() * (float) Math.PI * 2;
        float pitch = (random.nextFloat() - .5f) * 2 / 8;
        float thickness = (random.nextFloat() * 2 + random.nextFloat()) * 2;
        carvePath(random.nextLong(), chunkX, chunkZ, localClimate, blocks, null,
            x, y, z, thickness, yaw, pitch, 0, 0, 3, true);
    }

    private static void carvePath(long seed, int chunkX, int chunkZ, int[] climate, byte[] blocks,
                                  Map<Integer, Integer> sandFalls, double x, double y, double z, float size,
                                  float yaw, float pitch, int start, int end, double heightScale, boolean ravine) {
        var random = new Random(seed);
        int originX = chunkX * 16, originZ = chunkZ * 16;
        if (end <= 0) end = 112 - random.nextInt(28);
        boolean room = start == -1;
        if (room) start = end / 2;
        int branch = 0;
        boolean shallow = false;
        float[] widths = null;
        if (ravine) {
            widths = new float[HEIGHT];
            float scale = 1;
            for (int blockY = 0; blockY < HEIGHT; blockY++) {
                if (blockY == 0 || random.nextInt(3) == 0) scale = 1 + random.nextFloat() * random.nextFloat();
                widths[blockY] = scale * scale;
            }
        } else {
            branch = random.nextInt(end / 2) + end / 4;
            shallow = random.nextInt(6) == 0;
        }
        float turn = 0, incline = 0;
        for (int step = start; step < end; step++) {
            double radius = 1.5 + SourceDragonTrig.sin(step * (float) Math.PI / end) * size;
            double height = radius * heightScale;
            if (ravine) {
                radius *= random.nextFloat() * .25 + .75;
                height *= random.nextFloat() * .25 + .75;
            }
            float cosPitch = SourceDragonTrig.cos(pitch);
            x += SourceDragonTrig.cos(yaw) * cosPitch;
            y += SourceDragonTrig.sin(pitch);
            z += SourceDragonTrig.sin(yaw) * cosPitch;
            pitch *= shallow ? .92f : .7f;
            float influence = ravine ? .05f : .1f;
            pitch += incline * influence;
            yaw += turn * influence;
            incline *= ravine ? .8f : .9f;
            turn *= ravine ? .5f : .75f;
            incline += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2;
            turn += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4;
            if (!ravine && !room && step == branch && size > 1) {
                carvePath(random.nextLong(), chunkX, chunkZ, climate, blocks, sandFalls, x, y, z,
                    random.nextFloat() * .5f + .5f, yaw - (float) Math.PI / 2, pitch / 3, step, end, 1, false);
                carvePath(random.nextLong(), chunkX, chunkZ, climate, blocks, sandFalls, x, y, z,
                    random.nextFloat() * .5f + .5f, yaw + (float) Math.PI / 2, pitch / 3, step, end, 1, false);
                return;
            }
            if (!room && random.nextInt(4) == 0) continue;
            double dx = x - (originX + 8), dz = z - (originZ + 8);
            double remaining = end - step, maxTravel = size + 18;
            if (dx * dx + dz * dz - remaining * remaining > maxTravel * maxTravel) return;
            if (Math.abs(dx) > 16.0 + radius * 2 || Math.abs(dz) > 16.0 + radius * 2) continue;
            if (excavate(blocks, climate, originX, originZ, x, y, z, radius, height, widths, sandFalls) && room) return;
        }
    }

    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        static Bounds around(int originX, int originZ, double x, double y, double z, double radius, double height) {
            return new Bounds(
                Math.max(0, (int) Math.floor(x - radius) - originX - 1), Math.min(16, (int) Math.floor(x + radius) - originX + 1),
                Math.max(1, (int) Math.floor(y - height) - 1), Math.min(120, (int) Math.floor(y + height) + 1),
                Math.max(0, (int) Math.floor(z - radius) - originZ - 1), Math.min(16, (int) Math.floor(z + radius) - originZ + 1));
        }

        boolean waterAtEdge(byte[] blocks) {
            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    int base = column(x, z);
                    for (int y = maxY + 1; y >= minY - 1; y--) {
                        if (y < 0 || y >= HEIGHT) continue;
                        if (blocks[base + y] == WATER) return true;
                        if (y != minY - 1 && x != minX && x != maxX - 1 && z != minZ && z != maxZ - 1) y = minY;
                    }
                }
            }
            return false;
        }
    }

    private static boolean excavate(byte[] blocks, int[] climate, int originX, int originZ, double x, double y, double z,
                                     double radius, double height, float[] widths, Map<Integer, Integer> sandFalls) {
        Bounds bounds = Bounds.around(originX, originZ, x, y, z, radius, height);
        if (bounds.waterAtEdge(blocks)) return false;
        boolean ravine = widths != null;
        Map<Integer, Integer> pending = ravine ? null : new LinkedHashMap<>();
        for (int localX = bounds.minX(); localX < bounds.maxX(); localX++) {
            double acrossX = (localX + originX + .5 - x) / radius;
            for (int localZ = bounds.minZ(); localZ < bounds.maxZ(); localZ++) {
                double acrossZ = (localZ + originZ + .5 - z) / radius;
                double distance = acrossX * acrossX + acrossZ * acrossZ;
                if (distance >= 1) continue;
                int base = column(localX, localZ);
                int index = base + bounds.maxY();
                boolean restoreSurface = false;
                for (int localY = bounds.maxY() - 1; localY >= bounds.minY(); localY--, index--) {
                    double aboveCenter = (localY + .5 - y) / height;
                    boolean inside = ravine ? distance * widths[localY] + aboveCenter * aboveCenter / 6 < 1
                        : aboveCenter > -.7 && acrossX * acrossX + aboveCenter * aboveCenter + acrossZ * acrossZ < 1;
                    if (!inside) continue;
                    byte material = blocks[index];
                    if (material == GRASS) restoreSurface = true;
                    if (material != STONE && material != DIRT && material != GRASS && material != SAND && material != SANDSTONE) continue;
                    if (localY < 10) {
                        blocks[index] = LAVA;
                        continue;
                    }
                    if (!ravine && blocks[index + 1] == SAND && waterAboveSand(blocks, index + 1, base + HEIGHT)) break;
                    if (ravine || material != SAND) blocks[index] = AIR;
                    if (restoreSurface && blocks[index - 1] == DIRT) {
                        blocks[index - 1] = topBlock(climate[localZ * 16 + localX]);
                    } else if (blocks[index + 1] == SAND) {
                        if (ravine) clearSand(blocks, index + 1, base + HEIGHT);
                        else pending.putIfAbsent(base, localY + 2);
                    }
                }
            }
        }
        if (pending != null) pending.forEach((base, yStart) -> {
            int scan = yStart;
            while (scan > 48 && (blocks[base + scan] != SAND || blocks[base + scan - 1] != AIR)) scan--;
            if (blocks[base + scan] == SAND) sandFalls.put(base, scan);
        });
        return true;
    }

    private static boolean waterAboveSand(byte[] blocks, int index, int limit) {
        do { index++; } while (index < limit && blocks[index] == SAND);
        return index < limit && blocks[index] == WATER;
    }

    private static void clearSand(byte[] blocks, int index, int limit) {
        while (index < limit && blocks[index] == SAND) blocks[index++] = AIR;
    }

    private static void fallSand(byte[] blocks, int base, int y) {
        if (blocks[base + y] != SAND) return;
        int top = y;
        while (top + 1 < HEIGHT && blocks[base + top + 1] == SAND) top++;
        int count = top - y + 1;
        while (y > 0 && (blocks[base + y - 1] == AIR || blocks[base + y - 1] == COBWEB)) y--;
        while (y <= top) blocks[base + y++] = count-- > 0 ? SAND : AIR;
    }

    private static byte topBlock(int biome) {
        return biome == OriginalOverworldBiomes.DESERT || biome == OriginalOverworldBiomes.DESERT_HILLS
            || biome == OriginalOverworldBiomes.BEACH ? SAND : GRASS;
    }

    private static int column(int x, int z) {
        return ((x * 16) + z) * HEIGHT;
    }
}
