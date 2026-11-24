package dev.koikaster.mite;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Synchronizes the furnace front independently of its existing lit mechanics. */
public final class FurnaceEmbers {
    public static final int LAST_STAGE = 3;
    public static final IntegerProperty FADE = IntegerProperty.create("mite_embers", 0, LAST_STAGE);
    private static final int MAX_FADE_TICKS = 120;

    private FurnaceEmbers() {}

    @SuppressWarnings("unchecked")
    public static void addNativeProperty(Object block, Object stateBuilder) {
        var type = block.getClass();
        if (type == FurnaceBlock.class || type == BlastFurnaceBlock.class || type == SmokerBlock.class) {
            ((StateDefinition.Builder<Block, BlockState>) stateBuilder).add(FADE);
        }
    }

    static int stage(int remaining, int total, BooleanSupplier canRefuel) {
        int window = Math.min(MAX_FADE_TICKS, Math.max(LAST_STAGE, total / 4));
        if (remaining <= 0 || remaining > window || canRefuel.getAsBoolean()) return 0;
        return LAST_STAGE - (remaining - 1) * LAST_STAGE / window;
    }

    static void update(Level level, BlockPos pos, BlockState current, int remaining, int total, BooleanSupplier canRefuel) {
        boolean lit = remaining > 0;
        boolean toggled = current.getValue(BlockStateProperties.LIT) != lit;
        var next = current.setValue(BlockStateProperties.LIT, lit);
        if (next.hasProperty(FADE)) next = next.setValue(FADE, stage(remaining, total, canRefuel));
        if (next != current) level.setBlock(pos, next, toggled ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
    }
}

