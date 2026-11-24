package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ClientTournamentTime {
    private static Connection owner;
    private static int remaining = -1;

    static void receive(Connection connection, int ticks) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener == null || listener.getConnection() != connection) return;
        owner = connection;
        remaining = ticks;
    }

    static int remaining(Minecraft client) {
        var listener = client.getConnection();
        return owner != null && listener != null && listener.getConnection() == owner ? remaining : -1;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        owner = null;
        remaining = -1;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        int ticks = remaining(client);
        if (ticks < 0 || client.player == null || client.level == null || client.options.hideGui) return;
        String time = OriginalTournamentReconnect.format(ticks);
        int y = 2 + 9 * MinimalDebug.lines(client).size();
        var graphics = event.getGuiGraphics();
        graphics.fill(1, y - 1, client.font.width(time) + 3, y + 8, 0x90000000);
        graphics.drawString(client.font, time, 2, y, 0x80ff20, false);
    }

    private ClientTournamentTime() {}
}

