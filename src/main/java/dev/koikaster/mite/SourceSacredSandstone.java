package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Ordinary survival behavior of the R195 sandstone subtype 24:3. */
final class SourceSacredSandstone extends Block {
    private static final Direction[] PARTICLE_FACES = {
        Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST
    };

    SourceSacredSandstone(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!OriginalRandomTables.random_numbers_have_been_initialized
                || OriginalRandomTables.chance_in_2[++OriginalRandomTables.random_number_index & 32767]) {
            return;
        }
        for (Direction face : PARTICLE_FACES) {
            double x = pos.getX() + level.random.nextFloat();
            double y = pos.getY() + level.random.nextFloat();
            double z = pos.getZ() + level.random.nextFloat();
            BlockPos adjacent = pos.relative(face);
            BlockState neighbor = level.getBlockState(adjacent);
            if (neighbor.canOcclude() && neighbor.isCollisionShapeFullBlock(level, adjacent)) {
                continue;
            }
            // The original lower-face emission checks absolute y < 0.
            if (face == Direction.DOWN && pos.getY() > 0) {
                continue;
            }
            double particleX = face.getStepX() == 0 ? x : pos.getX() + 0.5 + face.getStepX() * 0.5625;
            double particleY = face.getStepY() == 0 ? y : pos.getY() + 0.5 + face.getStepY() * 0.5625;
            double particleZ = face.getStepZ() == 0 ? z : pos.getZ() + 0.5 + face.getStepZ() * 0.5625;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientSacredParticles1120.spawn(level, particleX, particleY, particleZ));
        }
    }

    static final class Item extends BlockItem {
        Item(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        protected BlockState getPlacementState(BlockPlaceContext context) {
            if (!context.getLevel().getBiome(context.getClickedPos()).is(Biomes.DESERT)) {
                return null;
            }
            return super.getPlacementState(context);
        }
    }
}

