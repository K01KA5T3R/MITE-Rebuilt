package dev.koikaster.mite;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Dedicated R195 tournament settings. Regular worlds never open a tournament session. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceTournament {
    enum Mode {
        OPEN(true, false, false, 384, 0),
        PICKAXE(true, false, true, 384, 0),
        DIAMOND(true, false, true, 384, 0),
        NETHER(true, false, true, 384, 0),
        SCORE(true, false, true, 384, 12),
        WONDER(false, true, false, 2048, 432);

        final boolean safeZone;
        final boolean allottedTime;
        final boolean animals;
        final int radius;
        final int days;

        Mode(boolean safeZone, boolean allottedTime, boolean animals, int radius, int days) {
            this.safeZone = safeZone;
            this.allottedTime = allottedTime;
            this.animals = animals;
            this.radius = radius;
            this.days = days;
        }

        static Mode parse(String name) {
            for (Mode mode : values()) if (mode.name().equalsIgnoreCase(name)) return mode;
            return null;
        }
    }

    record Config(Mode mode, int pyramidHeight, int allottedTicks, String startTime, String noticeAppend) {
        static Config load(Properties properties) {
            // The old dedicated-server activation code is unrelated to login or permissions.
            if (!"zombies".equals(properties.getProperty("tournament-password"))) return null;
            Mode mode = Mode.parse(properties.getProperty("tournament-type"));
            if (mode == null) return null;
            return new Config(mode, integer(properties, "required-pyramid-height", 20),
                    integer(properties, "allotted-time", 432000),
                    properties.getProperty("tournament-start-time", ""),
                    properties.getProperty("tournament-notice-append", ""));
        }

        private static int integer(Properties properties, String key, int defaultValue) {
            String value = properties.getProperty(key);
            if (value == null) return defaultValue;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException error) {
                return defaultValue;
            }
        }
    }

    private static final String SHARE = "R195TournamentShare";
    private static final String SHARE_LEFT = "Left";
    private static final String SHARE_TICK = "WorldTick";
    private static final String SHARE_DIMENSION = "Dimension";

    private static final class Session {
        final Config config;
        int shutdownTicks;
        boolean victory;
        boolean closing;
        long previousWorldTick = -1;
        long pausedTicks;
        final Map<UUID, OriginalTournamentReconnect> reconnects = new HashMap<>();
        final Map<UUID, Long> depleted = new HashMap<>();
        final Map<UUID, Long> pausedAtDeparture = new HashMap<>();

        Session(Config config) {
            this.config = config;
        }
    }

    private static final Map<MinecraftServer, Session> SESSIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void starting(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) return;
        try (var input = Files.newInputStream(Path.of("server.properties"))) {
            var properties = new Properties();
            properties.load(input);
            Config config = Config.load(properties);
            if (config != null) SESSIONS.put(server, new Session(config));
        } catch (NoSuchFileException absent) {
            // A regular server has no tournament properties.
        } catch (IOException | IllegalArgumentException error) {
            LogUtils.getLogger().warn("Unable to read R195 tournament settings", error);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SESSIONS.remove(event.getServer());
    }

    static Config config(MinecraftServer server) {
        Session session = SESSIONS.get(server);
        return session == null ? null : session.config;
    }

    static Mode mode(MinecraftServer server) {
        Config config = config(server);
        return config == null ? null : config.mode();
    }

    static boolean inArena(ServerLevel level, int x, int z) {
        Mode mode = mode(level.getServer());
        if (mode == null) return false;
        BlockPos spawn = level.getSharedSpawnPos();
        return Math.abs((long) x - spawn.getX()) <= mode.radius
                && Math.abs((long) z - spawn.getZ()) <= mode.radius;
    }

    private static boolean inSafeZone(ServerLevel level, BlockPos pos) {
        Mode mode = mode(level.getServer());
        if (mode == null || !mode.safeZone) return false;
        BlockPos spawn = level.getSharedSpawnPos();
        float dx = pos.getX() - spawn.getX();
        float dy = pos.getY() - spawn.getY();
        float dz = pos.getZ() - spawn.getZ();
        return dx * dx + dy * dy + dz * dz < 1024;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placeInSafeZone(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player)) return;
        if (inSafeZone(level, event.getPos()) || event instanceof BlockEvent.EntityMultiPlaceEvent many
                && many.getReplacedBlockSnapshots().stream().anyMatch(snapshot -> inSafeZone(level, snapshot.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectNewPlayer(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount < 1000
                && event.getSource().getEntity() instanceof Player
                && inSafeZone(player.serverLevel(), player.blockPosition())) {
            event.setCanceled(true);
        }
    }

    static void wonderCompleted(MinecraftServer server) {
        victory(server, Mode.WONDER, Component.translatable("message.mite_reforged.tournament.wonder_completed"));
    }

    static void victory(MinecraftServer server, Mode mode, Component notice) {
        Session session = SESSIONS.get(server);
        if (session == null || session.config.mode() != mode || session.closing) return;
        session.closing = true;
        session.victory = true;
        session.shutdownTicks = 500;
        server.getPlayerList().broadcastSystemMessage(notice, false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (SourceServerAdministration.tick(server)) return;
        Session session = SESSIONS.get(server);
        if (session == null) return;
        long worldTick = server.overworld().getGameTime();
        if (worldTick == session.previousWorldTick && server.getPlayerList().getPlayerCount() == 0) {
            session.pausedTicks++;
        }
        session.previousWorldTick = worldTick;
        Mode mode = session.config.mode();
        if (!session.closing && mode.days > 0
                && SourceMoonCycle.dayOfWorld(server.overworld().getDayTime()) > mode.days
                && (mode != Mode.WONDER || !SourceWonderTournament.finished(server))) {
            session.closing = true;
            session.shutdownTicks = 200;
            message(server, mode == Mode.SCORE ? "score_elapsed" : "time_elapsed");
        }
        if (session.shutdownTicks <= 0) return;
        if (server.getPlayerList().getPlayerCount() == 0 || --session.shutdownTicks == 0) {
            server.halt(false);
        } else if (session.victory && session.shutdownTicks == 400) {
            message(server, "server_closing");
        }
    }

    /** Retain the native ban, allowlist and capacity results before checking tournament admission. */
    public static Component admission(Component original, Object rawList, Object rawProfile) {
        if (original != null) return original;
        var list = (PlayerList) rawList;
        var profile = (GameProfile) rawProfile;
        MinecraftServer server = list.getServer();
        Session session = SESSIONS.get(server);
        if (session == null) return null;
        UUID id = profile.getId();
        long now = competitionTick(server, session);
        Long exhaustedAt = session.depleted.get(id);
        if (exhaustedAt != null) {
            long wait = OriginalTournamentReconnect.DEPLETION_TICKS - (now - exhaustedAt);
            if (wait > 0) return waitMessage("share_wait", (int) Math.min(wait, OriginalTournamentReconnect.DEPLETION_TICKS));
        }
        var entry = session.reconnects.get(id);
        if (entry == null) return null;
        int wait = entry.evaluate(now, competitionDay(server, session)).delayTicks();
        return wait > 0 ? waitMessage("reconnect_wait", wait) : null;
    }

    private static long competitionTick(MinecraftServer server, Session session) {
        return server.overworld().getGameTime() + session.pausedTicks;
    }

    private static int competitionDay(MinecraftServer server, Session session) {
        return SourceMoonCycle.adjustedTime(server.overworld().getDayTime() + session.pausedTicks);
    }

    private static Component waitMessage(String reason, int ticks) {
        return Component.translatable("message.mite_reforged.tournament." + reason,
                OriginalTournamentReconnect.format(ticks));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leaving(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        Session session = SESSIONS.get(server);
        if (session == null) return;
        UUID id = player.getUUID();
        if (session.config.mode().allottedTime) session.pausedAtDeparture.put(id, session.pausedTicks);
        if (session.config.mode() == Mode.OPEN || player.getHealth() <= 0 || player.isSleeping()) {
            session.reconnects.remove(id);
            return;
        }
        session.reconnects.computeIfAbsent(id, key -> new OriginalTournamentReconnect())
                .leave(competitionTick(server, session), competitionDay(server, session));
    }

    @SubscribeEvent
    public static void loadShare(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        Session session = SESSIONS.get(server);
        if (session == null) return;
        UUID id = player.getUUID();
        long now = competitionTick(server, session);
        var entry = session.reconnects.get(id);
        if (entry != null) entry.joined(now, competitionDay(server, session));
        Config config = session.config;
        if (!config.mode().allottedTime) return;
        CompoundTag data = PlayerState.data(player);
        if (!data.contains(SHARE, Tag.TAG_COMPOUND)) {
            data.put(SHARE, new CompoundTag());
            data.getCompound(SHARE).putInt(SHARE_LEFT, config.allottedTicks());
        }
        CompoundTag share = data.getCompound(SHARE);
        long time = player.serverLevel().getGameTime();
        String dimension = player.serverLevel().dimension().location().toString();
        Long pausedAtDeparture = session.pausedAtDeparture.remove(id);
        long pausedWhileAway = pausedAtDeparture == null ? 0 : Math.max(0, session.pausedTicks - pausedAtDeparture);
        int left = share.getInt(SHARE_LEFT);
        if (share.contains(SHARE_TICK, Tag.TAG_LONG) && dimension.equals(share.getString(SHARE_DIMENSION))) {
            left = restoreShare(left, share.getLong(SHARE_TICK), time + pausedWhileAway, config.allottedTicks());
        }
        Long depletedAt = session.depleted.remove(id);
        if (depletedAt != null) {
            left = Math.max(left, restoreShare(0, depletedAt, now, config.allottedTicks()));
        }
        share.putInt(SHARE_LEFT, left);
        rememberShareLocation(share, dimension, time);
        SourceTournamentTransport.send(player, left);
    }

    static int restoreShare(int remaining, long previous, long now, int cap) {
        return (int) Math.min((long) remaining + (now - previous) / 3, cap);
    }

    @SubscribeEvent
    public static void countShare(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        Session session = SESSIONS.get(server);
        if (session == null) return;
        UUID id = player.getUUID();
        if (player.tickCount % 24 == 0) {
            var entry = session.reconnects.get(id);
            if (entry != null) entry.creditOnlineTick();
        }
        if (!session.config.mode().allottedTime) return;
        CompoundTag data = PlayerState.data(player);
        if (!data.contains(SHARE, Tag.TAG_COMPOUND)) return;
        CompoundTag share = data.getCompound(SHARE);
        int left = share.getInt(SHARE_LEFT);
        if (left > 0) share.putInt(SHARE_LEFT, --left);
        rememberShareLocation(share, player.serverLevel().dimension().location().toString(),
                player.serverLevel().getGameTime());
        if (left == 0 && player.connection != null) {
            session.depleted.putIfAbsent(id, competitionTick(server, session));
            player.connection.disconnect(Component.translatable("message.mite_reforged.tournament.share_depleted"));
        } else if (left > 0 && left % 20 == 0) {
            SourceTournamentTransport.send(player, left);
        }
    }

    private static void rememberShareLocation(CompoundTag share, String dimension, long time) {
        share.putLong(SHARE_TICK, time);
        share.putString(SHARE_DIMENSION, dimension);
    }

    private static void message(MinecraftServer server, String key) {
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.mite_reforged.tournament." + key), false);
    }

    private SourceTournament() {}
}

