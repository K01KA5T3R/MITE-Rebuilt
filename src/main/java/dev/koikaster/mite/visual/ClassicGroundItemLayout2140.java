package dev.koikaster.mite.visual;

import java.util.Random;
import net.minecraft.util.Mth;

/** R195 bgw ground-item count, placement and rotation. */
final class ClassicGroundItemLayout2140 {
    static final float ICON_SCALE = .5f;
    static final float VOLUME_SCALE = .25f;
    static final float ICON_DEPTH_STEP = .0625f + .021875f;

    static int copies(int count, boolean fancyIcon) {
        if (fancyIcon) {
            if (count < 2) return 1;
            if (count < 16) return 2;
            return count < 32 ? 3 : 4;
        }
        if (count > 40) return 5;
        if (count > 20) return 4;
        if (count > 5) return 3;
        return count > 1 ? 2 : 1;
    }

    static float bob(int age, float partialTick, float phase) {
        return Mth.sin((age + partialTick) / 10f + phase) * .1f + .1f;
    }

    static float spin(int age, float partialTick, float phase) {
        return ((age + partialTick) / 20f + phase) * 57.295776f;
    }

    static float spread(Random random, boolean volume) {
        float signed = random.nextFloat() * 2f - 1f;
        return volume ? signed * .2f / VOLUME_SCALE : signed * .3f;
    }

    private ClassicGroundItemLayout2140() {}
}

