package dev.koikaster.mite;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

/** World-specific application of the Rebuilt slime and honey choices. */
public final class ModernPistonPolicy305 {
    private static boolean useRebuiltPhysics(LevelReader reader) {
        Level level = reader instanceof Level direct ? direct
            : reader instanceof ServerLevelAccessor generation ? generation.getLevel() : null;
        return level != null && WorldContentMode.rebuilt(level);
    }

    public static boolean restricted(BlockState state, Level level) {
        return (state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK)) && useRebuiltPhysics(level);
    }

    public static boolean isSticky(BlockState state, Level level) {
        return !restricted(state, level) && state.isStickyBlock();
    }

    public static boolean canStick(BlockState state, BlockState other, Level level) {
        return !restricted(state, level) && !restricted(other, level) && state.canStickTo(other);
    }

    public static boolean pistonBounce(BlockState state, Level level) {
        return !restricted(state, level) && state.isSlimeBlock();
    }

    public static float slimeFriction(SlimeBlock block, LevelReader level) {
        return block == Blocks.SLIME_BLOCK && useRebuiltPhysics(level) ? 0.6F : block.getFriction();
    }

    private ModernPistonPolicy305() {}
}
