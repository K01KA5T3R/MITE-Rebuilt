package dev.koikaster.mite.visual;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;

/** Select the original fire layer order only for the ordinary first-person screen effects. */
public final class ClassicScreenOverlay {
    public static boolean earlyFire(Object actor) {
        if (!ClassicVisuals.firstPerson() || !(actor instanceof LocalPlayer player)) return false;
        return player.getEyeInFluidType().isAir() || player.isEyeInFluid(FluidTags.WATER);
    }

    private ClassicScreenOverlay() {}
}
