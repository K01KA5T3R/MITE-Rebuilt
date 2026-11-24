package dev.koikaster.mite;

import com.google.gson.JsonParser;
import dev.koikaster.mite.visual.ClassicVisualState;
import dev.koikaster.mite.visual.ClassicVisuals;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Shared R195 bet/beo update using the existing particle movement adapter. */
public final class ClientSourceRain2146 {
    private record Form(boolean solid, boolean liquid, boolean alwaysCube, int precipitationMask) {}
    private static final Map<ResourceLocation, Form> FORMS = originalForms();

    public static final class State {
        private final ClientLevel level;
        private final ClientSourceIcon1001.World world;
        private final SourceIconMotion1014.State motion;

        private State(ClientLevel level, double x, double y, double z, RandomSource random) {
            this.level = level;
            world = new ClientSourceIcon1001.World(level);
            motion = new SourceIconMotion1014.State(x, y, z);
            // R195 beg starts with a .2 box and a .1 visual offset. bet shrinks the box at its existing minimum.
            motion.width = .01f;
            motion.height = .01f;
            motion.box.d = motion.box.a + motion.width;
            motion.box.e = motion.box.b + motion.height;
            motion.box.f = motion.box.c + motion.width;

            double dx = random.nextFloat() * .8f - (double) .4f;
            double dy = random.nextFloat() * .8f - (double) .4f;
            double dz = random.nextFloat() * .8f - (double) .4f;
            float speed = (random.nextFloat() * random.nextFloat() + 1f) * .15f;
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0f) {
                motion.vx = dx / length * speed * (double) .4f * (double) .3f;
                motion.vz = dz / length * speed * (double) .4f * (double) .3f;
            }
        }

        public double x() { return motion.x; }
        public double y() { return motion.y; }
        public double z() { return motion.z; }
        public double xd() { return motion.vx; }
        public double yd() { return motion.vy; }
        public double zd() { return motion.vz; }
        public boolean onGround() { return motion.onGround; }
        public AABB box() {
            var box = motion.box;
            return new AABB(box.a, box.b, box.c, box.d, box.e, box.f);
        }
    }

    private static boolean enabled(Object particle) {
        return ClassicVisualState.ready && ClassicVisuals.particles()
            && (particle.getClass() == WaterDropParticle.class || particle.getClass() == SplashParticle.class);
    }

    public static double initialVertical(double random, Object particle) {
        if (enabled(particle)) return (float) random * .2f + .1f;
        return random * (double) .2f + (double) .1f;
    }

    public static State create(Object particle, ClientLevel level, double x, double y, double z, RandomSource random) {
        return enabled(particle) ? new State(level, x, y, z, random) : null;
    }

    public static boolean tick(State state, double x, double y, double z, double xd, double yd, double zd,
                               float gravity, AABB box, boolean physics, int lifetime) {
        var motion = state.motion;
        motion.x = x;
        motion.y = y;
        motion.z = z;
        motion.vx = xd;
        motion.vy = yd - gravity;
        motion.vz = zd;
        motion.noClip = !physics;
        motion.box = ClientSourceIcon1001.World.box(box);
        SourceIconMotion1014.move(motion, state.world, motion.vx, motion.vy, motion.vz);
        motion.vx *= (double) .98f;
        motion.vy *= (double) .98f;
        motion.vz *= (double) .98f;

        boolean expired = lifetime <= 0;
        if (motion.onGround) {
            expired |= Math.random() < .5;
            motion.vx *= (double) .7f;
            motion.vz *= (double) .7f;
        }
        if (motion.vy > 0) return expired;
        return contactsSurface(state.level, motion.x, motion.y, motion.z, motion.vy) || expired;
    }

    private static Form form(BlockState state) {
        return FORMS.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    static boolean contactsSurface(ClientLevel level, double x, double y, double z, double yd) {
        double nextY = y + yd;
        var pos = BlockPos.containing(x, y, z);
        if (precipitationAbove(level, pos.getX(), pos.getZ(), Mth.floor(nextY))) return true;
        var block = level.getBlockState(pos);
        if (block.isAir()) return false;
        var original = form(block);
        if (original == null ? block.blocksMotion() : original.solid) {
            // Legacy single and double slabs had separate IDs. A flattened slab's current state selects its old form.
            if (standardGround(level, pos, block)) return true;
            var outline = block.getShape(level, pos);
            // bet compares world y with the original block-local maxY for this branch.
            return !outline.isEmpty() && y < outline.max(Direction.Axis.Y);
        }
        if ((original == null ? SourcePhaseNativeWorld.liquid(block) : original.liquid) && block.getBlock() instanceof LiquidBlock) {
            int metadata = block.getValue(LiquidBlock.LEVEL);
            float surface = LegacyRainSpawn2160.liquidSurface(pos.getY() + 1, metadata);
            return nextY < surface;
        }
        return false;
    }

    static boolean standardGround(ClientLevel level, BlockPos pos, BlockState block) {
        var original = form(block);
        return original == null || block.getBlock() instanceof SlabBlock
            ? SourceBlockForms716.standard(block, level, pos) : original.alwaysCube;
    }

    static final int NO_SURFACE = Integer.MIN_VALUE;

    private static boolean precipitationAbove(ClientLevel level, int x, int z, int nextBlockY) {
        return precipitationHeight(level, x, z, nextBlockY + 1) != NO_SURFACE;
    }

    /** First block above the R195 precipitation surface, limited to loaded columns. */
    static int precipitationHeight(ClientLevel level, int x, int z, int lowestBlock) {
        if (!level.hasChunk(x >> 4, z >> 4)) return NO_SURFACE;
        int upper = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        int lower = Math.max(level.getMinBuildHeight(), lowestBlock);
        var pos = new BlockPos.MutableBlockPos(x, upper, z);
        for (int y = upper - 1; y >= lower; y--) {
            pos.setY(y);
            var block = level.getBlockState(pos);
            if (block.isAir()) continue;
            var original = form(block);
            if (original != null && original.precipitationMask == 0) continue;
            if (original != null && original.precipitationMask == 0xffff || SourceBlockForms716.precipitation(block, level, pos)) {
                return y + 1;
            }
        }
        return NO_SURFACE;
    }

    private static Map<ResourceLocation, Form> originalForms() {
        try (var stream = ClientSourceRain2146.class.getResourceAsStream("/assets/mite_reforged/rain_contact_forms2146.json")) {
            if (stream == null) throw new IOException("Missing R195 rain material table");
            var raw = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<ResourceLocation, Form> result = new HashMap<>();
            for (var entry : raw.entrySet()) {
                var row = entry.getValue().getAsJsonObject();
                result.put(new ResourceLocation(entry.getKey()), new Form(row.get("solid").getAsBoolean(),
                    row.get("liquid").getAsBoolean(), row.get("always_cube").getAsBoolean(), row.get("precipitation_mask").getAsInt()));
            }
            return Map.copyOf(result);
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private ClientSourceRain2146() {}
}

