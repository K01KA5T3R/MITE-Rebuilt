package dev.koikaster.mite.visual;

/** Original terrain ranges after void-fog adjustment, before native fog reduction. */
final class ClassicFogRanges {
    static float[] terrain(float distance, int dimension, int eyeY) {
        float far = dimension == -2 ? 128 : distance;
        boolean foggy = dimension == 1 || dimension == -1 && eyeY < 128;
        return foggy ? new float[]{far * .05f, Math.min(far, 192) * .5f}
                     : new float[]{far * .25f, far};
    }

    private ClassicFogRanges() {}
}
