package dev.koikaster.mite;

import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-owned R195 cave scheduling and dimension-local delivery. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SourceCaveAmbient {
    private static final class Audio {
        final RandomSource random = RandomSource.create();
        final RandomSource samples = RandomSource.create();
        final SourceCaveSoundRules.State state = new SourceCaveSoundRules.State(
            SourceCaveSoundRules.delay(true, random::nextInt), samples.nextInt());
    }

    private static final Map<ServerLevel, Audio> STATES = new WeakHashMap<>();
    private static SimpleChannel channel;

    public record Position(int x, int z) {
        @Override public int hashCode() { return x ^ z; }
    }

    public static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(Message.class, 34, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Message::write).decoder(Message::read).consumerMainThread(Message::handle).add();
    }

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) STATES.computeIfAbsent(level, ignored -> new Audio());
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) STATES.remove(level);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) tick(level);
    }

    public static void tick(ServerLevel level) {
        var audio = STATES.computeIfAbsent(level, ignored -> new Audio());
        var state = audio.state;
        state.remaining = SourceCaveSoundRules.countdown(state.remaining);
        if (state.remaining != 0 || level.players().isEmpty()) return;

        var active = new HashSet<Position>();
        for (var player : level.players()) {
            var center = player.chunkPosition();
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) active.add(new Position(center.x + dx, center.z + dz));
            }
        }
        var world = new SourceCaveSoundRules.World() {
            public int minY() { return level.getMinBuildHeight(); }
            public int maxY() { return level.getMaxBuildHeight(); }
            public boolean client() { return false; }
            public boolean air(int x, int y, int z) {
                var pos = new BlockPos(x, y, z);
                var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                return chunk != null && chunk.isLightCorrect() && chunk.getBlockState(pos).isAir()
                    && (level.dimensionType().hasSkyLight() || belowCover(chunk, pos));
            }
            public int light(int x, int y, int z) { return level.getRawBrightness(new BlockPos(x, y, z), 0); }
            public int sky(int x, int y, int z) { return level.getBrightness(LightLayer.SKY, new BlockPos(x, y, z)); }
            public double nearestAlivePlayerSquared(int x, int y, int z) {
                double nearest = -1;
                for (var player : level.players()) {
                    double distance = player.distanceToSqr(x + .5, y + .5, z + .5);
                    if (distance < 64 && (nearest < 0 || distance < nearest) && sheltered(level, player)) nearest = distance;
                }
                return nearest;
            }
            public int nextInt(int bound) { return audio.random.nextInt(bound); }
            public float nextFloat() { return audio.random.nextFloat(); }
            public void play(int x, int y, int z, float volume, float pitch) {
                SourceCaveAmbient.play(level, x + .5, y + .5, z + .5, volume, pitch);
            }
        };
        for (var pos : active) {
            if (state.remaining != 0) break;
            if (level.getChunkSource().getChunkNow(pos.x, pos.z) != null) {
                SourceCaveSoundRules.sample(state, world, pos.x << 4, pos.z << 4);
            }
        }
    }

    static boolean sheltered(ServerLevel level, ServerPlayer player) {
        if (player.level() != level || !player.isAlive() || player.isSpectator()) return false;
        var eye = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        var chunk = level.getChunkSource().getChunkNow(eye.getX() >> 4, eye.getZ() >> 4);
        if (chunk == null || !chunk.isLightCorrect() || !belowCover(chunk, eye)) return false;
        return !level.dimensionType().hasSkyLight() || !level.canSeeSky(eye);
    }

    private static boolean belowCover(LevelChunk chunk, BlockPos pos) {
        return pos.getY() < chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX() & 15, pos.getZ() & 15);
    }

    static boolean inRange(double squaredDistance) {
        return squaredDistance > 4 && squaredDistance < 64;
    }

    public static void play(ServerLevel level, double x, double y, double z, float volume, float pitch) {
        var host = level.getServer().getSingleplayerProfile();
        boolean resolvedRemote = false;
        Message remote = null;
        ClientboundSoundPacket replacement = null;
        for (var player : level.players()) {
            if (!inRange(player.distanceToSqr(x, y, z)) || !sheltered(level, player)) continue;
            if (host != null && host.getId().equals(player.getUUID())) {
                send(player, new Message(level.dimension(), x, y, z, volume, pitch, nextSoundSeed(level), true));
                continue;
            }
            if (!resolvedRemote) {
                resolvedRemote = true;
                var sound = ForgeEventFactory.onPlaySoundAtPosition(level, x, y, z, SoundEvents.AMBIENT_CAVE,
                    SoundSource.AMBIENT, volume, pitch);
                if (!sound.isCanceled() && sound.getSound() != null) {
                    long seed = nextSoundSeed(level);
                    if (sound.getSound().value() == SoundEvents.AMBIENT_CAVE.value() && sound.getSource() == SoundSource.AMBIENT) {
                        remote = new Message(level.dimension(), x, y, z, sound.getNewVolume(), sound.getNewPitch(), seed, false);
                    } else {
                        replacement = new ClientboundSoundPacket(sound.getSound(), sound.getSource(), x, y, z,
                            sound.getNewVolume(), sound.getNewPitch(), seed);
                    }
                }
            }
            if (remote != null) send(player, remote);
            if (replacement != null) player.connection.send(replacement);
        }
    }

    private static long nextSoundSeed(ServerLevel level) {
        return STATES.computeIfAbsent(level, ignored -> new Audio()).samples.nextLong();
    }

    private static void send(ServerPlayer player, Message cue) {
        if (channel != null) channel.send(PacketDistributor.PLAYER.with(() -> player), cue);
    }

    public record Message(ResourceKey<Level> dimension, double x, double y, double z, float volume, float pitch,
                          long seed, boolean local) {
        static void write(Message value, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(value.dimension.location());
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
            buffer.writeFloat(value.volume);
            buffer.writeFloat(value.pitch);
            buffer.writeLong(value.seed);
            buffer.writeBoolean(value.local);
        }

        static Message read(FriendlyByteBuf buffer) {
            var dimension = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
            return new Message(dimension, buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readLong(), buffer.readBoolean());
        }

        static void handle(Message value, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> ClientCaveSound.play(value));
            context.get().setPacketHandled(true);
        }
    }

    private SourceCaveAmbient() {}
}
