package dev.koikaster.mite.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FogType;

/** The original underwater plane used alpha0.5; keep native geometry and cleanup. */
public final class ClassicWaterOverlay {
    public static float alpha(float nativeAlpha, Object texture) {
        if (!ClassicVisuals.water() || !(texture instanceof ResourceLocation id)
                || !id.getNamespace().equals("minecraft")
                || !id.getPath().equals("textures/misc/underwater.png")) return nativeAlpha;
        var mc = Minecraft.getInstance();
        return mc.level != null && mc.gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER
                ? .5f : nativeAlpha;
    }

    private ClassicWaterOverlay() {}
}
