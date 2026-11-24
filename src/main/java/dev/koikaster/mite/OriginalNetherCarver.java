package dev.koikaster.mite;

import java.util.Random;

import static dev.koikaster.mite.OriginalNetherTerrain.*;

/** R195 aes removes netherrack and soil without opening pockets to existing lava. */
final class OriginalNetherCarver {
    private static final int RANGE = 8;
    private static final int MAX_TUNNEL_LENGTH = (RANGE - 1) * 16;
    private final long seed, xMultiplier, zMultiplier;

    OriginalNetherCarver(long seed) {
        this.seed = seed;
        var random = new Random(seed);
        xMultiplier = random.nextLong();
        zMultiplier = random.nextLong();
    }

    void carve(int chunkX, int chunkZ, byte[] blocks) {
        var random = new Random();
        for (int x = chunkX - RANGE; x <= chunkX + RANGE; x++) {
            for (int z = chunkZ - RANGE; z <= chunkZ + RANGE; z++) {
                random.setSeed(x * xMultiplier ^ z * zMultiplier ^ seed);
                carveFrom(x, z, chunkX * 16, chunkZ * 16, blocks, random);
            }
        }
    }

    private static void carveFrom(int sourceX, int sourceZ, int originX, int originZ, byte[] blocks, Random random) {
        int caves = random.nextInt(random.nextInt(random.nextInt(10) + 1) + 1);
        if (random.nextInt(5) != 0) return;
        for (int cave = 0; cave < caves; cave++) {
            double x = sourceX * 16 + random.nextInt(16);
            double y = random.nextInt(128);
            double z = sourceZ * 16 + random.nextInt(16);
            int tunnels = 1;
            if (random.nextInt(4) == 0) {
                long roomSeed = random.nextLong();
                float size = 1 + random.nextFloat() * 6;
                carvePath(roomSeed, originX, originZ, blocks, x, y, z, size, 0, 0, -1, -1, .5);
                tunnels += random.nextInt(4);
            }
            for (int tunnel = 0; tunnel < tunnels; tunnel++) {
                float yaw = random.nextFloat() * (float) Math.PI * 2;
                float pitch = (random.nextFloat() - .5f) * 2 / 8;
                float size = (random.nextFloat() * 2 + random.nextFloat()) * 2;
                carvePath(random.nextLong(), originX, originZ, blocks, x, y, z, size, yaw, pitch, 0, 0, .5);
            }
        }
    }

    private static void carvePath(long seed, int originX, int originZ, byte[] blocks,
                                  double x, double y, double z, float size, float yaw, float pitch,
                                  int start, int length, double heightScale) {
        var random = new Random(seed);
        if (length <= 0) length = MAX_TUNNEL_LENGTH - random.nextInt(MAX_TUNNEL_LENGTH / 4);
        boolean room = start == -1;
        if (room) start = length / 2;
        int split = random.nextInt(length / 2) + length / 4;
        boolean shallow = random.nextInt(6) == 0;
        float turn = 0, incline = 0;
        for (int step = start; step < length; step++) {
            double radius = 1.5 + SourceDragonTrig.sin(step * (float) Math.PI / length) * size;
            double height = radius * heightScale;
            float cosPitch = SourceDragonTrig.cos(pitch);
            x += SourceDragonTrig.cos(yaw) * cosPitch;
            y += SourceDragonTrig.sin(pitch);
            z += SourceDragonTrig.sin(yaw) * cosPitch;
            pitch *= shallow ? .92f : .7f;
            pitch += incline * .1f;
            yaw += turn * .1f;
            incline *= .9f;
            turn *= .75f;
            incline += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2;
            turn += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4;
            if (!room && step == split && size > 1) {
                carvePath(random.nextLong(), originX, originZ, blocks, x, y, z, random.nextFloat() * .5f + .5f,
                    yaw - (float) (Math.PI / 2), pitch / 3, step, length, 1);
                carvePath(random.nextLong(), originX, originZ, blocks, x, y, z, random.nextFloat() * .5f + .5f,
                    yaw + (float) (Math.PI / 2), pitch / 3, step, length, 1);
                return;
            }
            if (!room && random.nextInt(4) == 0) continue;
            double centerX = x - (originX + 8), centerZ = z - (originZ + 8);
            double remaining = length - step, maxReach = size + 2.0f + 16.0f;
            if (centerX * centerX + centerZ * centerZ - remaining * remaining > maxReach * maxReach) return;
            if (Math.abs(centerX) > 16 + radius * 2 || Math.abs(centerZ) > 16 + radius * 2) continue;
            if (carveSegment(blocks, originX, originZ, x, y, z, radius, height) && room) return;
        }
    }

    private static boolean carveSegment(byte[] blocks, int originX, int originZ,
                                        double x, double y, double z, double radius, double height) {
        int minX = Math.max(0, (int) Math.floor(x - radius) - originX - 1);
        int maxX = Math.min(16, (int) Math.floor(x + radius) - originX + 1);
        int minY = Math.max(1, (int) Math.floor(y - height) - 1);
        int maxY = Math.min(120, (int) Math.floor(y + height) + 1);
        int minZ = Math.max(0, (int) Math.floor(z - radius) - originZ - 1);
        int maxZ = Math.min(16, (int) Math.floor(z + radius) - originZ + 1);

        for (int dx = minX; dx < maxX; dx++) for (int dz = minZ; dz < maxZ; dz++) {
            int base = (dx * 16 + dz) << 7;
            boolean edge = dx == minX || dx == maxX - 1 || dz == minZ || dz == maxZ - 1;
            for (int dy = maxY + 1; dy >= minY - 1; dy--) {
                if (blocks[base + dy] == LAVA || blocks[base + dy] == 10) return false;
                if (!edge && dy != minY - 1) dy = minY;
            }
        }

        for (int dx = minX; dx < maxX; dx++) for (int dz = minZ; dz < maxZ; dz++) {
            double nx = (dx + originX + .5 - x) / radius, nz = (dz + originZ + .5 - z) / radius;
            int index = ((dx * 16 + dz) << 7) + maxY;
            for (int dy = maxY - 1; dy >= minY; dy--, index--) {
                double ny = (dy + .5 - y) / height;
                byte block = blocks[index];
                if (ny > -.7 && nx * nx + ny * ny + nz * nz < 1
                        && (block == NETHERRACK || block == 3 || block == 2)) blocks[index] = AIR;
            }
        }
        return true;
    }
}
