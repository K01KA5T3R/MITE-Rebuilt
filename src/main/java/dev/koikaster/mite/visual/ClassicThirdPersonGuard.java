package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.koikaster.mite.ClassicBlocking;
import dev.koikaster.mite.MiteReforged;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ClassicThirdPersonGuard {
    private static final Set<String> ORIGINAL_SWORDS = Set.of("iron_sword", "gold_sword", "copper_sword",
        "silver_sword", "mithril_sword", "adamantium_sword", "rusted_iron_sword", "ancient_metal_sword");
    private static final ItemDisplayContext DISPLAY = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    private static final Map<BakedModel, Boolean> STANDARD_MODELS = new WeakHashMap<>();

    // The right-hand display object inherited from 1.20.1's item/handheld.
    private static final Matrix4f HANDHELD = new Matrix4f().translation(0, 4 / 16f, .5f / 16f)
        .rotateXYZ(0, radians(-90), radians(55)).scale(.85f);
    private static final Matrix4f NATIVE_INVERSE = new Matrix4f().rotateX(radians(-90)).rotateY(radians(180))
        .translate(1 / 16f, .125f, -.625f).mul(HANDHELD).translate(-.5f, -.5f, -.5f).invert();
    private static final Grip IDLE = correction(false);
    private static final Grip BLOCK = correction(true);

    private ClassicThirdPersonGuard() {}

    public static void apply(Object actor, Object held, Object arm, Object poses) {
        if (!ClassicVisuals.entities() || !(actor instanceof AbstractClientPlayer player)
                || !(held instanceof ItemStack stack) || !(arm instanceof HumanoidArm hand)
                || !(poses instanceof PoseStack pose)
                || (player.getPose() != Pose.STANDING && player.getPose() != Pose.CROUCHING)) return;

        boolean blocking = isBlocking(player, stack, hand);
        if (usesClassicSwordGrip(player, stack, hand)) {
            (blocking ? BLOCK : IDLE).apply(pose);
        } else if (blocking) {
            applyGuardOffset(pose, hand);
        }
    }

    private static boolean isBlocking(AbstractClientPlayer player, ItemStack stack, HumanoidArm hand) {
        if (!player.isUsingItem() || player.getUseItem() != stack || !ClassicBlocking.handled(stack)) return false;
        HumanoidArm active = player.getUsedItemHand() == InteractionHand.MAIN_HAND
            ? player.getMainArm() : player.getMainArm().getOpposite();
        return hand == active;
    }

    private static boolean usesClassicSwordGrip(AbstractClientPlayer player, ItemStack stack, HumanoidArm hand) {
        if (hand != HumanoidArm.RIGHT || player.getMainArm() != HumanoidArm.RIGHT || player.getMainHandItem() != stack
                || !player.getOffhandItem().isEmpty() || player.isFallFlying() || player.isAutoSpinAttack()
                || player.getSwimAmount(0) > 0 || player.getSwimAmount(1) > 0 || !(stack.getItem() instanceof SwordItem)) return false;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(MiteReforged.ID) || !ORIGINAL_SWORDS.contains(id.getPath())
                || IClientItemExtensions.of(stack) != IClientItemExtensions.DEFAULT) return false;

        var renderer = Minecraft.getInstance().getItemRenderer();
        var model = renderer.getModel(stack, player.level(), player, player.getId() + DISPLAY.ordinal());
        return model.getClass() == SimpleBakedModel.class && !model.isGui3d() && !model.isCustomRenderer()
            && STANDARD_MODELS.computeIfAbsent(model, baked -> hasStandardGeometry(baked, stack));
    }

    private static boolean hasStandardGeometry(BakedModel model, ItemStack stack) {
        // Respect an item's own display or custom geometry from resource packs.
        var displayPose = new PoseStack();
        model.getTransforms().getTransform(DISPLAY).apply(false, displayPose);
        return HANDHELD.equals(displayPose.last().pose(), .000001f)
            && ClassicFlatItemRenderer.prepare(model, stack) != null;
    }

    private static void applyGuardOffset(PoseStack pose, HumanoidArm hand) {
        // Retain the existing grip-relative adjustment for other guard models and hands.
        int side = hand == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(-side / 16f, 7 / 16f, 1 / 16f);
        pose.translate(side * .05f, 0, -.10f);
        pose.mulPose(Axis.YP.rotationDegrees(-50 * side));
        pose.mulPose(Axis.XP.rotationDegrees(-10));
        pose.mulPose(Axis.ZP.rotationDegrees(-60 * side));
        pose.translate(side / 16f, -7 / 16f, -1 / 16f);
    }

    private static Grip correction(boolean guarding) {
        // bhj's held sword, measured from the posed right arm.
        var transform = new Matrix4f().translation(-1 / 16f, 7 / 16f, 1 / 16f);
        if (guarding) {
            transform.translate(.05f, 0, -.10f).rotateY(radians(-50)).rotateX(radians(-10)).rotateZ(radians(-60));
        }
        transform.translate(0, 3 / 16f, 0).scale(.625f, -.625f, .625f)
            .rotateX(radians(-100)).rotateY(radians(45));

        // bfj's extrusion uses a reversed U axis and starts its front face at Z=0.
        transform.translate(0, -.3f, 0).scale(1.5f).rotateY(radians(50)).rotateZ(radians(335))
            .translate(-.9375f, -.0625f, 0).translate(1, 0, -8.5f / 16f).scale(-1, 1, 1)
            .mul(NATIVE_INVERSE);

        // The two reflections cancel. Use PoseStack's rotation and positive uniform scale
        // to carry the same transform into the native renderer's lighting normals.
        var origin = transform.getTranslation(new Vector3f());
        var rotation = transform.getUnnormalizedRotation(new Quaternionf()).normalize();
        float scale = transform.getScale(new Vector3f()).x;
        return new Grip(origin, rotation, scale);
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private record Grip(Vector3f origin, Quaternionf rotation, float scale) {
        void apply(PoseStack pose) {
            pose.translate(origin.x, origin.y, origin.z);
            pose.mulPose(rotation);
            pose.scale(scale, scale, scale);
        }
    }
}
