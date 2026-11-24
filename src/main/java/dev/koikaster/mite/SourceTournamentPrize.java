package dev.koikaster.mite;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;
import net.minecraft.server.level.ServerPlayer;

final class SourceTournamentPrize {
    static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    static int code(long worldSeed, String name) {
        int nameSum = 0;
        for (int i = 0; i < name.length(); i++) nameSum += name.charAt(i) * i;
        var random = new Random(worldSeed * 37L + (long) (nameSum * 61));
        int result = 0;
        for (int i = 0; i < 8; i++) result = result * 10 + random.nextInt(10);
        return result;
    }

    static String digits(int code) {
        return String.format(Locale.ROOT, "%08d", code);
    }

    static void issue(ServerPlayer player) {
        String name = player.getGameProfile().getName();
        if (!validName(name)) return;
        var directory = Path.of("prize_keys");
        int code = code(player.serverLevel().getSeed(), name);
        try {
            SourceTournamentFiles.directory(directory);
            SourceTournamentFiles.replace(directory.resolve(name + ".key"), digits(code));
            SourceTournamentTransport.sendPrize(player, code);
        } catch (IOException error) {
            LogUtils.getLogger().warn("Unable to write a local tournament prize key", error);
        }
    }

    private SourceTournamentPrize() {}
}
