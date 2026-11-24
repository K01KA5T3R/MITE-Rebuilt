package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class CrabulonClient {
    private static final ModelLayerLocation MODEL = new ModelLayerLocation(Content.id("crabulon"), "main");
    private static final ResourceLocation HULL = Content.id("textures/entity/crabulon.png");
    private static final ResourceLocation CAP = new ResourceLocation("minecraft", "textures/block/red_mushroom_block.png");

    private CrabulonClient() {}

    @SubscribeEvent
    public static void layer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MODEL, CrabulonModel::layer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CrabulonContent.CRABULON.get(), CrabRenderer::new);
        event.registerEntityRenderer(CrabulonContent.SPORE.get(), context -> new ThrownItemRenderer<>(context, .66f, true));
        event.registerEntityRenderer(CrabulonContent.BROOD.get(), BroodRenderer::new);
    }

    private static final class CrabRenderer extends MobRenderer<SourceCrabulon, CrabulonModel> {
        private CrabRenderer(EntityRendererProvider.Context context) {
            super(context, new CrabulonModel(context.bakeLayer(MODEL)), 3.0f);
            addLayer(new RenderLayer<SourceCrabulon, CrabulonModel>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, SourceCrabulon crab,
                                   float step, float stride, float partial, float age, float yaw, float pitch) {
                    if (crab.isInvisible()) return;
                    getParentModel().renderEyes(pose, buffers.getBuffer(RenderType.eyes(HULL)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }
            });
        }

        @Override
        protected void scale(SourceCrabulon crab, PoseStack pose, float partial) {
            pose.scale(2.86f, 3.62f, 2.95f);
        }

        @Override
        public ResourceLocation getTextureLocation(SourceCrabulon crab) { return HULL; }
    }

    private static final class BroodRenderer extends EntityRenderer<CrabulonBrood> {
        private final net.minecraft.client.renderer.entity.ItemRenderer items;
        private final ItemStack cap = new ItemStack(Items.RED_MUSHROOM);
        private final ItemStack stem = new ItemStack(Items.BROWN_MUSHROOM);

        private BroodRenderer(EntityRendererProvider.Context context) {
            super(context);
            items = context.getItemRenderer();
        }

        @Override
        public void render(CrabulonBrood seed, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.translate(0, .19, 0);
            pose.mulPose(Axis.YP.rotationDegrees((seed.tickCount + partial) * 7.5f));
            pose.scale(1.5f, 1.5f, 1.5f);
            items.renderStatic(stem, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, seed.level(), seed.getId());
            pose.translate(0, .20, 0);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            items.renderStatic(cap, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, seed.level(), seed.getId());
            pose.popPose();
            super.render(seed, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(CrabulonBrood seed) { return CAP; }
    }

    @Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
    static final class Intro {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null || client.player.tickCount % 3 != 0) return;
            for (var actor : client.level.entitiesForRendering()) {
                if (actor instanceof SourceCrabulon crab) ClientDragonIntro.receiveCrabulon(crab);
            }
        }
    }
}
