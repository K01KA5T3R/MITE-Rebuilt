package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Bridges original Nether terrain and caves to the Forge chunk pipeline. */
final class ClassicNetherChunkGenerator extends ChunkGenerator {
    private static final int HEIGHT = 128;
    static final Codec<ClassicNetherChunkGenerator> SERIALIZER = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(generator -> generator.seed),
        Biome.CODEC.fieldOf("biome").forGetter(generator -> generator.biome)
    ).apply(instance, ClassicNetherChunkGenerator::new));

    private final long seed;
    private final Holder<Biome> biome;
    private final OriginalNetherTerrain terrain;
    private final OriginalNetherCarver carver;

    ClassicNetherChunkGenerator(long seed, Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.seed = seed;
        this.biome = biome;
        terrain = new OriginalNetherTerrain(seed);
        carver = new OriginalNetherCarver(seed);
    }

    @Override protected Codec<? extends ChunkGenerator> codec() { return SERIALIZER; }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                         StructureManager structures, ChunkAccess chunk) {
        var pos = chunk.getPos();
        var blocks = terrain.surface(pos.x, pos.z);
        carver.carve(pos.x, pos.z, blocks);
        var target = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            for (int y = 0; y < HEIGHT; y++) {
                var state = state(blocks[((x * 16 + z) << 7) + y], y);
                if (!state.isAir()) chunk.setBlockState(target.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z), state, false);
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    @Override public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {}
    @Override public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomes,
                                      StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {}
    @Override
    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structures) {
        super.applyBiomeDecoration(world, chunk, structures);
        var pos = chunk.getPos();
        var random = terrain.decorationRandom(pos.x, pos.z);
        ClassicNetherDecoration.decorate(world, random, pos.getMinBlockX(), pos.getMinBlockZ());
    }

    @Override public void spawnOriginalMobs(WorldGenRegion level) {}
    @Override public int getMinY() { return 0; }
    @Override public int getGenDepth() { return HEIGHT; }
    @Override public int getSeaLevel() { return 32; }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        var blocks = terrain.surface(x >> 4, z >> 4);
        int start = ((x & 15) * 16 + (z & 15)) * HEIGHT;
        var column = new BlockState[HEIGHT];
        for (int y = 0; y < HEIGHT; y++) column[y] = state(blocks[start + y], y);
        return new NoiseColumn(0, column);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor height, RandomState random) {
        var column = getBaseColumn(x, z, height, random);
        for (int y = Math.min(HEIGHT, height.getMaxBuildHeight()) - 1; y >= 0; y--) {
            if (type.isOpaque().test(column.getBlock(y))) return y + 1;
        }
        return 0;
    }

    @Override public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos position) {}

    private static BlockState state(byte block, int y) {
        return switch (block) {
            case OriginalNetherTerrain.AIR -> Blocks.AIR.defaultBlockState();
            case OriginalNetherTerrain.NETHERRACK -> Blocks.NETHERRACK.defaultBlockState();
            case OriginalNetherTerrain.LAVA -> Blocks.LAVA.defaultBlockState();
            case OriginalNetherTerrain.GRAVEL -> Content.NETHER_GRAVEL.get().defaultBlockState();
            case OriginalNetherTerrain.SOUL_SAND -> Blocks.SOUL_SAND.defaultBlockState();
            case OriginalNetherTerrain.CORE -> (y == 0 ? SourceUnderworldContent.CORE : SourceUnderworldContent.MANTLE).get().defaultBlockState();
            default -> throw new IllegalArgumentException("Unmapped classic Nether block " + block);
        };
    }
}

