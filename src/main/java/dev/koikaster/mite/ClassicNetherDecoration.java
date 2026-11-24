package dev.koikaster.mite;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** The feature order and local generators from R195 aep and afj/afh/afn/afi/aff/afc. */
final class ClassicNetherDecoration {
    private static final Direction[] DIRECTIONS = Direction.values();

    static void decorate(WorldGenLevel world, Random random, int x, int z) {
        for (int i = 0; i < 8; i++) {
            spring(world, sample(random, x, z, 120, 4, 8), false);
        }
        int fires = random.nextInt(random.nextInt(10) + 1) + 1;
        for (int i = 0; i < fires; i++) {
            fire(world, random, sample(random, x, z, 120, 4, 8));
        }
        int hanging = random.nextInt(random.nextInt(10) + 1);
        for (int i = 0; i < hanging; i++) {
            glowstone(world, random, sample(random, x, z, 120, 4, 8));
        }
        for (int i = 0; i < 10; i++) {
            glowstone(world, random, sample(random, x, z, 128, 0, 8));
        }
        random.nextInt(1);
        brownMushroom(world, random, sample(random, x, z, 128, 0, 8));
        for (int i = 0; i < 16; i++) {
            witherwood(world, random, sample(random, x, z, 128, 0, 8));
        }
        veins(world, random, x, z, Blocks.NETHER_QUARTZ_ORE, 13, 16);
        veins(world, random, x, z, Blocks.NETHER_GOLD_ORE, 8, 2);
        veins(world, random, x, z, SourceInfestation.NETHER_STONE.get(), 8, random.nextInt(7) + 2);
        for (int i = 0; i < 16; i++) {
            spring(world, sample(random, x, z, 108, 10, 0), true);
        }
    }

    private static BlockPos sample(Random random, int x, int z, int rangeY, int minY, int offset) {
        return new BlockPos(x + random.nextInt(16) + offset, random.nextInt(rangeY) + minY, z + random.nextInt(16) + offset);
    }

    private static int spread(Random random, int range) {
        return random.nextInt(range) - random.nextInt(range);
    }

    private static void veins(WorldGenLevel world, Random random, int x, int z, Block block, int size, int attempts) {
        for (int i = 0; i < attempts; i++) {
            ClassicOreFeature.netherVein(world, random, sample(random, x, z, 108, 10, 0), block, size);
        }
    }

    private static void spring(WorldGenLevel world, BlockPos pos, boolean enclosed) {
        if (!world.getBlockState(pos.above()).is(Blocks.NETHERRACK)) return;
        var state = world.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.NETHERRACK)) return;
        int rock = 0, air = 0;
        for (var direction : DIRECTIONS) {
            if (direction == Direction.UP) continue;
            var adjacent = world.getBlockState(pos.relative(direction));
            if (adjacent.is(Blocks.NETHERRACK)) rock++;
            if (adjacent.isAir()) air++;
        }
        if (rock == 5 || !enclosed && rock == 4 && air == 1) {
            if (world.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS)) {
                world.scheduleTick(pos, Fluids.LAVA, 0);
            }
        }
    }

    private static void fire(WorldGenLevel world, Random random, BlockPos start) {
        for (int i = 0; i < 64; i++) {
            var pos = start.offset(spread(random, 8), spread(random, 4), spread(random, 8));
            if (world.isEmptyBlock(pos) && world.getBlockState(pos.below()).is(Blocks.NETHERRACK)) {
                world.setBlock(pos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void glowstone(WorldGenLevel world, Random random, BlockPos start) {
        if (!world.isEmptyBlock(start) || !world.getBlockState(start.above()).is(Blocks.NETHERRACK)) return;
        var state = Blocks.GLOWSTONE.defaultBlockState();
        if (!world.setBlock(start, state, Block.UPDATE_CLIENTS)) return;
        for (int i = 0; i < 1500; i++) {
            var pos = start.offset(spread(random, 8), -random.nextInt(12), spread(random, 8));
            if (world.isOutsideBuildHeight(pos) || !world.isEmptyBlock(pos)) continue;
            int neighbors = 0;
            for (var direction : DIRECTIONS) {
                if (world.getBlockState(pos.relative(direction)).is(Blocks.GLOWSTONE)) neighbors++;
            }
            if (neighbors == 1) world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void brownMushroom(WorldGenLevel world, Random random, BlockPos start) {
        var state = Blocks.BROWN_MUSHROOM.defaultBlockState();
        for (int i = 0; i < 64; i++) {
            int dy = spread(random, 4);
            if (start.getY() + dy < 0) continue;
            var pos = start.offset(spread(random, 8), dy, spread(random, 8));
            if (pos.getY() >= 127 || !world.isEmptyBlock(pos)) continue;
            var floor = world.getBlockState(pos.below());
            if (mushroomFloor(floor) && state.canSurvive(world, pos)) {
                world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean mushroomFloor(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.GRAVEL) || state.is(Content.NETHER_GRAVEL.get())
            || state.is(Blocks.DIRT) || state.is(Blocks.FARMLAND) || state.is(Blocks.MYCELIUM);
    }

    private static void witherwood(WorldGenLevel world, Random random, BlockPos start) {
        var state = SourceWitherwoodContent1120.WITHERWOOD.get().defaultBlockState();
        while (start.getY() > 0 && (world.isEmptyBlock(start) || world.getBlockState(start).is(Blocks.OAK_LEAVES))) {
            start = start.below();
        }
        for (int i = 0; i < 4; i++) {
            var pos = start.offset(spread(random, 8), spread(random, 4), spread(random, 8));
            if (world.isEmptyBlock(pos) && state.canSurvive(world, pos)) {
                world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private ClassicNetherDecoration() {}
}
