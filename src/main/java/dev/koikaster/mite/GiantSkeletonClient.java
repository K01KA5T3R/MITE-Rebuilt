package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.block.Blocks;

final class GiantSkeletonClient extends SkeletonRenderer {
    private final BlockRenderDispatcher blocks;
    private final GiantSkeletonSealModel seal = new GiantSkeletonSealModel();

    GiantSkeletonClient(EntityRendererProvider.Context context) {
        super(context);
        model = new GiantBowModel(context.bakeLayer(ModelLayers.SKELETON));
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 1.15f;
    }

    @Override
    public void render(AbstractSkeleton skeleton, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (!(skeleton instanceof SourceGiantSkeleton giant) || !giant.sealed()) {
            super.render(skeleton, yaw, partialTick, pose, buffers, light);
            return;
        }
        pose.pushPose();
        try {
            pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
            rock(pose, buffers, light, -.91f, .01f, -.73f, 1.82f, 2.45f, 1.65f, 0);
            rock(pose, buffers, light, -.77f, 1.76f, -.47f, 1.56f, 1.81f, 1.38f, -5);
            rock(pose, buffers, light, -.55f, 3.17f, -.34f, 1.17f, 1.05f, 1.05f, 8);
            rock(pose, buffers, light, -.92f, .15f, -.84f, .47f, 1.08f, .45f, -11);
            rock(pose, buffers, light, .40f, .22f, -.82f, .49f, .86f, .44f, 10);
            seal.render(pose, buffers, light, getTextureLocation(skeleton));
        } finally {
            pose.popPose();
        }
    }

    private void rock(PoseStack pose, MultiBufferSource buffers, int light,
                      float x, float y, float z, float width, float height, float depth, float tilt) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.ZP.rotationDegrees(tilt));
        pose.scale(width, height, depth);
        blocks.renderSingleBlock(Blocks.STONE.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    protected void scale(AbstractSkeleton skeleton, PoseStack pose, float partialTick) {
        pose.scale(3, 3, 3);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton skeleton) {
        return Content.id("textures/entity/underworld_giant_skeleton.png");
    }

    private static final class GiantBowModel extends SkeletonModel<AbstractSkeleton> {
        GiantBowModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(AbstractSkeleton skeleton, float walk, float speed, float age, float yaw, float pitch) {
            body.xRot = 0;
            rightArm.y = leftArm.y = 2;
            rightArm.z = leftArm.z = 0;
            super.setupAnim(skeleton, walk, speed, age, yaw, pitch);
            if (!(skeleton instanceof SourceGiantSkeleton giant) || !giant.isUsingItem()) return;
            if (giant.bowPose() == SourceGiantSkeleton.LOW_BOW_POSE) {
                float time = Mth.clamp(giant.getTicksUsingItem() / 7f, 0, 1);
                float weight = time * time * (3 - 2 * time);
                body.xRot = .37f * weight;
                rightArm.y += 3.5f * weight;
                leftArm.y += 3.5f * weight;
                rightArm.z -= 1.5f * weight;
                leftArm.z -= 1.5f * weight;
                rightArm.xRot += .32f * weight;
                leftArm.xRot += .32f * weight;
            } else if (giant.bowPose() == SourceGiantSkeleton.LOB_BOW_POSE) {
                float time = Mth.clamp(giant.getTicksUsingItem() / 7f, 0, 1);
                float weight = time * time * (3 - 2 * time);
                rightArm.y -= 1.2f * weight;
                leftArm.y -= 1.2f * weight;
                rightArm.xRot -= .22f * weight;
                leftArm.xRot -= .22f * weight;
            }
        }
    }
}
