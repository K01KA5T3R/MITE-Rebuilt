package dev.koikaster.mite;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;

final class ClientTournamentPrize {
    static void receive(Connection connection, int code) {
        var game = Minecraft.getInstance();
        var listener = game.getConnection();
        if (listener == null || listener.getConnection() != connection || game.player == null
                || code < 0 || code > 99_999_999) return;
        String name = game.player.getGameProfile().getName();
        if (!SourceTournamentPrize.validName(name)) return;
        var directory = game.gameDirectory.toPath().resolve("MITE");
        try {
            SourceTournamentFiles.directory(directory);
            SourceTournamentFiles.replace(directory.resolve("prize_key.txt"),
                    "Tournament Prize Key\n" + name + "\n" + SourceTournamentPrize.digits(code) + "\n");
        } catch (IOException error) {
            LogUtils.getLogger().warn("Unable to write the local tournament prize key", error);
        }
    }

    private ClientTournamentPrize() {}
}
