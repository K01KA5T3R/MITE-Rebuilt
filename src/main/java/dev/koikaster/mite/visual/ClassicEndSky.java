package dev.koikaster.mite.visual;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

/** R195 draws the End sky even while the dragon's environment effect is active. */
public final class ClassicEndSky {
    public static boolean foggy(boolean original, Object value) {
        if (!ClassicVisualState.ready || !ClassicVisuals.sky()
                || !(value instanceof Camera camera)) return original;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != Level.END
                || mc.level.effects().getClass() != DimensionSpecialEffects.EndEffects.class
                || mc.levelRenderer.getClass() != LevelRenderer.class
                || mc.gameRenderer.getClass() != GameRenderer.class
                || camera.getClass() != Camera.class || camera.getEntity() != mc.player
                || mc.player.isSpectator() || camera.getFluidInCamera() != FogType.NONE
                || ClassicFog.protectedVision(mc.player)) return original;
        if (ClassicAirFog.distanceIndex(mc.options.getEffectiveRenderDistance()) >= 2) return true;
        return original && !mc.gui.getBossOverlay().shouldCreateWorldFog();
    }

    private ClassicEndSky() {}
}
