package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Original first-person view translation, on world/culling copies only. */
public final class ClassicCameraAlignment {
    public static Object worldPose(Object value) {
        if (!(value instanceof PoseStack source) || !ClassicVisualState.ready) return value;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ClassicVisuals.firstPerson()) return value;
        var camera = mc.gameRenderer.getMainCamera();
        // Native clear() only tests that the stack is at its base depth.
        if (source.getClass() != PoseStack.class || camera.getClass() != Camera.class || camera.getEntity() != mc.player || camera.isDetached()
                || !mc.options.getCameraType().isFirstPerson() || mc.player.isSleeping()
                || mc.player.isSpectator() || mc.player.isScoping() || !source.clear()) return value;
        var copy = new PoseStack();
        copy.last().pose().set(source.last().pose());
        copy.last().normal().set(source.last().normal());
        offset(copy.last().pose(), camera.getXRot(), camera.getYRot());
        return copy;
    }

    /** P=base*roll*pitch*yaw. Insert T after roll without assuming an identity base. */
    static void offset(Matrix4f view, float pitch, float yaw) {
        float radians = (float)Math.PI / 180;
        var delta = new Vector3f(0, 0, -.1f).rotateX(-pitch * radians).rotateY(-(yaw + 180) * radians);
        view.translate(delta);
    }

    private ClassicCameraAlignment() {}
}
