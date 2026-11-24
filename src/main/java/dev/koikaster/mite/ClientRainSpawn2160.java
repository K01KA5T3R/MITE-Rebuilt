package dev.koikaster.mite;

import dev.koikaster.mite.visual.ClassicVisualState;
import dev.koikaster.mite.visual.ClassicVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import dev.koikaster.mite.LegacyRainSpawn2160.Surface;

public final class ClientRainSpawn2160 {
    private static Entity viewer(Minecraft client, ClientLevel level) {
        if (!ClassicVisualState.ready || !ClassicVisuals.particles() || level == null || level != client.level) return null;
        var viewer = client.getCameraEntity();
        return viewer != null && viewer.level() == level ? viewer : null;
    }

    public static boolean ownsAudio(Minecraft client) {
        return viewer(client, client.level) != null;
    }

    public static int sample(Minecraft client, int ticks, int counter) {
        var level = client.level;
        var viewer = viewer(client, level);
        if (viewer == null) return counter;
        var status = client.options.particles().get();
        int density = status == ParticleStatus.MINIMAL ? 2 : status == ParticleStatus.DECREASED ? 1 : 0;
        int attempts = LegacyRainSpawn2160.attempts(level.getRainLevel(1F), Minecraft.useFancyGraphics(), density);
        var center = viewer.blockPosition();
        var sound = LegacyRainSpawn2160.sample(ticks, center.getX(), center.getY(), center.getZ(), attempts, counter,
            (x, z, minimum) -> column(level, x, z, minimum, center.getY() + 10),
            (smoke, x, y, z) -> {
                if (smoke) client.particleEngine.add(new ClientRainSmoke2180(client.particleEngine, level, x, y, z));
                else client.particleEngine.createParticle(ParticleTypes.RAIN, x, y, z, 0, 0, 0);
            });
        var impact = sound.impact();
        if (impact != null && client.player != null) {
            int cover = ClientSourceRain2146.precipitationHeight(level, center.getX(), center.getZ(), center.getY());
            SourceSleepClient.rainSound(level, impact.x(), impact.y(), impact.z(), impact.above(viewer.getY(), cover));
        }
        return sound.counter();
    }

    static LegacyRainSpawn2160.Column column(ClientLevel level, int x, int z, int minimum, int maximum) {
        int height = ClientSourceRain2146.precipitationHeight(level, x, z, minimum - 1);
        if (height == ClientSourceRain2146.NO_SURFACE || height > maximum) return null;
        var top = new BlockPos(x, height, z);
        int motionHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (motionHeight > height && motionHeight <= maximum && nativeHot(level.getBlockState(top.atY(motionHeight - 1)))) {
            height = motionHeight;
            top = top.atY(height);
        }
        var biome = level.getBiome(top).value();
        if (!(biome.getBaseTemperature() >= .2F)
            || ClientSourceMoons.precipitation(biome.getPrecipitationAt(top)) != Biome.Precipitation.RAIN) return null;
        var at = top.below();
        var state = level.getBlockState(at);
        if (nativeHot(state)) return new LegacyRainSpawn2160.Column(height, Surface.NATIVE_HOT, 0, 0, 0);
        if (state.getBlock() instanceof LiquidBlock) {
            var surface = state.getFluidState().is(FluidTags.LAVA) ? Surface.LAVA : Surface.LIQUID;
            return new LegacyRainSpawn2160.Column(height, surface, 0, 0, state.getValue(LiquidBlock.LEVEL));
        }
        if (ClientSourceRain2146.standardGround(level, at, state)) {
            return new LegacyRainSpawn2160.Column(height, Surface.CUBE, 0, 0, 0);
        }
        if (state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN)) {
            return new LegacyRainSpawn2160.Column(height, Surface.OPEN_TRAPDOOR, 0, 0, 0);
        }
        var outline = state.getShape(level, at);
        return new LegacyRainSpawn2160.Column(height, Surface.PARTIAL,
            outline.isEmpty() ? 0 : outline.min(Direction.Axis.Y), outline.isEmpty() ? 0 : outline.max(Direction.Axis.Y), 0);
    }

    /** Native particle sampling remains responsible for the newer hot surfaces. */
    public static void nativeParticle(ClientLevel level, ParticleOptions type, double x, double y, double z,
                                      double dx, double dy, double dz, BlockState state) {
        boolean replaced = type == ParticleTypes.RAIN || type == ParticleTypes.SMOKE && !nativeHot(state);
        if (!replaced || viewer(Minecraft.getInstance(), level) == null) level.addParticle(type, x, y, z, dx, dy, dz);
    }

    private static boolean nativeHot(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK) || CampfireBlock.isLitCampfire(state);
    }

    private ClientRainSpawn2160() {}
}
