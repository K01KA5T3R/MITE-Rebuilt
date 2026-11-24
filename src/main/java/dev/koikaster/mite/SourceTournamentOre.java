package dev.koikaster.mite;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** The six scored metals retain apr's natural (0) and placed (1) distinction. */
public final class SourceTournamentOre extends DropExperienceBlock {
    static final BooleanProperty PLACED = BooleanProperty.create("placed");

    public SourceTournamentOre(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PLACED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PLACED, true);
    }
}
