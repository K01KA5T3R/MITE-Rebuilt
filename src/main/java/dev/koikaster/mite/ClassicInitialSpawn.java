package dev.koikaster.mite;

import java.util.Random;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 js.b and acv choose the first classic Overworld spawn. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
final class ClassicInitialSpawn {
    private static final int ORIGINAL_SURFACE_START = 63;
    private static final int ORIGINAL_QUART_RADIUS = 64;
    private static final int ORIGINAL_SEARCH_WIDTH = ORIGINAL_QUART_RADIUS * 2 + 1;
    private static final int ORIGINAL_WALK_LIMIT = 1000;
    private static final int FALLBACK_STEP_QUARTS = 8;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void create(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel world) || world.dimension() != Level.OVERWORLD
            || !(world.getChunkSource().getGenerator() instanceof ClassicOverworldChunkGenerator generator)) return;
        var biomes = (ClassicOverworldBiomeSource) generator.getBiomeSource();
        var spawn = find(world, generator, biomes.layers());
        if (spawn == null) return;

        event.getSettings().setSpawn(spawn, 0);
        if (world.getServer().getWorldData().worldGenOptions().generateBonusChest()) {
            world.registryAccess().registry(Registries.CONFIGURED_FEATURE)
                .flatMap(registry -> registry.getHolder(MiscOverworldFeatures.BONUS_CHEST))
                .ifPresent(feature -> feature.value().place(world, generator, world.getRandom(), spawn));
        }
        event.setCanceled(true);
    }

    private static BlockPos find(ServerLevel world, ClassicOverworldChunkGenerator generator, OriginalOverworldBiomes biomes) {
        var random = new Random(world.getSeed());
        var anchor = chooseOriginalBiome(biomes, random);
        int x = anchor.getX();
        int z = anchor.getZ();
        for (int attempt = 0; attempt < ORIGINAL_WALK_LIMIT; attempt++) {
            var at = confirmedGround(world, generator, x, z, true);
            if (at != null) return at;
            x += random.nextInt(64) - random.nextInt(64);
            z += random.nextInt(64) - random.nextInt(64);
        }

        // R195 stops its random walk even when the limit leaves it in the sea.
        // Keep the world playable with a bounded search of actual nearby land.
        int previousBoundary = -FALLBACK_STEP_QUARTS;
        for (int boundary : new int[]{128, 256, 512, 1024}) {
            var at = scanBeyond(world, generator, biomes, previousBoundary, boundary);
            if (at != null) return at;
            previousBoundary = boundary;
        }
        return null;
    }

    static BlockPos chooseOriginalBiome(OriginalOverworldBiomes biomes, Random random) {
        int[] cells = biomes.terrain(-ORIGINAL_QUART_RADIUS, -ORIGINAL_QUART_RADIUS,
            ORIGINAL_SEARCH_WIDTH, ORIGINAL_SEARCH_WIDTH);
        var chosen = BlockPos.ZERO;
        int replacements = 0;
        for (int index = 0; index < cells.length; index++) {
            if (!originalBiome(cells[index]) || replacements > 0 && random.nextInt(replacements + 1) != 0) continue;
            int x = (index % ORIGINAL_SEARCH_WIDTH - ORIGINAL_QUART_RADIUS) * 4;
            int z = (index / ORIGINAL_SEARCH_WIDTH - ORIGINAL_QUART_RADIUS) * 4;
            chosen = new BlockPos(x, 0, z);
            // In acv the counter advances only when a new candidate replaces the previous one.
            replacements++;
        }
        return chosen;
    }

    private static boolean originalBiome(int biome) {
        return switch (biome) {
            case OriginalOverworldBiomes.FOREST, OriginalOverworldBiomes.PLAINS, OriginalOverworldBiomes.TAIGA,
                OriginalOverworldBiomes.TAIGA_HILLS, OriginalOverworldBiomes.FOREST_HILLS,
                OriginalOverworldBiomes.JUNGLE, OriginalOverworldBiomes.JUNGLE_HILLS -> true;
            default -> false;
        };
    }

    private static BlockPos scanBeyond(ServerLevel world, ClassicOverworldChunkGenerator generator,
                                     OriginalOverworldBiomes biomes, int previousBoundary, int boundary) {
        int width = boundary * 2 + 1;
        var cells = biomes.terrain(-boundary, -boundary, width, width);
        BlockPos dryGround = null;
        for (int ring = previousBoundary + FALLBACK_STEP_QUARTS; ring <= boundary; ring += FALLBACK_STEP_QUARTS) {
            for (int z = -ring; z <= ring; z += FALLBACK_STEP_QUARTS) {
                boolean outerRow = Math.abs(z) == ring;
                int xStride = outerRow || ring == 0 ? FALLBACK_STEP_QUARTS : ring * 2;
                for (int x = -ring; x <= ring; x += xStride) {
                    int id = cells[(z + boundary) * width + (x + boundary)];
                    if (id == OriginalOverworldBiomes.OCEAN || id == OriginalOverworldBiomes.FROZEN_OCEAN) continue;
                    boolean grass = originalBiome(id);
                    if (!grass && dryGround != null) continue;
                    var feet = confirmedGround(world, generator, x * 4, z * 4, grass);
                    if (feet == null) continue;
                    if (grass) return feet;
                    dryGround = feet;
                }
            }
        }
        return dryGround;
    }

    private static BlockPos confirmedGround(ServerLevel world, ClassicOverworldChunkGenerator generator,
                                            int x, int z, boolean requireGrass) {
        if (!world.getWorldBorder().isWithinBounds(new BlockPos(x, ORIGINAL_SURFACE_START, z))) return null;
        int lastFloor = Math.min(world.getMaxBuildHeight(), generator.getGenDepth()) - 3;
        var estimated = generator.getBaseColumn(x, z, world, world.getChunkSource().randomState());
        int estimate = surfaceFloor(estimated::getBlock, lastFloor);
        if (estimate < 0 || !acceptable(estimated.getBlock(estimate), requireGrass)
            || !estimated.getBlock(estimate + 2).isAir()) return null;

        var chunk = world.getChunk(x >> 4, z >> 4);
        var cursor = new BlockPos.MutableBlockPos(x, estimate, z);
        IntFunction<BlockState> real = y -> chunk.getBlockState(cursor.setY(y));
        int floor = surfaceFloor(real, lastFloor);
        if (floor < 0 || !acceptable(real.apply(floor), requireGrass) || !real.apply(floor + 2).isAir()) return null;
        var ground = new BlockPos(x, floor, z);
        return real.apply(floor).isFaceSturdy(world, ground, Direction.UP) ? ground.above() : null;
    }

    private static int surfaceFloor(IntFunction<BlockState> stateAt, int lastFloor) {
        for (int y = ORIGINAL_SURFACE_START; y <= lastFloor; y++) {
            if (stateAt.apply(y + 1).isAir()) return y;
        }
        return -1;
    }

    private static boolean acceptable(BlockState state, boolean requireGrass) {
        return requireGrass ? state.is(Blocks.GRASS_BLOCK) : state.isSolid() && state.getFluidState().isEmpty()
            && !state.is(Blocks.CACTUS) && !state.is(Blocks.POWDER_SNOW) && !(state.getBlock() instanceof net.minecraft.world.level.block.FallingBlock);
    }

    private ClassicInitialSpawn() {}
}

