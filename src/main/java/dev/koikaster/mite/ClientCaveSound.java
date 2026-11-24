package dev.koikaster.mite;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Owns only the cave sounds emitted by the R195 environment scheduler. */
public final class ClientCaveSound {
    private static final ResourceLocation CAVE = new ResourceLocation("minecraft:ambient.cave");

    public static Optional<AmbientMoodSettings> mood(Optional<AmbientMoodSettings> selected) {
        return selected.filter(value -> !value.getSoundEvent().value().getLocation().equals(CAVE));
    }

    public static void play(SourceCaveAmbient.Message value) {
        var game = Minecraft.getInstance();
        var player = game.player;
        var level = game.level;
        var connection = game.getConnection();
        if (player == null || level == null || connection == null || !level.dimension().equals(value.dimension())
                || !Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())
                || !Float.isFinite(value.volume()) || value.volume() <= 0
                || !Float.isFinite(value.pitch()) || value.pitch() <= 0) return;

        var sound = new CaveSound(game, level, player, connection, value);
        if (sound.eligible()) game.getSoundManager().play(sound);
    }

    private static LevelChunk loadedChunk(ClientLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return null;
        return level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
    }

    private static boolean belowCover(LevelChunk chunk, BlockPos pos) {
        // The no-leaves heightmap is not sent to clients. Its predicate is a subset of MOTION_BLOCKING.
        int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX() & 15, pos.getZ() & 15);
        var probe = new BlockPos.MutableBlockPos();
        for (int y = top; y > pos.getY(); --y) {
            if (Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque().test(
                    chunk.getBlockState(probe.set(pos.getX(), y, pos.getZ())))) return true;
        }
        return false;
    }

    private static final class CaveSound extends AbstractTickableSoundInstance {
        private final Minecraft game;
        private final ClientLevel level;
        private final LocalPlayer player;
        private final ClientPacketListener connection;
        private final SourceCaveAmbient.Message cue;
        private final BlockPos origin;

        CaveSound(Minecraft game, ClientLevel level, LocalPlayer player, ClientPacketListener connection,
                  SourceCaveAmbient.Message cue) {
            super(SoundEvents.AMBIENT_CAVE.value(), cue.local() ? SoundSource.MASTER : SoundSource.AMBIENT,
                RandomSource.create(cue.seed()));
            this.game = game;
            this.level = level;
            this.player = player;
            this.connection = connection;
            this.cue = cue;
            origin = BlockPos.containing(cue.x(), cue.y(), cue.z());
            relative = cue.local();
            volume = cue.local() ? SourceSoundGain.ui(cue.volume()) : cue.volume();
            pitch = cue.pitch();
            if (relative) {
                attenuation = Attenuation.NONE;
            } else {
                attenuation = Attenuation.LINEAR;
                x = cue.x();
                y = cue.y();
                z = cue.z();
            }
        }

        @Override
        public boolean canPlaySound() {
            return !isStopped() && game.level == level && game.player == player && game.getConnection() == connection;
        }

        private boolean eligible() {
            if (!canPlaySound() || !player.isAlive() || player.isSpectator()
                    || !SourceCaveAmbient.inRange(player.distanceToSqr(cue.x(), cue.y(), cue.z()))) return false;

            var eye = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
            var listenerChunk = loadedChunk(level, eye);
            var sourceChunk = loadedChunk(level, origin);
            if (listenerChunk == null || !belowCover(listenerChunk, eye)
                    || sourceChunk == null || !sourceChunk.getBlockState(origin).isAir()
                    || level.getRawBrightness(origin, 0) >= 8) return false;
            if (level.dimensionType().hasSkyLight()) {
                return !level.canSeeSky(eye) && level.getBrightness(LightLayer.SKY, origin) == 0;
            }
            return belowCover(sourceChunk, origin);
        }

        @Override
        public void tick() {
            if (!eligible()) stop();
        }
    }

    private ClientCaveSound() {}
}
