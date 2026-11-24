package dev.koikaster.mite;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Phase D Round 1 (0.4.20) client logout state sanitizer: resets remote IteConfig
 * snapshot whenever disconnecting from a server or exiting a singleplayer world.
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class IteAuditFixes0420 {
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        IteConfig.clearClient();
        ClassicQuality.setDurabilityScalingSuppressed(false);
    }

    private IteAuditFixes0420() {}
}
