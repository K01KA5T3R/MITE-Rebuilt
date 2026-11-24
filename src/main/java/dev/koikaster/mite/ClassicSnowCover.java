package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** R195 aet's post-decoration ice and snow pass in the host chunk. */
final class ClassicSnowCover {
    static void decorate(WorldGenLevel world, ChunkAccess chunk, OriginalOverworldBiomes biomes) {
        int x0 = chunk.getPos().getMinBlockX() + 8;
        int z0 = chunk.getPos().getMinBlockZ() + 8;
        int[] originalBiomes = biomes.blocks(x0, z0, 16, 16);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (!freezing(originalBiomes[z * 16 + x])) continue;
                int bx = x0 + x, bz = z0 + z;
                int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
                var snow = new BlockPos(bx, top, bz);
                freeze(world, snow.below());
                if (top > 63 && world.isEmptyBlock(snow.atY(63))) freeze(world, snow.atY(62));
                snow(world, snow);
            }
        }
    }

    static boolean freezing(int biome) {
        return switch (biome) {
            case OriginalOverworldBiomes.TAIGA, OriginalOverworldBiomes.TAIGA_HILLS,
                 OriginalOverworldBiomes.ICE_PLAINS, OriginalOverworldBiomes.ICE_MOUNTAINS,
                 OriginalOverworldBiomes.FROZEN_OCEAN, OriginalOverworldBiomes.FROZEN_RIVER -> true;
            default -> false;
        };
    }

    private static void freeze(WorldGenLevel world, BlockPos pos) {
        if (!writableLowLight(world, pos)) return;
        var state = world.getBlockState(pos);
        if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
            world.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void snow(WorldGenLevel world, BlockPos pos) {
        if (!writableLowLight(world, pos) || !world.isEmptyBlock(pos)) return;
        var support = world.getBlockState(pos.below());
        if (support.is(Blocks.ICE)) return;
        var state = Blocks.SNOW.defaultBlockState();
        if (support.is(BlockTags.LEAVES) || state.canSurvive(world, pos)) {
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean writableLowLight(WorldGenLevel world, BlockPos pos) {
        return !world.isOutsideBuildHeight(pos) && world.ensureCanWrite(pos) && world.getBrightness(LightLayer.BLOCK, pos) < 10;
    }

    private ClassicSnowCover() {}
}
