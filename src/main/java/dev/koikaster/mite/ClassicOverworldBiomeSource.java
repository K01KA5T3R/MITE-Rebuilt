package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/** Maps original 2D biome layers to holders required by the host chunk format. */
final class ClassicOverworldBiomeSource extends BiomeSource {
    private static final int TILE_SIZE = 12;
    private static final int BLOCK_TILE_SIZE = 16;
    private static final List<ResourceKey<Biome>> HOST_BIOMES = List.of(
        Biomes.OCEAN, Biomes.PLAINS, Biomes.DESERT, Biomes.WINDSWEPT_HILLS, Biomes.FOREST,
        Biomes.SNOWY_TAIGA, Biomes.SWAMP, Biomes.RIVER, Biomes.NETHER_WASTES, Biomes.THE_END,
        Biomes.FROZEN_OCEAN, Biomes.FROZEN_RIVER, Biomes.SNOWY_PLAINS, Biomes.SNOWY_SLOPES,
        Biomes.MUSHROOM_FIELDS, Biomes.MUSHROOM_FIELDS, Biomes.BEACH, Biomes.DESERT, Biomes.FOREST,
        Biomes.SNOWY_TAIGA, Biomes.WINDSWEPT_FOREST, Biomes.JUNGLE, Biomes.JUNGLE, Biomes.RIVER,
        Biomes.RIVER, Biomes.RIVER);

    static final Codec<ClassicOverworldBiomeSource> SERIALIZER = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("seed").forGetter(source -> source.seed),
        Codec.BOOL.fieldOf("large_biomes").forGetter(source -> source.largeBiomes),
        Biome.CODEC.listOf().fieldOf("biomes").forGetter(source -> source.biomes)
    ).apply(instance, ClassicOverworldBiomeSource::new));

    private record Tile(int x, int z, int[] ids) {}
    private final long seed;
    private final boolean largeBiomes;
    private final OriginalOverworldBiomes layers;
    private final List<Holder<Biome>> biomes;
    private final ThreadLocal<Tile> previous = new ThreadLocal<>();
    private final ThreadLocal<Tile> previousBlockTile = new ThreadLocal<>();

    ClassicOverworldBiomeSource(long seed, boolean largeBiomes, Registry<Biome> registry) {
        this(seed, largeBiomes, HOST_BIOMES.stream().<Holder<Biome>>map(registry::getHolderOrThrow).toList());
    }

    long seed() {
        return seed;
    }

    private ClassicOverworldBiomeSource(long seed, boolean largeBiomes, List<Holder<Biome>> biomes) {
        if (biomes.size() != HOST_BIOMES.size()) throw new IllegalArgumentException("Incomplete classic biome registry");
        this.seed = seed;
        this.largeBiomes = largeBiomes;
        this.biomes = List.copyOf(biomes);
        layers = new OriginalOverworldBiomes(seed, largeBiomes);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return SERIALIZER;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return java.util.stream.IntStream.range(0, biomes.size())
            .filter(id -> id != 8 && id != 9 && id != 14 && id != 15)
            .mapToObj(biomes::get).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        var tile = previous.get();
        if (tile == null || quartX < tile.x() || quartX >= tile.x() + TILE_SIZE
                || quartZ < tile.z() || quartZ >= tile.z() + TILE_SIZE) {
            int x = Math.floorDiv(quartX, TILE_SIZE) * TILE_SIZE;
            int z = Math.floorDiv(quartZ, TILE_SIZE) * TILE_SIZE;
            tile = new Tile(x, z, layers.terrain(x, z, TILE_SIZE, TILE_SIZE));
            previous.set(tile);
        }
        return biomes.get(tile.ids()[(quartZ - tile.z()) * TILE_SIZE + quartX - tile.x()]);
    }

    int originalBlockBiome(int x, int z) {
        int minX = Math.floorDiv(x, BLOCK_TILE_SIZE) * BLOCK_TILE_SIZE;
        int minZ = Math.floorDiv(z, BLOCK_TILE_SIZE) * BLOCK_TILE_SIZE;
        var tile = previousBlockTile.get();
        if (tile == null || tile.x() != minX || tile.z() != minZ) {
            tile = new Tile(minX, minZ, layers.blocks(minX, minZ, BLOCK_TILE_SIZE, BLOCK_TILE_SIZE));
            previousBlockTile.set(tile);
        }
        return tile.ids()[(z - minZ) * BLOCK_TILE_SIZE + x - minX];
    }

    OriginalOverworldBiomes layers() {
        return layers;
    }
}
