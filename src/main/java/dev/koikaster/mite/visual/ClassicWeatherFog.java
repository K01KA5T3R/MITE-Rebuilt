package dev.koikaster.mite.visual;

import java.util.Random;

/** Original daily fog roll and morning/evening strength, using its exact overflow order. */
public final class ClassicWeatherFog {
    public record Plan(float limit, float cycle) {
        public float apply(float far, double biomeDistance) {
            float factor = cycle * Math.max(1 - (float)(biomeDistance / 32d), 0);
            if (factor <= 0) return far;
            float target = limit * factor + far * (1 - factor);
            return far > target ? target : far;
        }
    }

    public static Plan plan(float far, long time, long creation) {
        int day = (int)((time - 12000L + 6000L) / 24000L) + 1;
        if (far <= 96 || day <= 7) return null;
        var random = new Random((long)(day * 365024131) * creation * 672784657L);
        random.nextInt();
        if (random.nextInt(7) != 0) return null;
        float limit = 96 + random.nextFloat() * (far - 96) * .75f;
        long adjusted = ((int)(time % 24000L) + 6000) % 24000;
        boolean dusk = adjusted > 12000;
        if (dusk) adjusted = 24000L - adjusted;
        float cycle = Math.max(0, Math.min(1, (float)(8000L - adjusted) / (dusk ? 4000f : 2000f)));
        return cycle > 0 ? new Plan(limit, cycle) : null;
    }

    private ClassicWeatherFog() {}
}
