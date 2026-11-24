package dev.koikaster.mite;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceServerAdministration {
    private static final Path PLAYERS = Path.of("players");
    private static final Path KICK = Path.of("kick.txt");
    private static final Path SIGNAL = Path.of("signal.txt");
    private static final String MESSAGE = "message.mite_reforged.tournament.";
    private static final Map<MinecraftServer, Integer> SHUTDOWNS = new WeakHashMap<>();

    enum Signal {
        SHUTDOWN("shutdown", "admin_shutdown", 400),
        IMMEDIATE("shutdown immediately", "admin_immediate", 1),
        BACKUP("shutdown for world backup", "admin_backup", 400),
        UPDATE("shutdown for update", "admin_update", 400);

        final String command;
        final String message;
        final int ticks;

        Signal(String command, String message, int ticks) {
            this.command = command;
            this.message = message;
            this.ticks = ticks;
        }

        static Signal parse(String input) {
            for (var signal : values()) {
                if (signal.command.equals(input)) return signal;
            }
            return null;
        }
    }

    /** Returns true while an operator-requested shutdown owns this server's countdown. */
    static boolean tick(MinecraftServer server) {
        if (!server.isDedicatedServer()) return false;
        int tick = server.getTickCount();
        if (tick % 1000 == 0) roster(server, null);
        if (tick % 20 == 0) {
            kick(server);
            signal(server);
        }
        Integer remaining = SHUTDOWNS.get(server);
        if (remaining == null) return false;
        if (remaining <= 1 || server.getPlayerList().getPlayerCount() == 0) {
            server.halt(false);
        } else {
            SHUTDOWNS.put(server, remaining - 1);
        }
        return true;
    }

    private static void kick(MinecraftServer server) {
        try {
            String name = firstLine(KICK);
            if (name == null || name.isEmpty()) return;
            var player = server.getPlayerList().getPlayerByName(name);
            if (player != null && Files.deleteIfExists(KICK)) {
                player.connection.disconnect(Component.translatable(MESSAGE + "admin_kicked"));
            }
        } catch (IOException error) {
            LogUtils.getLogger().warn("Unable to process {}", KICK, error);
        }
    }

    private static void signal(MinecraftServer server) {
        try {
            var signal = Signal.parse(firstLine(SIGNAL));
            if (signal == null || !Files.deleteIfExists(SIGNAL)) return;
            SHUTDOWNS.put(server, signal.ticks);
            var notice = Component.translatable(MESSAGE + signal.message);
            if (signal == Signal.IMMEDIATE) LogUtils.getLogger().info("{}", notice.getString());
            else server.getPlayerList().broadcastSystemMessage(notice, false);
        } catch (IOException error) {
            LogUtils.getLogger().warn("Unable to process {}", SIGNAL, error);
        }
    }

    private static String firstLine(Path file) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return null;
        try (var reader = Files.newBufferedReader(file)) {
            var line = new StringBuilder();
            for (int c; (c = reader.read()) >= 0 && c != '\n' && c != '\r';) {
                if (line.length() == 64) return null;
                line.append((char) c);
            }
            return line.toString();
        }
    }

    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        var server = player.getServer();
        if (server == null || !server.isDedicatedServer()) return;
        roster(server, null);
        var config = SourceTournament.config(server);
        if (config == null) return;
        player.sendSystemMessage(notice(config));
        SourceTournamentPrize.issue(player);
    }

    @SubscribeEvent
    public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null
                && player.getServer().isDedicatedServer()) roster(player.getServer(), player);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        var server = event.getServer();
        SHUTDOWNS.remove(server);
        if (server.isDedicatedServer()) writeRoster("");
    }

    private static Component notice(SourceTournament.Config config) {
        String prefix = MESSAGE + "objective_";
        var message = switch (config.mode()) {
            case OPEN -> config.startTime().isEmpty() ? Component.translatable(prefix + "open")
                    : Component.translatable(prefix + "start", config.startTime());
            case PICKAXE -> Component.translatable(prefix + "pickaxe");
            case DIAMOND -> Component.translatable(prefix + "diamond");
            case NETHER -> Component.translatable(prefix + "nether");
            case SCORE -> Component.translatable(prefix + "score", config.mode().days);
            case WONDER -> Component.translatable(prefix + "wonder", config.pyramidHeight());
        };
        if (!config.noticeAppend().isEmpty()) message.append("\n").append(config.noticeAppend());
        return message;
    }

    private static void roster(MinecraftServer server, ServerPlayer leaving) {
        String names = server.getPlayerList().getPlayers().stream().filter(player -> player != leaving)
                .map(player -> player.getGameProfile().getName()).collect(Collectors.joining(System.lineSeparator()));
        writeRoster(names);
    }

    private static void writeRoster(String names) {
        try {
            SourceTournamentFiles.replace(PLAYERS, names);
        } catch (IOException error) {
            LogUtils.getLogger().warn("Unable to write the online roster", error);
        }
    }

    private SourceServerAdministration() {}
}
