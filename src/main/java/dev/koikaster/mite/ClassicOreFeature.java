package dev.koikaster.mite;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** The R195 acu ore decorator, with the already ported afq growth rules. */
public final class ClassicOreFeature {
    private enum Mineral {
        DIRT(() -> Blocks.DIRT, 32, 200, 0, 32, 128, false, false),
        GRAVEL(() -> Blocks.GRAVEL, 32, 200, 300, 24, 128, false, false),
        COAL(() -> Blocks.COAL_ORE, 16, 50, 0, 16, 96, false, true),
        COPPER(() -> Blocks.COPPER_ORE, 6, 40, 40, 0, 128, true, true),
        SILVER(() -> ore("silver_ore"), 6, 10, 10, 0, 96, true, true),
        GOLD(() -> Blocks.GOLD_ORE, 4, 20, 20, 0, 48, true, true),
        IRON(() -> Blocks.IRON_ORE, 6, 60, 60, 0, 64, true, true),
        MITHRIL(() -> ore("mithril_ore"), 3, 10, 10, 0, 32, true, true),
        ADAMANTIUM(() -> ore("adamantium_ore"), 3, 0, 5, 0, 24, true, true),
        INFESTED(() -> Blocks.INFESTED_STONE, 3, 5, 50, 0, 24, true, false),
        REDSTONE(() -> Blocks.REDSTONE_ORE, 5, 10, 10, 0, 24, false, false),
        DIAMOND(() -> Blocks.DIAMOND_ORE, 3, 5, 5, 0, 32, false, true),
        LAPIS(() -> Blocks.LAPIS_ORE, 3, 5, 5, 8, 40, false, true);

        final Supplier<Block> block;
        final int size, overworld, underworld, min, max;
        final boolean depth, ore;

        Mineral(Supplier<Block> block, int size, int overworld, int underworld, int min, int max, boolean depth, boolean ore) {
            this.block = block;
            this.size = size;
            this.overworld = overworld;
            this.underworld = underworld;
            this.min = min;
            this.max = max;
            this.depth = depth;
            this.ore = ore;
        }

        private int height(Random random, boolean underground) {
            if (underground) {
                if (this == ADAMANTIUM || ore && random.nextFloat() < .75f) return random.nextInt(136);
                return random.nextInt(256);
            }
            float height;
            if (this == LAPIS) height = (random.nextFloat() + random.nextFloat()) / 2.0f;
            else if (this == COPPER && random.nextInt(2) == 0) height = random.nextFloat() * .6f + .4f;
            else if (this == DIRT || this == GRAVEL || this == COAL) {
                do { height = random.nextFloat(); } while (!(height > random.nextFloat()));
            } else {
                do { height = random.nextFloat(); } while (!(height < random.nextFloat()));
            }
            return min + (int) (height * (max - min + 1));
        }
    }

    private static final List<Mineral> OVERWORLD = List.of(Mineral.DIRT, Mineral.GRAVEL, Mineral.COAL,
        Mineral.COPPER, Mineral.SILVER, Mineral.GOLD, Mineral.IRON, Mineral.MITHRIL, Mineral.INFESTED,
        Mineral.REDSTONE, Mineral.DIAMOND, Mineral.LAPIS);
    private static final List<Mineral> UNDERWORLD = List.of(Mineral.GRAVEL, Mineral.COPPER, Mineral.SILVER,
        Mineral.GOLD, Mineral.IRON, Mineral.MITHRIL, Mineral.ADAMANTIUM, Mineral.REDSTONE, Mineral.DIAMOND,
        Mineral.LAPIS, Mineral.INFESTED);

    private static Block ore(String path) {
        return BuiltInRegistries.BLOCK.getOptional(Content.id(path)).orElseThrow();
    }

    public static void decorate(WorldGenLevel world, ChunkAccess chunk) {
        var level = world.getLevel();
        if (WorldContentMode.rebuilt(level)) return;
        boolean underground = level.dimension() == WorldEvents.UNDERWORLD;
        if (!underground && level.dimension() != Level.OVERWORLD) return;

        var chunkPos = chunk.getPos();
        int originX = chunkPos.getMinBlockX();
        int originZ = chunkPos.getMinBlockZ();
        var random = new Random((long) (originX + originZ * 65536) + world.getSeed() * 0x100000000L);
        for (var mineral : underground ? UNDERWORLD : OVERWORLD) {
            int attempts = underground ? mineral.underworld : mineral.overworld;
            if (underground && mineral != Mineral.GRAVEL) attempts *= mineral == Mineral.ADAMANTIUM ? 16 : 8;
            var block = mineral.block.get();
            for (int i = 0; i < attempts; i++) {
                if (random.nextInt(10) != 0) continue;
                int x = originX + random.nextInt(16);
                int y = mineral.height(random, underground);
                int z = originZ + random.nextInt(16);
                var at = new BlockPos(x, y, z);
                if (!replaceable(world, at, Blocks.STONE)) continue;
                int size = veinSize(mineral, random, y, underground);
                grow(world, random, Blocks.STONE, at, size, candidate -> {
                    if (underground && mineral == Mineral.GRAVEL && (candidate.getY() < 1
                            || !SourcePortalMaterials.solid(world.getBlockState(candidate.below())))) return null;
                    if (mineral == Mineral.DIRT && world.canSeeSky(candidate.above())) {
                        return (world.getBiome(candidate).is(BiomeTags.IS_BEACH)
                            || world.getBiome(candidate).is(net.minecraft.world.level.biome.Biomes.DESERT)
                            ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState();
                    }
                    return block.defaultBlockState();
                });
            }
        }
        if (!underground && level.getChunkSource().getGenerator().getBiomeSource() instanceof ClassicOverworldBiomeSource biomes) {
            int decorationBiome = biomes.layers().blocks(originX + 16, originZ + 16, 1, 1)[0];
            mountainEmeralds(world, random, originX, originZ, decorationBiome);
        }
    }

    // R195 acx is only registered for Extreme Hills and Extreme Hills Edge.
    private static void mountainEmeralds(WorldGenLevel world, Random random, int originX, int originZ, int biome) {
        if (biome != OriginalOverworldBiomes.EXTREME_HILLS && biome != OriginalOverworldBiomes.MOUNTAIN_EDGE) return;
        int attempts = 3 + random.nextInt(6);
        var emerald = Blocks.EMERALD_ORE.defaultBlockState();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = originX + random.nextInt(16);
            int y = 4 + random.nextInt(28);
            int z = originZ + random.nextInt(16);
            var at = new BlockPos(x, y, z);
            if (replaceable(world, at, Blocks.STONE)) world.setBlock(at, emerald, Block.UPDATE_CLIENTS);
        }
    }

    private static int veinSize(Mineral mineral, Random random, int y, boolean underground) {
        int min = underground ? 0 : mineral.min;
        int range = underground ? 255 : mineral.max - min;
        boolean depth = mineral.depth && range > 16 && (!underground || mineral != Mineral.INFESTED);
        return veinSize(mineral.size, random, depth ? 1.0f - (float) (y - min) / range + .5f : 1.0f);
    }

    private static int veinSize(int base, Random random, float depthScale) {
        float scale = 1.0f;
        while (random.nextInt(2) == 0) scale = (float) ((double) scale * (random.nextFloat() * .6 + (double) .7f));
        scale = Math.min(scale, 4.0f);
        scale *= depthScale;
        if (base * scale <= 3.0f && random.nextInt(2) == 0) {
            if (random.nextInt(2) != 0) return 0;
            scale *= 2.0f;
        }
        int size = (int) (base * scale);
        if (size == 1) return random.nextInt(3) == 0 ? 3 : 0;
        if (size == 2) return random.nextInt(3) == 0 ? 0 : 3;
        return Math.min(size, 32);
    }

    static void netherVein(WorldGenLevel world, Random random, BlockPos at, Block block, int baseSize) {
        if (!replaceable(world, at, Blocks.NETHERRACK)) return;
        int size = veinSize(baseSize, random, 1.0f);
        var state = block.defaultBlockState();
        grow(world, random, Blocks.NETHERRACK, at, size, position -> state);
    }

    private static int grow(WorldGenLevel world, Random random, Block host, BlockPos at,
                            int remaining, Function<BlockPos, BlockState> placement) {
        if (remaining < 1 || !replaceable(world, at, host)) return 0;
        var state = placement.apply(at);
        if (state == null || !world.setBlock(at, state, Block.UPDATE_CLIENTS)) return 0;
        int grown = 1;
        for (int attempt = 0; attempt < 16; attempt++) {
            int axis = random.nextInt(3);
            int direction = random.nextInt(2) == 0 ? -1 : 1;
            var neighbor = axis == 0 ? at.offset(direction, 0, 0)
                : axis == 1 ? at.offset(0, direction, 0) : at.offset(0, 0, direction);
            grown += grow(world, random, host, neighbor, remaining - grown, placement);
            if (grown == remaining) break;
        }
        return grown;
    }

    private static boolean replaceable(WorldGenLevel world, BlockPos at, Block host) {
        return !world.isOutsideBuildHeight(at) && world.ensureCanWrite(at) && world.getBlockState(at).is(host);
    }

    private ClassicOreFeature() {}
}
