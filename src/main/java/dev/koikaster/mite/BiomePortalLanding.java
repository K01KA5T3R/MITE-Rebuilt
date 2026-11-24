package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Creates a landing only after natural sites have been exhausted. */
final class BiomePortalLanding {
    static Vec3 natural(ServerLevel level, ServerPlayer player, BiomePortalStone kind, BlockPos column) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        var at = Vec3.atBottomCenterOf(column.atY(surface));
        return SourceBiomePortal.safeLanding(level, player, kind, at) ? at : null;
    }

    static Vec3 prepare(ServerPlayer player, BiomePortalStone kind, BlockPos column) {
        var level = player.serverLevel();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        if (surface <= level.getMinBuildHeight() || surface >= level.getMaxBuildHeight() - 1) return null;
        var feet = column.atY(surface);
        if (kind != null && !kind.matches(level.getBiome(feet))) return null;
        var at = Vec3.atBottomCenterOf(feet);
        var below = feet.below();
        var floor = level.getBlockState(below);
        boolean needsFloor = !floor.isFaceSturdy(level, below, Direction.UP) || SourceBiomePortal.unsafe(floor);
        if (!BiomePortalBorderLanding.prepare(level, player, at)) return null;
        if (needsFloor) extendFloor(level, below);
        return SourceBiomePortal.safeLanding(player, kind, at) ? at : null;
    }

    private static void extendFloor(ServerLevel level, BlockPos center) {
        for (var at : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            if (at.equals(center) || !level.getWorldBorder().isWithinBounds(at) || !level.hasChunkAt(at)
                    || !SourceBiomePortal.fitsBorder(level, new net.minecraft.world.phys.AABB(at))
                    || !openFloor(level.getBlockState(at)) || !level.getBlockState(at.above()).isAir()
                    || !level.getBlockState(at.above(2)).isAir() || BossArenaPortals.blocked(level, at)
                    || BossArenaPortals.blocked(level, at.above())) continue;
            level.setBlock(at, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean openFloor(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }

    static boolean canClear(BlockState state) {
        if (state.hasBlockEntity() || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY)) return false;
        return state.isAir() || state.canBeReplaced() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)
            || state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT)
                && !state.getValue(LeavesBlock.PERSISTENT);
    }

    private BiomePortalLanding() {}
}
