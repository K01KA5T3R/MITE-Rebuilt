package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Private Forge adapter for the R195 terrain. */
final class ClassicOverworldChunkGenerator extends ChunkGenerator {
    static final Codec<ClassicOverworldChunkGenerator> SERIALIZER = RecordCodecBuilder.create(instance -> instance.group(
        ClassicOverworldBiomeSource.SERIALIZER.fieldOf("biome_source").forGetter(generator -> generator.classicBiomes)
    ).apply(instance, ClassicOverworldChunkGenerator::new));

    private final ClassicOverworldBiomeSource classicBiomes;
    private final OriginalOverworldDensity density;
    private final OriginalOverworldCarvers carvers;
    private final MinecraftServer server;
    private final LinkedHashMap<Long, byte[]> landSurface = new LinkedHashMap<>(32, .75f, true);

    private ClassicOverworldChunkGenerator(ClassicOverworldBiomeSource biomes) {
        this(biomes, null);
    }

    ClassicOverworldChunkGenerator(ClassicOverworldBiomeSource biomes, MinecraftServer server) {
        super(biomes);
        classicBiomes = biomes;
        this.server = server;
        density = new OriginalOverworldDensity(biomes.seed());
        carvers = new OriginalOverworldCarvers(biomes.seed(), biomes.layers());
    }

    boolean permitsStructure(Holder<Structure> structure, ChunkPos chunk) {
        if (structure.is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.MINESHAFT)) {
            return ClassicMineshaftPlacement.allowsBiome(classicBiomes, chunk.getMinBlockX(), chunk.getMinBlockZ());
        }
        if (!structure.is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.DESERT_PYRAMID)
                && !structure.is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.JUNGLE_TEMPLE)
                && !structure.is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT)) return true;
        var overworld = server == null ? null : server.overworld();
        return ClassicTemplePlacement.permits(structure, overworld, chunk, classicBiomes);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return SERIALIZER;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState random, long seed) {
        if (classicBiomes.seed() != seed) throw new IllegalStateException("Classic terrain seed must match the save");
        return super.createState(structureSets, random, seed);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                         StructureManager structures, ChunkAccess chunk) {
        var pos = chunk.getPos();
        var surfaceRandom = OriginalOverworldDensity.surfaceRandom(pos.x, pos.z);
        var blocks = density.surfaceBlocks(pos.x, pos.z, classicBiomes.layers(), surfaceRandom);
        carvers.carve(pos.x, pos.z, blocks, surfaceRandom);
        var target = new BlockPos.MutableBlockPos();
        int bottom = Math.max(getMinY(), chunk.getMinBuildHeight());
        int limit = Math.min(getGenDepth(), chunk.getMaxBuildHeight());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = bottom; y < limit; y++) {
                    var state = state(blocks[((x * 16 + z) << 7) + y]);
                    if (!state.isAir()) chunk.setBlockState(target.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z), state, false);
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structures) {
        super.applyBiomeDecoration(world, chunk, structures);
        ClassicSnowCover.decorate(world, chunk, classicBiomes.layers());
        if (!chunk.isUpgrading() && world instanceof WorldGenRegion region) populate(region, chunk);
    }

    @Override public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {}
    @Override public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomes,
                                      StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {}
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // The native spawn stage exposes only the center chunk.
    }

    private void populate(WorldGenRegion region, ChunkAccess center) {
        ChunkPos chunk = center.getPos();
        int x = chunk.getMinBlockX() + 8;
        int z = chunk.getMinBlockZ() + 8;
        int originalBiome = classicBiomes.layers().blocks(x + 8, z + 8, 1, 1)[0];
        float probability = ClassicSnowCover.freezing(originalBiome) ? 0.05F : 0.1F;
        var world = new SourceWorldgenWorld(region, center) {
            @Override
            public float worldgenProbability(Object biome) {
                return probability;
            }

            @Override
            public List<SourceSpawningKernel.Entry> worldgenRows(Object biome, SourceSpawningKernel.Group group) {
                return ClassicInitialPopulation.rows(originalBiome, group);
            }
        };
        var random = net.minecraft.util.RandomSource.create(region.getSeed());
        long xSalt = random.nextLong() / 2 * 2 + 1;
        long zSalt = random.nextLong() / 2 * 2 + 1;
        random.setSeed((chunk.x * xSalt + chunk.z * zSalt) ^ region.getSeed());
        var sequence = SourceWorldgenSpawning.adapt(random);
        SourceSpawningKernel.worldgen(world, originalBiome, SourceSpawningKernel.Group.ANIMAL, x, z, 16, 16, sequence);
        SourceSpawningKernel.worldgen(world, originalBiome, SourceSpawningKernel.Group.AQUATIC, x, z, 16, 16, sequence);
    }
    @Override public int getMinY() { return 0; }
    @Override public int getGenDepth() { return OriginalOverworldDensity.HEIGHT; }
    @Override public int getSeaLevel() { return OriginalOverworldDensity.SEA_LEVEL; }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        var blocks = surface(x >> 4, z >> 4);
        int start = ((x & 15) * 16 + (z & 15)) << 7;
        var states = new BlockState[getGenDepth()];
        for (int y = 0; y < states.length; y++) states[y] = state(blocks[start + y]);
        return new NoiseColumn(0, states);
    }

    private synchronized byte[] surface(int chunkX, int chunkZ) {
        long key = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
        var blocks = landSurface.computeIfAbsent(key, ignored -> density.surfaceBlocks(chunkX, chunkZ, classicBiomes.layers()));
        if (landSurface.size() > 32) landSurface.remove(landSurface.keySet().iterator().next());
        return blocks;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor height, RandomState random) {
        var column = getBaseColumn(x, z, height, random);
        for (int y = Math.min(getGenDepth(), height.getMaxBuildHeight()) - 1; y >= getMinY(); y--) {
            if (type.isOpaque().test(column.getBlock(y))) return y + 1;
        }
        return getMinY();
    }

    @Override public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos position) {}

    private static BlockState state(byte block) {
        return switch (block) {
            case OriginalOverworldDensity.AIR -> Blocks.AIR.defaultBlockState();
            case OriginalOverworldDensity.STONE -> Blocks.STONE.defaultBlockState();
            case OriginalOverworldDensity.GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
            case OriginalOverworldDensity.DIRT -> Blocks.DIRT.defaultBlockState();
            case OriginalOverworldDensity.BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            case OriginalOverworldDensity.WATER -> Blocks.WATER.defaultBlockState();
            case OriginalOverworldDensity.SAND -> Blocks.SAND.defaultBlockState();
            case OriginalOverworldDensity.SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
            case OriginalOverworldDensity.ICE -> Blocks.ICE.defaultBlockState();
            case OriginalOverworldCarvers.LAVA -> Blocks.LAVA.defaultBlockState();
            case OriginalOverworldCarvers.COBWEB -> Blocks.COBWEB.defaultBlockState();
            case OriginalCaveNetwork.MYCELIUM -> Blocks.MYCELIUM.defaultBlockState();
            case OriginalCaveNetwork.CLAY -> Blocks.CLAY.defaultBlockState();
            default -> throw new IllegalArgumentException("Unmapped classic terrain block " + block);
        };
    }
}
