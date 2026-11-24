package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class KingSlimeClient {
    private KingSlimeClient() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KingSlimeContent.KING.get(), context -> new SlimeRenderer(context) {
            {
                addLayer(new KingSlimeCrownLayer(this, context.getItemRenderer()));
            }
        });
        event.registerEntityRenderer(KingSlimeContent.MINION.get(), SlimeRenderer::new);
        event.registerEntityRenderer(KingSlimeContent.JEWEL.get(), context -> new ThrownItemRenderer<>(context, 1.15f, true));
        event.registerEntityRenderer(KingSlimeContent.JEWEL_BOLT.get(), context -> new ThrownItemRenderer<>(context, .48f, true));
    }

    @Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
    static final class IntroTick {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            var client = Minecraft.getInstance();
            if (client.level == null || client.player == null || client.player.tickCount % 3 != 0) return;
            for (var actor : client.level.entitiesForRendering()) {
                if (actor instanceof SourceKingSlime king) ClientDragonIntro.receiveKingSlime(king);
            }
        }
    }
}
