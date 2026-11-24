package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class QueenBeeClient {
    private static final ModelLayerLocation BODY = new ModelLayerLocation(Content.id("queen_bee"), "body");
    private static final ModelLayerLocation CROWN = new ModelLayerLocation(Content.id("queen_bee"), "crown");
    private static final ResourceLocation BEE = new ResourceLocation("minecraft", "textures/entity/bee/bee_angry.png");
    private static final ResourceLocation GOLD = new ResourceLocation("minecraft", "textures/block/gold_block.png");

    private QueenBeeClient() {}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BODY, BeeModel::createBodyLayer);
        event.registerLayerDefinition(CROWN, QueenBeeModel::crownLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(QueenBeeContent.QUEEN.get(), QueenRenderer::new);
        event.registerEntityRenderer(QueenBeeContent.GUARD.get(), GuardRenderer::new);
        event.registerEntityRenderer(QueenBeeContent.SHOT.get(), context -> new FlightItemRenderer<>(context, false));
        event.registerEntityRenderer(QueenBeeContent.THROWN_BOOMERANG.get(), context -> new FlightItemRenderer<>(context, true));
    }

    private static final class QueenRenderer extends MobRenderer<SourceQueenBee, QueenBeeModel<SourceQueenBee>> {
        private QueenRenderer(EntityRendererProvider.Context context) {
            super(context, new QueenBeeModel<>(context.bakeLayer(BODY)), 1.44f);
            ModelPart crown = context.bakeLayer(CROWN).getChild("crown");
            addLayer(new RenderLayer<SourceQueenBee, QueenBeeModel<SourceQueenBee>>(this) {
                @Override
                public void render(PoseStack pose, MultiBufferSource buffers, int light, SourceQueenBee queen,
                                   float step, float stride, float partial, float age, float yaw, float pitch) {
                    if (!queen.isInvisible()) {
                        getParentModel().renderCrown(crown, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(GOLD)),
                            light, OverlayTexture.NO_OVERLAY);
                    }
                }
            });
        }

        @Override
        protected void scale(SourceQueenBee queen, PoseStack pose, float partial) { pose.scale(4.72f, 4.52f, 6.75f); }

        @Override
        public ResourceLocation getTextureLocation(SourceQueenBee queen) { return BEE; }
    }

    private static final class GuardRenderer extends MobRenderer<QueenBeeGuard, QueenBeeModel<QueenBeeGuard>> {
        private GuardRenderer(EntityRendererProvider.Context context) {
            super(context, new QueenBeeModel<>(context.bakeLayer(BODY)), .23f);
        }

        @Override
        protected void scale(QueenBeeGuard guard, PoseStack pose, float partial) { pose.scale(.79f, .79f, .79f); }

        @Override
        public ResourceLocation getTextureLocation(QueenBeeGuard guard) { return BEE; }
    }

    private static final class FlightItemRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T> {
        private final net.minecraft.client.renderer.entity.ItemRenderer items;
        private final boolean spin;

        private FlightItemRenderer(EntityRendererProvider.Context context, boolean spin) {
            super(context);
            items = context.getItemRenderer();
            this.spin = spin;
        }

        @Override
        public void render(T actor, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.translate(0, .11, 0);
            if (spin) {
                pose.mulPose(Axis.YP.rotationDegrees((actor.tickCount + partial) * 48));
                pose.mulPose(Axis.XP.rotationDegrees(68));
                pose.scale(.87f, .87f, .87f);
            } else if (actor instanceof QueenBeeShot shot && shot.honey()) {
                pose.mulPose(entityRenderDispatcher.cameraOrientation());
                pose.mulPose(Axis.ZP.rotationDegrees((actor.tickCount + partial) * 20));
                pose.scale(.53f, .53f, .53f);
            } else {
                pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, actor.yRotO, actor.getYRot()) - 90));
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial, actor.xRotO, actor.getXRot()) - 45));
                pose.scale(.64f, .64f, .64f);
            }
            items.renderStatic(actor.getItem(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, actor.level(), actor.getId());
            pose.popPose();
            super.render(actor, yaw, partial, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T actor) { return InventoryMenu.BLOCK_ATLAS; }
    }

    @Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
    static final class Intro {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null || client.player.tickCount % 3 != 0) return;
            for (var actor : client.level.entitiesForRendering()) {
                if (actor instanceof SourceQueenBee queen) ClientDragonIntro.receiveQueenBee(queen);
            }
        }
    }
}
