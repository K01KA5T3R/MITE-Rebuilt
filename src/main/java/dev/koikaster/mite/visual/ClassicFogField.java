package dev.koikaster.mite.visual;

import java.util.ArrayList;
import java.util.Random;

/** R195 virtual fog-post field. It creates no blocks and consumes no game RNG. */
public final class ClassicFogField {
    private static final long TYPE = type();
    private final long seed;
    private boolean ready;
    private int chunkX, chunkZ;
    private int[] posts = new int[0];

    public ClassicFogField(long hashedSeed) { seed = hashedSeed; }

    public static long hashSeed(long seed) {
        var random = new Random(seed);
        random.nextInt();
        return random.nextLong();
    }

    private static long type() {
        var random = new Random(0);
        random.nextInt();
        return random.nextLong();
    }

    public float proximity(double x, double z) {
        int cx = (int)Math.floor(x) >> 4, cz = (int)Math.floor(z) >> 4;
        if (!ready || chunkX != cx || chunkZ != cz) {
            chunkX = cx; chunkZ = cz; ready = true;
            var found = new ArrayList<Integer>();
            var random = new Random(0);
            for (int dx = -65; dx <= 65; dx++) for (int dz = -65; dz <= 65; dz++) {
                int px = cx + dx, pz = cz + dz;
                int hash = (17 * 31 + px) * 31 + pz;
                random.setSeed((long)hash * TYPE * seed);
                random.nextInt();
                if (random.nextInt(4828) == 0) { found.add(px); found.add(pz); }
            }
            posts = new int[found.size()];
            for (int i = 0; i < posts.length; i++) posts[i] = found.get(i);
        }
        float nearest = 1024;
        for (int i = 0; i < posts.length; i += 2) {
            double dx = x - posts[i] * 16, dz = z - posts[i + 1] * 16;
            float distance = (float)Math.sqrt(dx * dx + dz * dz);
            if (distance < nearest) nearest = distance;
        }
        return nearest >= 1024 ? 0 : 1 - nearest / 1024;
    }

    /** The source applies the minimum24 after proximity relief, before daily weather fog. */
    public static float beforeWeather(float distance, double height, float sky, float proximity) {
        double visibility = Math.max(0, sky / 16d + (height + 4d) / 20d);
        double power = 1 + (16d - height) / 2d;
        if (power > 1) visibility = Math.pow(visibility, power);
        float far = Math.min(distance, Math.max(5, 100 * (float)visibility));
        if (far < distance) far = (float)(far / Math.max(Math.sqrt(proximity), .01f));
        return Math.max(24, far);
    }
}
