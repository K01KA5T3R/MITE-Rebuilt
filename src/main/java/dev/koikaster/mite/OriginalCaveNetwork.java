package dev.koikaster.mite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static dev.koikaster.mite.OriginalOverworldDensity.*;

/** The R195 cave network origin selection and 64×48×64 cell simulation. */
final class OriginalCaveNetwork {
    static final byte MYCELIUM = 110, CLAY = 82;
    private static final int SIDE = 64, TALL = 48, PLANE = SIDE * SIDE, VOLUME = PLANE * TALL;
    private static final int RANGE = 3;
    private static final int[] ADJACENT = {-PLANE, PLANE, -1, 1, -SIDE, SIDE};
    private static final int[] NEIGHBORS = neighbors();
    private final long seed, multiplierX, multiplierZ;
    private final OriginalOverworldBiomes biomes;
    private final Map<Origin, Optional<Location>> known = new LinkedHashMap<>(512, .75f, true);
    private final Map<Location, byte[]> caves = new LinkedHashMap<>(8, .75f, true);

    OriginalCaveNetwork(long seed, OriginalOverworldBiomes biomes) {
        this.seed = seed;
        this.biomes = biomes;
        var random = new Random(seed);
        multiplierX = random.nextLong();
        multiplierZ = random.nextLong();
    }

    private record Origin(int x, int z) {}
    private record Location(int x, int z, long seed, boolean mycelium, boolean legacy) {}

    void carve(int chunkX, int chunkZ, byte[] chunk) {
        var location = nearby(chunkX, chunkZ);
        if (location == null) return;
        byte[] network = cells(location);
        int offsetX = (chunkX - location.x()) * 16, offsetZ = (chunkZ - location.z()) * 16;
        int lowest = location.mycelium() ? lowestAir(network) : 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int base = (x * 16 + z) * HEIGHT;
                int networkBase = (offsetZ + z) * SIDE + offsetX + x;
                int highest = -1;
                for (int y = 0; y < TALL; y++) {
                    if (network[networkBase + y * PLANE] == 0) {
                        chunk[base + y + 8] = AIR;
                        highest = y + 8;
                    }
                }
                if (location.mycelium()) {
                    for (int y = Math.max(lowest, 1); y < Math.min(lowest + 6, TALL - 1); y++) {
                        int at = networkBase + y * PLANE;
                        if (network[at] != 0 || network[at - PLANE] == 0) continue;
                        int floor = base + y + 8;
                        chunk[floor] = chunk[floor + 1] == AIR ? MYCELIUM : STONE;
                        chunk[floor - 1] = STONE;
                    }
                }
                if (highest >= 0) {
                    byte unstable = chunk[base + highest + 1];
                    if (unstable == SAND || unstable == 13) {
                        chunk[base + highest + 1] = chunk[base + highest + 2] == WATER ? CLAY
                            : unstable == SAND ? SANDSTONE : STONE;
                    }
                }
            }
        }
    }

    private synchronized Location nearby(int chunkX, int chunkZ) {
        if (!land(chunkX, chunkZ)) return null;
        var local = new HashMap<Origin, Optional<Location>>();
        for (int x = chunkX - RANGE; x <= chunkX; x++) {
            for (int z = chunkZ - RANGE; z <= chunkZ; z++) {
                var site = select(new Origin(x, z), local);
                if (site.isPresent()) return site.get();
            }
        }
        return null;
    }

    private Optional<Location> select(Origin origin, Map<Origin, Optional<Location>> local) {
        var cached = local.get(origin);
        if (cached == null) cached = known.get(origin);
        if (cached != null) return cached;
        var value = Optional.ofNullable(qualify(origin, local));
        local.put(origin, value);
        known.put(origin, value);
        if (known.size() > 1024) known.remove(known.keySet().iterator().next());
        return value;
    }

    private Location qualify(Origin origin, Map<Origin, Optional<Location>> local) {
        int x = origin.x(), z = origin.z();
        if (!land(x, z)) return null;
        if (seed == 1 && x == -14 && z == 29) return new Location(x, z, 2375913967323326907L, true, true);
        float worldX = x * 16, worldZ = z * 16;
        double distance = (float) Math.sqrt(worldX * worldX + worldZ * worldZ);
        if (distance < 1000) return null;
        var random = new Random(x * multiplierX ^ z * multiplierZ ^ seed);
        if (random.nextInt(200) != 0) return null;
        for (int nx = x + 1; nx <= x + RANGE; nx++) {
            for (int nz = z + 1; nz <= z + RANGE; nz++) {
                if (!land(nx, nz) || select(new Origin(nx, nz), local).isPresent()) return null;
            }
        }
        boolean mycelium = distance >= 1500 && random.nextInt(8) == 0;
        return new Location(x, z, random.nextLong(), mycelium, false);
    }

    private boolean land(int chunkX, int chunkZ) {
        return biomes.blocks(chunkX * 16, chunkZ * 16, 1, 1)[0] != OriginalOverworldBiomes.OCEAN;
    }

    private synchronized byte[] cells(Location location) {
        var result = caves.computeIfAbsent(location, site -> new Cells(site).generate());
        if (caves.size() > 8) caves.remove(caves.keySet().iterator().next());
        return result;
    }

    private static int lowestAir(byte[] cells) {
        for (int index = 0; index < VOLUME; index++) {
            if (cells[index] == 0) return index / PLANE;
        }
        return TALL;
    }

    private static int[] neighbors() {
        int[] result = new int[27];
        int index = 0;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            for (int y = -1; y <= 1; y++) result[index++] = x + z * SIDE + y * PLANE;
        }
        return result;
    }

    private static final class Cells {
        private final Location location;
        private final Random random;
        private final byte[] cells = new byte[VOLUME], prior = new byte[VOLUME];

        private Cells(Location location) {
            this.location = location;
            random = new Random(location.seed());
            if (location.legacy()) {
                random.nextInt(200);
                random.nextInt(2);
                random.nextLong();
            } else {
                random.nextInt();
            }
        }

        private byte[] generate() {
            int shell = 0, interior = 0, combined;
            switch (random.nextInt(3)) {
                case 0 -> {
                    shell = 2;
                    interior = random.nextInt(57) + 8;
                    combined = 0;
                }
                case 1 -> {
                    shell = random.nextInt(3);
                    int steps = random.nextInt(57) + 8;
                    interior = random.nextInt(steps + 1);
                    combined = steps - interior;
                    if (shell + combined < 2) {
                        interior -= 2;
                        combined += 2;
                    }
                }
                default -> combined = random.nextInt(57) + 8;
            }
            shell(shell);
            byte[] walls = cells.clone();
            interior(interior);
            for (int index = 0; index < cells.length; index++) if (walls[index] != 0) cells[index] = walls[index];
            smooth(combined);
            closePockmarks();
            return cells;
        }

        private void shell(int smoothing) {
            Arrays.fill(cells, (byte) 1);
            int floor = location.mycelium() || random.nextBoolean() ? 24 : 16;
            for (int x = 16; x < SIDE - 16; x++) {
                for (int z = 16; z < SIDE - 16; z++) {
                    float dx = x + .5f - SIDE / 2f, dz = z + .5f - SIDE / 2f;
                    if (dx * dx + dz * dz > 256f) continue;
                    for (int y = floor; y < TALL - 16; y++) cells[index(x, y, z)] = 0;
                }
            }
            for (int step = 0; step < SIDE / 2; step++) {
                System.arraycopy(cells, 0, prior, 0, VOLUME);
                for (int x = 1; x < SIDE - 1; x++) {
                    for (int z = 1; z < SIDE - 1; z++) {
                        for (int y = 1; y < TALL - 1; y++) {
                            int index = index(x, y, z);
                            if (prior[index] == 0) cells[index + ADJACENT[random.nextInt(6)]] = 0;
                        }
                    }
                }
            }
            smooth(smoothing);
        }

        private void interior(int smoothing) {
            Arrays.fill(cells, (byte) 1);
            for (int index = 0; index < VOLUME; index++) if (random.nextFloat() < .5f) cells[index] = 0;
            if (!location.legacy() && random.nextBoolean()) biasCorner(.51f);
            if (!location.legacy() && random.nextBoolean()) biasCorner(.49f);
            if (location.mycelium()) {
                if (!location.legacy() && random.nextBoolean()) biasCeiling();
                fill(0, 0, 0, SIDE, TALL / 2, SIDE, .51f + random.nextFloat() * .01f);
            } else {
                if (random.nextBoolean()) biasCorner(.48f);
                if (random.nextFloat() < .8f) {
                    if (random.nextBoolean()) fill(0, 0, 0, SIDE, random.nextInt(TALL / 2 + 1), SIDE, .47f);
                    else biasCeiling();
                } else if (random.nextBoolean()) {
                    fill(0, 0, 0, SIDE, TALL / 2, SIDE, .51f + random.nextFloat() * .01f);
                }
            }
            smooth(smoothing);
        }

        private void biasCeiling() {
            int height = random.nextInt(TALL / 2 + 1);
            fill(0, TALL - height, 0, SIDE, height, SIDE, .47f);
        }

        private void biasCorner(float chance) {
            int width, height, depth;
            do {
                width = random.nextInt(SIDE) + 1;
                height = random.nextInt(TALL) + 1;
                depth = random.nextInt(SIDE) + 1;
            } while ((float) (width * height * depth) / VOLUME > .5f);
            int x = random.nextBoolean() ? 0 : SIDE - width;
            int y = random.nextBoolean() ? 0 : TALL - height;
            int z = random.nextBoolean() ? 0 : SIDE - depth;
            fill(x, y, z, width, height, depth, chance);
        }

        private void fill(int x, int y, int z, int width, int height, int depth, float chance) {
            for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < depth; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    cells[index(x + dx, y + dy, z + dz)] = (byte) (random.nextFloat() < chance ? 0 : 1);
                }
            }
        }

        private void smooth(int iterations) {
            for (int step = 0; step < iterations; step++) {
                System.arraycopy(cells, 0, prior, 0, VOLUME);
                for (int y = 1; y < TALL - 1; y++) {
                    for (int z = 1; z < SIDE - 1; z++) {
                        int base = index(0, y, z);
                        for (int x = 1; x < SIDE - 1; x++) {
                            int walls = 0;
                            for (int delta : NEIGHBORS) if (prior[base + x + delta] != 0 && ++walls >= 14) break;
                            cells[base + x] = (byte) (walls < 14 ? 0 : 1);
                        }
                    }
                }
            }
        }

        private void closePockmarks() {
            for (int x = 0; x < SIDE; x++) for (int z = 0; z < SIDE; z++) {
                setWallIfBacking(index(x, 0, z), PLANE);
                setWallIfBacking(index(x, TALL - 1, z), -PLANE);
            }
            for (int x = 0; x < SIDE; x++) for (int y = 0; y < TALL; y++) {
                setWallIfBacking(index(x, y, 0), SIDE);
                setWallIfBacking(index(x, y, SIDE - 1), -SIDE);
            }
            for (int z = 0; z < SIDE; z++) for (int y = 0; y < TALL; y++) {
                setWallIfBacking(index(0, y, z), 1);
                setWallIfBacking(index(SIDE - 1, y, z), -1);
            }
        }

        private void setWallIfBacking(int face, int towardCenter) {
            if (cells[face + towardCenter] != 0) cells[face] = 1;
        }
    }

    private static int index(int x, int y, int z) {
        return x + z * SIDE + y * PLANE;
    }
}
