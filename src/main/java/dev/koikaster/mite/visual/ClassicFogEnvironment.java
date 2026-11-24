package dev.koikaster.mite.visual;

import dev.koikaster.mite.LegacyLighting;
import dev.koikaster.mite.MiteReforged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

/** Read-only client view of the server's existing state-packet metadata. */
final class ClassicFogEnvironment {
    private static ClientLevel level;
    private static long seed;
    private static ClassicFogField field;
    private static ClientLevel biomeLevel;
    private static ClassicFogBiomes biomes;
    private static long biomeTick = Long.MIN_VALUE;
    private static double biomeX, biomeZ, biomeDistance;

    static float distance(float full, double height, float sky) {
        var mc = Minecraft.getInstance();
        var data = mc.player.getPersistentData().getCompound(MiteReforged.ID);
        if (!data.contains("ClassicFogSeed", Tag.TAG_LONG))
            return LegacyLighting.fogFar(full, (float)height, sky, true);
        var viewer = mc.player;
        float far = ClassicFogField.beforeWeather(full, height, sky, 1);
        if (far < full) {
            long current = data.getLong("ClassicFogSeed");
            if (field == null || level != mc.level || seed != current) {
                level = mc.level; seed = current; field = new ClassicFogField(seed);
            }
            far = ClassicFogField.beforeWeather(full, height, sky, field.proximity(viewer.getX(), viewer.getZ()));
        }
        if (data.contains("ClassicFogCreation", Tag.TAG_LONG)) {
            var plan = ClassicWeatherFog.plan(far, mc.level.getDayTime(), data.getLong("ClassicFogCreation"));
            if (plan != null) far = plan.apply(far, biomeDistance(mc.level, viewer.getX(), viewer.getZ()));
        }
        return Math.min(full, far);
    }

    private static double biomeDistance(ClientLevel current, double x, double z) {
        if (!columnLoaded(current, (int)Math.floor(x), (int)Math.floor(z))) return 10000;
        boolean changed = current != biomeLevel;
        if (changed) {
            biomeLevel = current;
            biomes = new ClassicFogBiomes(new ClimateView(current));
        }
        long tick = current.getGameTime();
        if (changed || tick != biomeTick) {
            if (changed || biomeX != x || biomeZ != z) {
                biomeDistance = biomes.distance(x, z);
                biomeX = x; biomeZ = z;
            }
            biomeTick = tick;
        }
        return biomeDistance;
    }

    private static boolean columnLoaded(ClientLevel current, int x, int z) {
        var chunk = current.getChunkSource().getChunk(x >> 4, z >> 4, false);
        return chunk != null && !chunk.isEmpty();
    }

    static void chunkLoaded(ClientLevel current, net.minecraft.world.level.ChunkPos pos) {
        if (current != biomeLevel || biomes == null) return;
        int x = (int)Math.floor(biomeX), z = (int)Math.floor(biomeZ);
        if (pos.x < (x - 32 >> 4) || pos.x > (x + 32 >> 4)
                || pos.z < (z - 32 >> 4) || pos.z > (z + 32 >> 4)) return;
        biomeLevel = null; biomes = null; biomeTick = Long.MIN_VALUE;
    }

    static void unload(ClientLevel old) {
        if (level == old) { level = null; field = null; }
        if (biomeLevel == old) { biomeLevel = null; biomes = null; biomeTick = Long.MIN_VALUE; }
    }

    private static final class ClimateView implements ClassicFogBiomes.View {
        private final ClientLevel level;
        ClimateView(ClientLevel level) { this.level = level; }
        public boolean loaded(int x, int z) {
            return columnLoaded(level, x, z);
        }
        public boolean supports(int x, int z) {
            if (!loaded(x, z)) return false;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            var biome = level.getBiome(new BlockPos(x, y, z)).value();
            return biome.getModifiedClimateSettings().downfall() > 0 && biome.getBaseTemperature() > .15f;
        }
    }

    private ClassicFogEnvironment() {}
}
