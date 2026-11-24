package dev.koikaster.mite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Network membership of one live encounter, independent of combat contribution. */
final class BossMusicAudience {
    private final BossMusicTrack track;
    private final Map<UUID, ServerPlayer> listeners = new HashMap<>();

    BossMusicAudience(BossMusicTrack track) {
        this.track = track;
    }

    void sync(ServerLevel level, UUID encounter, List<ServerPlayer> players) {
        sync(level, encounter, players, false);
    }

    void sync(ServerLevel level, UUID encounter, List<ServerPlayer> players, boolean refresh) {
        listeners.values().removeIf(player -> {
            if (players.contains(player) && player.connection.isAcceptingMessages()) return false;
            BossMusicPacket.stop(player, encounter);
            return true;
        });
        for (var player : players) {
            if (!player.connection.isAcceptingMessages()) continue;
            if (listeners.put(player.getUUID(), player) != player || refresh) {
                BossMusicPacket.start(player, encounter, track, level.dimension().location());
            }
        }
    }

    void duck(UUID encounter, int gain, int ticks) {
        for (var player : listeners.values()) BossMusicPacket.duck(player, encounter, gain, ticks);
    }

    void stop(UUID encounter) {
        for (var player : listeners.values()) BossMusicPacket.stop(player, encounter);
        listeners.clear();
    }
}
