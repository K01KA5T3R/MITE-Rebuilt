package dev.koikaster.mite.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.koikaster.mite.MiteReforged;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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

/** Original fixed-function liquid fog, scoped to world rendering. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID, value=Dist.CLIENT)
public final class ClassicWaterFogShader {
    private static final Map<ShaderInstance, Binding> BINDINGS = new WeakHashMap<>();
    private static FogType medium = FogType.NONE;
    private static float density;

    @SubscribeEvent
    public static void world(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            clear();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            clear();
            var mc = Minecraft.getInstance();
            var camera = event.getCamera();
            var entity = camera.getEntity();
            if (mc.level == null || entity == null || entity.isSpectator()
                    || ClassicFog.protectedVision(entity) || !ClassicFog.knownDimension(mc.level.dimension())) return;

            var fluid = camera.getFluidInCamera();
            if (fluid == FogType.LAVA && ClassicVisualState.ready && ClassicVisuals.fog()) {
                medium = FogType.LAVA;
                density = 2.0f;
                return;
            }
            if (fluid != FogType.WATER || !ClassicVisuals.water()) return;
            int respiration = entity instanceof LivingEntity living ? EnchantmentHelper.getRespiration(living) : 0;
            boolean breathing = entity instanceof LivingEntity living && living.hasEffect(MobEffects.WATER_BREATHING);
            medium = FogType.WATER;
            density = breathing ? .05f : Math.max(.01f, .1f - respiration * .03f);
        }
    }

    private static void clear() {
        medium = FogType.NONE;
        density = 0;
    }

    // R195 disables GL_FOG before rendering the first-person hand.
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void hand(RenderHandEvent event) { clear(); }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void gui(RenderGuiEvent.Pre event) { clear(); }

    static boolean available() {
        if (!RenderSystem.isOnRenderThread()) return false;
        var shader = net.minecraft.client.renderer.GameRenderer.getRendertypeSolidShader();
        return shader != null && shader.getId() > 0
                && BINDINGS.computeIfAbsent(shader, key -> new Binding(key.getId())).densityLocation >= 0;
    }

    /** Called only after ShaderInstance.apply has bound its own program and uniforms. */
    public static void applied(Object value) {
        if (!(value instanceof ShaderInstance shader) || !RenderSystem.isOnRenderThread()) return;
        int program = shader.getId();
        if (program <= 0) return;
        var binding = BINDINGS.computeIfAbsent(shader, key -> new Binding(program));
        if (binding.densityLocation < 0) return; // Foreign/native includes have no custom uniform.
        float current = switch (medium) {
            case LAVA -> ClassicVisualState.ready && ClassicVisuals.fog() ? density : 0;
            case WATER -> ClassicVisuals.water() ? density : 0;
            default -> 0;
        };
        if (Float.compare(binding.lastDensity, current) != 0) {
            GL20.glUniform1f(binding.densityLocation, current);
            binding.lastDensity = current;
        }
        int radial = GL.getCapabilities().GL_NV_fog_distance ? 1 : 0;
        if (binding.radialLocation >= 0 && binding.lastRadial != radial) {
            GL20.glUniform1i(binding.radialLocation, radial);
            binding.lastRadial = radial;
        }
    }

    private static final class Binding {
        final int densityLocation, radialLocation;
        float lastDensity = Float.NaN;
        int lastRadial = -1;
        Binding(int program) {
            densityLocation = GL20.glGetUniformLocation(program, "MiteClassicWaterDensity");
            radialLocation = GL20.glGetUniformLocation(program, "MiteClassicWaterRadial");
        }
    }

    private ClassicWaterFogShader() {}
}
