package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ClientTournamentScore {
    private static final int POSITIVE = 0x80ff20;
    private static final int NEGATIVE = 0xff1313;
    private static final int PARENTHESES = 0xc0c0c0;

    private static Connection owner;
    private static int score, delta, opacity;

    record Display(int score, int delta, int opacity) {}

    static void receive(Connection connection, int points, boolean showChange) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener == null || listener.getConnection() != connection) return;
        if (owner != connection) {
            owner = connection;
            delta = opacity = 0;
        } else if (showChange && points != score) {
            delta += points - score;
            opacity = 480;
        }
        score = points;
    }

    static Display current(Minecraft client) {
        var listener = client.getConnection();
        return owner != null && listener != null && listener.getConnection() == owner
                ? new Display(score, delta, opacity) : null;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        owner = null;
        score = delta = opacity = 0;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        var client = Minecraft.getInstance();
        var listener = client.getConnection();
        if (owner == null || listener == null || listener.getConnection() != owner
                || client.player == null || client.level == null || client.options.hideGui) return;

        // The R195 renderer uses two opacity units per rendered frame and allows gains to accumulate.
        if (opacity > 0) opacity = Math.max(0, opacity - 2);
        if (opacity == 0) delta = 0;
        String total = Integer.toString(score);
        int x = 2;
        int y = 2 + 9 * MinimalDebug.lines(client).size();
        var graphics = event.getGuiGraphics();
        graphics.drawString(client.font, total, x, y, score < 0 ? NEGATIVE : POSITIVE, true);
        if (delta == 0 || opacity <= 4) return;

        int alpha = Math.min(opacity, 255) << 24;
        String open = "  (";
        String change = delta > 0 ? "+" + delta : Integer.toString(delta);
        x += client.font.width(total);
        graphics.drawString(client.font, open, x, y, alpha | PARENTHESES, true);
        x += client.font.width(open);
        graphics.drawString(client.font, change, x, y, alpha | (delta < 0 ? NEGATIVE : POSITIVE), true);
        x += client.font.width(change);
        graphics.drawString(client.font, ")", x, y, alpha | PARENTHESES, true);
    }

    private ClientTournamentScore() {}
}
