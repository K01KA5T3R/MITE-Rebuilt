package dev.koikaster.mite;

import java.util.Random;

/** R195 bfe.g samples particles and the rain sound in the same random sequence. */
final class LegacyRainSpawn2160 {
    private static final int RADIUS = 10;

    enum Surface { CUBE, PARTIAL, LIQUID, LAVA, OPEN_TRAPDOOR, NATIVE_HOT }

    record Column(int height, Surface surface, double minY, double maxY, int liquidLevel) {
        private double particleY() {
            return switch (surface) {
                case LIQUID, LAVA -> liquidSurface(height, liquidLevel) + .1F + .125F;
                case CUBE -> (float)height + .1F;
                default -> (double)(height - 1) + maxY + (double).1F;
            };
        }
    }

    record Impact(double x, double y, double z) {
        boolean above(double viewerY, int coverHeight) {
            return y > viewerY + 1 && coverHeight > Math.floor(viewerY);
        }
    }

    record SoundTick(int counter, Impact impact) {}

    @FunctionalInterface
    interface Columns {
        Column find(int x, int z, int minimumHeight);
    }

    @FunctionalInterface
    interface Particles {
        void add(boolean smoke, double x, double y, double z);
    }

    static float liquidSurface(int height, int metadata) {
        return (float)height - ((metadata >= 8 ? 0 : metadata) + 1) / 9F;
    }

    static int attempts(float strength, boolean fancy, int density) {
        if (!fancy) strength /= 2F;
        int count = (int)(100F * strength * strength);
        return density == 2 ? 0 : density == 1 ? count >> 1 : count;
    }

    static SoundTick sample(int ticks, int x, int y, int z, int attempts, int counter, Columns world, Particles particles) {
        if (attempts == 0) return new SoundTick(counter, null);
        var random = new Random((long)ticks * 312987231L);
        int ordinary = 0;
        double soundX = 0, soundY = 0, soundZ = 0;
        for (int i = 0; i < attempts; i++) {
            int sampleX = x + random.nextInt(RADIUS) - random.nextInt(RADIUS);
            int sampleZ = z + random.nextInt(RADIUS) - random.nextInt(RADIUS);
            var column = world.find(sampleX, sampleZ, y - RADIUS);
            if (column == null || column.height() < y - RADIUS || column.height() > y + RADIUS) continue;
            float drawX = random.nextFloat();
            float drawZ = random.nextFloat();
            var surface = column.surface();
            if (surface == Surface.OPEN_TRAPDOOR) continue;
            double particleX = (float)sampleX + drawX;
            double particleZ = (float)sampleZ + drawZ;
            if (surface != Surface.LAVA && random.nextInt(++ordinary) == 0) {
                soundX = particleX;
                soundY = (double)((float)column.height() + .1F) - column.minY();
                soundZ = particleZ;
            }
            if (surface != Surface.NATIVE_HOT) {
                particles.add(surface == Surface.LAVA, particleX, column.particleY(), particleZ);
            }
        }
        if (ordinary > 0 && random.nextInt(3) < counter++) {
            return new SoundTick(0, new Impact(soundX, soundY, soundZ));
        }
        return new SoundTick(counter, null);
    }

    private LegacyRainSpawn2160() {}
}
