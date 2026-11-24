package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/** R195 central island and naturally decorated obsidian pillars. */
final class ClassicEndChunkGenerator extends ChunkGenerator {
    private static final int HEIGHT = 128, END_STONE = 121;
    static final Codec<ClassicEndChunkGenerator> SERIALIZER = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(generator -> generator.seed),
        Biome.CODEC.fieldOf("biome").forGetter(generator -> generator.biome)
    ).apply(instance, ClassicEndChunkGenerator::new));

    private final long seed;
    private final Holder<Biome> biome;
    private final OriginalEndTerrain terrain;

    ClassicEndChunkGenerator(long seed, Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.seed = seed;
        this.biome = biome;
        terrain = new OriginalEndTerrain(seed);
    }

    @Override protected Codec<? extends ChunkGenerator> codec() { return SERIALIZER; }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                         StructureManager structures, ChunkAccess chunk) {
        var pos = chunk.getPos();
        byte[] blocks = terrain.blocks(pos.x, pos.z);
        var at = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int base = (x * 16 + z) * HEIGHT;
            for (int y = 0; y < HEIGHT; y++) {
                if (blocks[base + y] == END_STONE) {
                    chunk.setBlockState(at.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z),
                        Blocks.END_STONE.defaultBlockState(), false);
                }
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
        int originX = pos.getMinBlockX();
        int originZ = pos.getMinBlockZ();
        // acu seeds the decorator with block coordinates. Its ore pass is empty in the End.
        var random = new Random((long) (originX + originZ * 65536) + seed * 0x100000000L);
        if (random.nextInt(5) != 0) return;
        int x = originX + random.nextInt(16) + 8;
        int z = originZ + random.nextInt(16) + 8;
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        var center = new BlockPos(x, y, z);
        if (y <= 1 || !world.isEmptyBlock(center) || !world.getBlockState(center.below()).is(Blocks.END_STONE)) return;

        int height = random.nextInt(32) + 6;
        int radius = random.nextInt(4) + 1;
        var at = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius + 1) continue;
                if (!world.getBlockState(at.set(x + dx, y - 1, z + dz)).is(Blocks.END_STONE)) return;
            }
        }
        for (int dy = 0; dy < height && y + dy < HEIGHT; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius + 1) {
                        world.setBlock(at.set(x + dx, y + dy, z + dz), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        int cap = y + height;
        var crystal = new SourceEndCrystal(world.getLevel(), x + .5, cap, z + .5);
        crystal.setYRot(random.nextFloat() * 360.0f);
        world.addFreshEntity(crystal);
        world.setBlock(center.atY(cap), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override public void spawnOriginalMobs(WorldGenRegion level) {}
    @Override public int getMinY() { return 0; }
    @Override public int getGenDepth() { return HEIGHT; }
    @Override public int getSeaLevel() { return 0; }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        var blocks = terrain.blocks(x >> 4, z >> 4);
        int start = ((x & 15) * 16 + (z & 15)) * HEIGHT;
        var column = new BlockState[HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            column[y] = blocks[start + y] == END_STONE ? Blocks.END_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
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
}
