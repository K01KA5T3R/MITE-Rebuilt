package dev.koikaster.mite.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.koikaster.mite.MiteReforged;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;

/** Original GL_LINEAR fog and eye-distance convention, scoped to the world pass. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID, value=Dist.CLIENT)
public final class ClassicAirFogShader {
    private static final Map<ShaderInstance, Binding> BINDINGS = new WeakHashMap<>();
    private static boolean world;

    @SubscribeEvent
    public static void world(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            world = false;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            var mc = Minecraft.getInstance();
            var camera = event.getCamera();
            var entity = camera.getEntity();
            world = ClassicVisualState.ready && ClassicVisuals.fog() && mc.level != null
                && mc.player != null && camera.getClass() == Camera.class && entity == mc.player
                && !entity.isSpectator() && !ClassicFog.protectedVision(entity)
                && camera.getFluidInCamera() == FogType.NONE
                && ClassicFog.knownDimension(mc.level.dimension());
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void hand(RenderHandEvent event) { world = false; }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void gui(RenderGuiEvent.Pre event) { world = false; }

    /** Optional uniforms are written only after the owning shader is bound. */
    public static void applied(Object value) {
        if (!(value instanceof ShaderInstance shader) || !RenderSystem.isOnRenderThread()) return;
        int program = shader.getId();
        if (program <= 0) return;
        var binding = BINDINGS.computeIfAbsent(shader, key -> new Binding(program));
        if (binding.airLocation < 0) return;
        int current = world && ClassicVisuals.fog() ? 1 : 0;
        if (binding.lastAir != current) {
            GL20.glUniform1i(binding.airLocation, current);
            binding.lastAir = current;
        }
        int radial = GL.getCapabilities().GL_NV_fog_distance ? 1 : 0;
        if (binding.radialLocation >= 0 && binding.lastRadial != radial) {
            GL20.glUniform1i(binding.radialLocation, radial);
            binding.lastRadial = radial;
        }
    }

    private static final class Binding {
        final int airLocation, radialLocation;
        int lastAir = -1, lastRadial = -1;
        Binding(int program) {
            airLocation = GL20.glGetUniformLocation(program, "MiteClassicAirFog");
            radialLocation = GL20.glGetUniformLocation(program, "MiteClassicAirRadial");
        }
    }

    private ClassicAirFogShader() {}
}
