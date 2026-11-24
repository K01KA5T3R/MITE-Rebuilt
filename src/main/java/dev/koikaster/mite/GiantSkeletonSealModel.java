package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

final class GiantSkeletonSealModel {
    private final ModelPart fragments;

    GiantSkeletonSealModel() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4, -8, -4, 8, 8, 8), PartPose.offsetAndRotation(0, -39, -8, -.18f, .16f, .07f));
        root.addOrReplaceChild("ribcage", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4, 0, -2, 8, 12, 4), PartPose.offsetAndRotation(0, -37, -8, .04f, .05f, -.08f));
        root.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(40, 16)
            .addBox(-1, -2, -1, 2, 11, 2), PartPose.offsetAndRotation(-7, -31, -8, .02f, -.25f, .60f));
        fragments = LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }

    void render(PoseStack pose, MultiBufferSource buffers, int light, ResourceLocation texture) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.scale(1.25f, 1.25f, 1.25f);
        fragments.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
