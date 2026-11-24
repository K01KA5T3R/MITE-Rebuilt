package dev.koikaster.mite.visual;

/** Source65x65 biome search. The view supplies loaded columns and their climate. */
public final class ClassicFogBiomes {
    public interface View {
        boolean loaded(int x, int z);
        boolean supports(int x, int z);
    }

    private final View view;
    private final boolean[] support = new boolean[65 * 65];
    private boolean ready, any;
    private int lastX, lastZ;

    public ClassicFogBiomes(View view) { this.view = view; }

    public double distance(double x, double z) {
        int bx = (int)Math.floor(x), bz = (int)Math.floor(z);
        if (view.supports(bx, bz)) {
            // R195 marks the current world present without refreshing the dry-cell mask.
            ready = true;
            double dx = bx + .5 - x, dz = bz + .5 - z;
            return Math.max((double)(float)Math.sqrt(dx * dx + dz * dz) - .7071, 0);
        }
        if (!ready || lastX != bx || lastZ != bz) {
            lastX = bx; lastZ = bz; ready = true; any = false;
            int index = -1;
            for (int dx = -32; dx <= 32; dx++) for (int dz = -32; dz <= 32; dz++) {
                boolean wet = view.loaded(bx + dx, bz + dz) && view.supports(bx + dx, bz + dz);
                support[++index] = wet;
                any |= wet;
            }
        }
        if (!any) return 10000;
        boolean found = false;
        double nearest = 0;
        int index = -1;
        for (int dx = -32; dx <= 32; dx++) for (int dz = -32; dz <= 32; dz++) {
            if (!support[++index]) continue;
            double xDelta = (double)((float)(bx + dx) + .5f) - x;
            double zDelta = (double)((float)(bz + dz) + .5f) - z;
            double squared = xDelta * xDelta + zDelta * zDelta;
            if (!found || squared < nearest) { found = true; nearest = squared; }
        }
        return found ? Math.max((double)(float)Math.sqrt(nearest) - .7071, 0) : 10000;
    }
}
