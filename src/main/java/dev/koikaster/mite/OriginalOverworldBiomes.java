package dev.koikaster.mite;

/** The R195 akq layer graph. Each tile owns its RNG and all layers are immutable. */
final class OriginalOverworldBiomes {
    static final int OCEAN = 0, PLAINS = 1, DESERT = 2, EXTREME_HILLS = 3, FOREST = 4, TAIGA = 5, SWAMP = 6,
        RIVER = 7, FROZEN_OCEAN = 10, FROZEN_RIVER = 11, ICE_PLAINS = 12, ICE_MOUNTAINS = 13, BEACH = 16,
        DESERT_HILLS = 17, FOREST_HILLS = 18, TAIGA_HILLS = 19, MOUNTAIN_EDGE = 20, JUNGLE = 21, JUNGLE_HILLS = 22,
        DESERT_RIVER = 23, JUNGLE_RIVER = 24, SWAMP_RIVER = 25;
    private static final int[] LAND_BIOMES = {DESERT, FOREST, EXTREME_HILLS, SWAMP, PLAINS, TAIGA, JUNGLE};
    private final Layer terrain;
    private final Layer blocks;

    OriginalOverworldBiomes(long seed, boolean largeBiomes) {
        int zooms = largeBiomes ? 6 : 4;
        Layer island = cells(null, seed, 1, 0, (cell, input, index, stride, x, z) ->
            x == 0 && z == 0 || cell.next(10) == 0 ? 1 : 0);
        island = growIslands(zoom(island, seed, 2000, true), seed, 1);
        island = growIslands(zoom(island, seed, 2001, false), seed, 2);
        island = cells(island, seed, 2, 1, (cell, input, index, stride, x, z) ->
            input[index] == OCEAN ? OCEAN : cell.next(5) == 0 ? ICE_PLAINS : 1);
        island = growIslands(zoom(island, seed, 2002, false), seed, 3);
        island = growIslands(zoom(island, seed, 2003, false), seed, 4);
        // akh samples a padded tile and returns each center, including the ocean case.
        island = cells(island, seed, 5, 1, (cell, input, index, stride, x, z) -> input[index]);

        Layer rivers = cells(island, seed, 100, 0, (cell, input, index, stride, x, z) ->
            input[index] > 0 ? cell.next(2) + 2 : 0);
        for (int step = 0; step < zooms + 2; step++) rivers = zoom(rivers, seed, 1000 + step, false);
        rivers = cells(rivers, seed, 1, 1, (cell, input, index, stride, x, z) -> {
            int center = input[index];
            return center != 0 && neighborsMatch(input, index, stride, center) ? -1 : RIVER;
        });
        Layer riverMask = smooth(rivers, seed);

        Layer biomes = cells(island, seed, 200, 0, (cell, input, index, stride, x, z) -> {
            if (input[index] == OCEAN) return OCEAN;
            int chosen = LAND_BIOMES[cell.next(LAND_BIOMES.length)];
            return input[index] == 1 || chosen == TAIGA ? chosen : ICE_PLAINS;
        });
        biomes = zoom(zoom(biomes, seed, 1000, false), seed, 1001, false);
        biomes = cells(biomes, seed, 1000, 1, (cell, input, index, stride, x, z) -> {
            int center = input[index];
            if (cell.next(3) != 0 || !neighborsMatch(input, index, stride, center)) return center;
            return switch (center) {
                case DESERT -> DESERT_HILLS;
                case FOREST -> FOREST_HILLS;
                case TAIGA -> TAIGA_HILLS;
                case PLAINS -> FOREST;
                case ICE_PLAINS -> ICE_MOUNTAINS;
                case JUNGLE -> JUNGLE_HILLS;
                default -> center;
            };
        });
        for (int step = 0; step < zooms; step++) {
            biomes = zoom(biomes, seed, 1000 + step, false);
            if (step == 0) biomes = growIslands(biomes, seed, 3);
            if (step == 1) {
                biomes = cells(biomes, seed, 1000, 1, OriginalOverworldBiomes::shore);
                biomes = cells(biomes, seed, 1000, 1, (cell, input, index, stride, x, z) -> {
                    int center = input[index];
                    if (center == SWAMP && cell.next(6) == 0) return SWAMP_RIVER;
                    if ((center == JUNGLE || center == JUNGLE_HILLS) && cell.next(8) == 0) return JUNGLE_RIVER;
                    return center;
                });
            }
        }
        Layer land = smooth(biomes, seed);
        terrain = (x, z, width, height) -> {
            int[] selected = land.sample(x, z, width, height);
            int[] river = riverMask.sample(x, z, width, height);
            for (int i = 0; i < selected.length; i++) {
                if (selected[i] != OCEAN && river[i] >= 0) {
                    selected[i] = switch (selected[i]) {
                        case ICE_PLAINS -> FROZEN_RIVER;
                        case DESERT, DESERT_HILLS -> DESERT_RIVER;
                        case JUNGLE, JUNGLE_HILLS -> JUNGLE_RIVER;
                        case SWAMP -> SWAMP_RIVER;
                        default -> river[i];
                    };
                }
            }
            return selected;
        };
        blocks = voronoi(terrain, seed);
    }

    int[] terrain(int quartX, int quartZ, int width, int height) {
        return terrain.sample(quartX, quartZ, width, height);
    }

    int[] blocks(int x, int z, int width, int height) {
        return blocks.sample(x, z, width, height);
    }

    private interface Layer {
        int[] sample(int x, int z, int width, int height);
    }

    private interface Rule {
        int biome(CellRandom random, int[] input, int index, int stride, int x, int z);
    }

    private static Layer cells(Layer parent, long seed, long salt, int border, Rule rule) {
        long layerSeed = layerSeed(seed, salt);
        return (x, z, width, height) -> {
            int[] output = new int[width * height];
            int stride = width + 2 * border;
            int[] input = parent == null ? null : parent.sample(x - border, z - border, stride, height + 2 * border);
            var cell = new CellRandom(layerSeed);
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    cell.reset(x + column, z + row);
                    int index = (row + border) * stride + column + border;
                    output[row * width + column] = rule.biome(cell, input, index, stride, x + column, z + row);
                }
            }
            return output;
        };
    }

    private static Layer growIslands(Layer parent, long seed, long salt) {
        return cells(parent, seed, salt, 1, (cell, input, index, stride, x, z) -> {
            int center = input[index];
            int nw = input[index - stride - 1], ne = input[index - stride + 1];
            int sw = input[index + stride - 1], se = input[index + stride + 1];
            if (center == OCEAN && (nw != OCEAN || ne != OCEAN || sw != OCEAN || se != OCEAN)) {
                int seen = 1;
                int landed = 1;
                if (nw != OCEAN && cell.next(seen++) == 0) landed = nw;
                if (ne != OCEAN && cell.next(seen++) == 0) landed = ne;
                if (sw != OCEAN && cell.next(seen++) == 0) landed = sw;
                if (se != OCEAN && cell.next(seen) == 0) landed = se;
                if (cell.next(3) == 0) return landed;
                return landed == ICE_PLAINS ? FROZEN_OCEAN : OCEAN;
            }
            if (center > OCEAN && (nw == OCEAN || ne == OCEAN || sw == OCEAN || se == OCEAN) && cell.next(5) == 0) {
                return center == ICE_PLAINS ? FROZEN_OCEAN : OCEAN;
            }
            return center;
        });
    }

    private static boolean neighborsMatch(int[] input, int index, int stride, int biome) {
        return input[index - stride] == biome && input[index + stride] == biome
            && input[index - 1] == biome && input[index + 1] == biome;
    }

    private static int shore(CellRandom random, int[] input, int index, int stride, int x, int z) {
        int center = input[index];
        if (center == EXTREME_HILLS) return neighborsMatch(input, index, stride, center) ? center : MOUNTAIN_EDGE;
        if (center == OCEAN || center == RIVER || center == SWAMP) return center;
        return input[index - stride] == OCEAN || input[index + stride] == OCEAN
            || input[index - 1] == OCEAN || input[index + 1] == OCEAN ? BEACH : center;
    }

    private static Layer smooth(Layer parent, long seed) {
        return cells(parent, seed, 1000, 1, (cell, input, index, stride, x, z) -> {
            boolean horizontal = input[index - 1] == input[index + 1];
            boolean vertical = input[index - stride] == input[index + stride];
            if (horizontal && vertical) return cell.next(2) == 0 ? input[index - 1] : input[index - stride];
            if (horizontal) return input[index - 1];
            return vertical ? input[index - stride] : input[index];
        });
    }

    private static Layer zoom(Layer parent, long seed, long salt, boolean fuzzy) {
        long layerSeed = layerSeed(seed, salt);
        return (x, z, width, height) -> {
            int tileX = x >> 1, tileZ = z >> 1;
            int stride = (width >> 1) + 3, rows = (height >> 1) + 3;
            int[] input = parent.sample(tileX, tileZ, stride, rows);
            int[] expanded = new int[stride * rows * 4];
            int line = stride << 1;
            var cell = new CellRandom(layerSeed);
            for (int row = 0; row < rows - 1; row++) {
                for (int column = 0; column < stride - 1; column++) {
                    int index = row * stride + column;
                    int nw = input[index], ne = input[index + 1], sw = input[index + stride], se = input[index + stride + 1];
                    cell.reset((tileX + column) << 1, (tileZ + row) << 1);
                    int pixel = (row << 1) * line + (column << 1);
                    expanded[pixel] = nw;
                    expanded[pixel + line] = cell.next(2) == 0 ? nw : sw;
                    expanded[pixel + 1] = cell.next(2) == 0 ? nw : ne;
                    expanded[pixel + line + 1] = zoomCorner(cell, nw, ne, sw, se, fuzzy);
                }
            }
            return crop(expanded, line, x & 1, z & 1, width, height);
        };
    }

    private static int zoomCorner(CellRandom cell, int nw, int ne, int sw, int se, boolean fuzzy) {
        if (!fuzzy) {
            if (ne == sw && sw == se) return ne;
            if (nw == ne && (nw == sw || nw == se || sw != se)) return nw;
            if (nw == sw && (nw == se || ne != se)) return nw;
            if (nw == se && ne != sw) return nw;
            if (ne == sw && nw != se || ne == se && nw != sw) return ne;
            if (sw == se && nw != ne) return sw;
        }
        return switch (cell.next(4)) {
            case 0 -> nw;
            case 1 -> ne;
            case 2 -> sw;
            default -> se;
        };
    }

    private static Layer voronoi(Layer parent, long seed) {
        long layerSeed = layerSeed(seed, 10);
        return (x, z, width, height) -> {
            int tileX = (x - 2) >> 2, tileZ = (z - 2) >> 2;
            int stride = (width >> 2) + 3, rows = (height >> 2) + 3;
            int[] input = parent.sample(tileX, tileZ, stride, rows);
            int line = stride << 2;
            int[] expanded = new int[line * (rows << 2)];
            var cell = new CellRandom(layerSeed);
            double[] px = new double[4], pz = new double[4], distance = new double[4];
            int[] samples = new int[4];
            for (int row = 0; row < rows - 1; row++) {
                for (int column = 0; column < stride - 1; column++) {
                    for (int corner = 0; corner < 4; corner++) {
                        int dx = corner & 1, dz = corner >> 1;
                        cell.reset((tileX + column + dx) << 2, (tileZ + row + dz) << 2);
                        px[corner] = (cell.next(1024) / 1024.0 - .5) * 3.6 + dx * 4;
                        pz[corner] = (cell.next(1024) / 1024.0 - .5) * 3.6 + dz * 4;
                        samples[corner] = input[(row + dz) * stride + column + dx];
                    }
                    for (int dz = 0; dz < 4; dz++) {
                        for (int dx = 0; dx < 4; dx++) {
                            for (int corner = 0; corner < 4; corner++) {
                                double offsetX = dx - px[corner], offsetZ = dz - pz[corner];
                                distance[corner] = offsetZ * offsetZ + offsetX * offsetX;
                            }
                            expanded[((row << 2) + dz) * line + (column << 2) + dx] = samples[nearest(distance)];
                        }
                    }
                }
            }
            return crop(expanded, line, (x - 2) & 3, (z - 2) & 3, width, height);
        };
    }

    private static int nearest(double[] distance) {
        for (int a = 0; a < 3; a++) {
            boolean smallest = true;
            for (int b = 0; b < 4; b++) if (b != a && distance[a] >= distance[b]) smallest = false;
            if (smallest) return a;
        }
        return 3;
    }

    private static int[] crop(int[] expanded, int stride, int x, int z, int width, int height) {
        int[] result = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(expanded, (row + z) * stride + x, result, row * width, width);
        }
        return result;
    }

    private static long advance(long state, long salt) {
        return state * (state * 6364136223846793005L + 1442695040888963407L) + salt;
    }

    private static long layerSeed(long worldSeed, long salt) {
        long base = advance(advance(advance(salt, salt), salt), salt);
        return advance(advance(advance(worldSeed, base), base), base);
    }

    private static final class CellRandom {
        private final long seed;
        private long state;

        private CellRandom(long seed) { this.seed = seed; }

        private void reset(int x, int z) {
            state = advance(advance(advance(advance(seed, x), z), x), z);
        }

        private int next(int bound) {
            int value = Math.floorMod(state >> 24, bound);
            state = advance(state, seed);
            return value;
        }
    }
}
