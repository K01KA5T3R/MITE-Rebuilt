package dev.koikaster.mite;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pure client-only entity renderer registration for 0.4.18 ITE entities.
 * Isolated via Dist.CLIENT so dedicated servers never reference client classes.
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IteClientEntityRenderers0418 {
    private static final ResourceLocation ZOMBIE_TEX = new ResourceLocation("minecraft", "textures/entity/zombie/zombie.png");
    private static final ResourceLocation PIGMAN_TEX = new ResourceLocation("minecraft", "textures/entity/piglin/zombified_piglin.png");
    private static final ResourceLocation ANNIHILATION_TEX = Content.id("textures/entity/annihilation_skeleton.png");
    private static final ResourceLocation WANDERING_WITCH_TEX = Content.id("textures/entity/wandering_witch.png");
    private static final ResourceLocation ANCIENT_DRAGON_TEX = Content.id("textures/entity/ancient_dragon/main.png");
    private static final ResourceLocation ANNIHILATION_GLOW = Content.id("textures/entity/annihilation_skeleton_glow.png");
    private static final ResourceLocation WANDERING_WITCH_GLOW = Content.id("textures/entity/wandering_witch_glow.png");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
            IteEntities0418.ZOMBIE_LORD.get(),
            ctx -> new HumanoidMobRenderer<IteEntities0418.ZombieLordEntity, HumanoidModel<IteEntities0418.ZombieLordEntity>>(
                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F
            ) {
                @Override
                public ResourceLocation getTextureLocation(IteEntities0418.ZombieLordEntity entity) {
                    return ZOMBIE_TEX;
                }
            }
        );

        event.registerEntityRenderer(
            IteEntities0418.ZOMBIE_PIGMAN_LORD.get(),
            ctx -> new HumanoidMobRenderer<IteEntities0418.ZombiePigmanLordEntity, HumanoidModel<IteEntities0418.ZombiePigmanLordEntity>>(
                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F
            ) {
                @Override
                public ResourceLocation getTextureLocation(IteEntities0418.ZombiePigmanLordEntity entity) {
                    return PIGMAN_TEX;
                }
            }
        );

        event.registerEntityRenderer(
            IteEntities0418.ANNIHILATION_SKELETON.get(),
            ctx -> new HumanoidMobRenderer<IteEntities0418.AnnihilationSkeletonEntity, SkeletonModel<IteEntities0418.AnnihilationSkeletonEntity>>(
                ctx, new SkeletonModel<>(ctx.bakeLayer(ModelLayers.SKELETON)), 0.5F
            ) {
                {
                    final RenderType glow = RenderType.eyes(ANNIHILATION_GLOW);
                    this.addLayer(new EyesLayer<>(this) {
                        @Override public RenderType renderType() { return glow; }
                    });
                }
                @Override
                public ResourceLocation getTextureLocation(IteEntities0418.AnnihilationSkeletonEntity entity) {
                    return ANNIHILATION_TEX;
                }
            }
        );

        event.registerEntityRenderer(
            IteEntities0418.WANDERING_WITCH.get(),
            ctx -> new MobRenderer<IteEntities0418.WanderingWitchEntity, WitchModel<IteEntities0418.WanderingWitchEntity>>(
                ctx, new WitchModel<>(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F
            ) {
                {
                    final RenderType glow = RenderType.eyes(WANDERING_WITCH_GLOW);
                    this.addLayer(new EyesLayer<>(this) {
                        @Override public RenderType renderType() { return glow; }
                    });
                }
                @Override
                public ResourceLocation getTextureLocation(IteEntities0418.WanderingWitchEntity entity) {
                    return WANDERING_WITCH_TEX;
                }
            }
        );

        event.registerEntityRenderer(
            IteEntities0418.ANCIENT_DRAGON.get(),
            ctx -> new MobRenderer<IteEntities0418.AncientDragonEntity, HumanoidModel<IteEntities0418.AncientDragonEntity>>(
                ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 2.5F
            ) {
                @Override
                public ResourceLocation getTextureLocation(IteEntities0418.AncientDragonEntity entity) {
                    return ANCIENT_DRAGON_TEX;
                }

                @Override
                protected void scale(IteEntities0418.AncientDragonEntity entity, com.mojang.blaze3d.vertex.PoseStack pose, float partialTick) {
                    pose.scale(3.0F, 3.0F, 3.0F);
                }
            }
        );

        event.registerEntityRenderer(IteEntities0418.LARGE_FIREBALL_NB.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(IteEntities0418.RIDE_MARKER.get(), NoopRenderer::new);
    }

    private IteClientEntityRenderers0418() {}
}
