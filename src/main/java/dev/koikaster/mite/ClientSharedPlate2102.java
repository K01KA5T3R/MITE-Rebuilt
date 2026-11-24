package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class ClientSharedPlate2102 {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SharedPlate2102.ENTITY.get(), FoodRenderer::new);
    }

    private static final class FoodRenderer implements BlockEntityRenderer<SharedPlate2102.PlateEntity> {
        private final ItemRenderer items;

        private FoodRenderer(BlockEntityRendererProvider.Context context) {
            items = context.getItemRenderer();
        }

        @Override public void render(SharedPlate2102.PlateEntity plate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var stack = plate.displayedFood();
            if (stack.isEmpty()) return;
            pose.pushPose();
            pose.translate(.5, .135, .5);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(.56f, .56f, .56f);
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, plate.getLevel(), 0);
            pose.popPose();
        }
    }

    private ClientSharedPlate2102() {}
}
