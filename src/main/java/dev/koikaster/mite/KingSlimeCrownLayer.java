package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class KingSlimeCrownLayer extends RenderLayer<Slime, SlimeModel<Slime>> {
    private final ItemRenderer items;
    private final ItemStack crown = KingSlimeContent.CROWN.get().getDefaultInstance();

    KingSlimeCrownLayer(SlimeRenderer parent, ItemRenderer items) {
        super(parent);
        this.items = items;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Slime slime,
                       float swing, float strength, float partial, float age, float yaw, float pitch) {
        if (slime.isInvisible()) return;
        pose.pushPose();
        // SlimeModel's outer shell starts at model-space Y=1.0.
        pose.translate(0, .90, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.scale(.39f, .39f, .39f);
        items.renderStatic(crown, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
            pose, buffers, slime.level(), slime.getId());
        pose.popPose();
    }
}
