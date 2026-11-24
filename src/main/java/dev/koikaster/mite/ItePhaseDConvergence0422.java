package dev.koikaster.mite;

import net.minecraft.world.level.Level;

/**
 * Phase D Round 3 (0.4.22) cleanroom convergence summary anchor & balance baseline
 * handoff constants prior to Phase E human playtest and boss difficulty tuning.
 */
public final class ItePhaseDConvergence0422 {
    public static final int R196_CLASSES_CLOSED = 56;
    public static final int ITE_CLASSES_CLOSED = 254;
    public static final String PHASE_D_TAG = "PHASE_D_CONVERGED_0422";

    public static String convergenceSummary(Level level) {
        if (level == null) return PHASE_D_TAG + ":null";
        var s = WorldContentMode.settings(level);
        return PHASE_D_TAG + ":flags=" + s.encode() + ",r196=" + s.r196() + ",ite=" + s.ite() + ",extras=" + s.extras();
    }

    private ItePhaseDConvergence0422() {}
}
