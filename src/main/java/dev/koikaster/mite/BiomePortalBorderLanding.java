package dev.koikaster.mite;

import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** An emergency biome-portal arrival whose standing footprint crosses block centers. */
final class BiomePortalBorderLanding {
    private record Change(BlockState before, BlockState after) {}

    static boolean fractional(Vec3 at) {
        return at.x != Mth.floor(at.x) + .5 || at.z != Mth.floor(at.z) + .5;
    }

    static Vec3 fit(WorldBorder border, double width, Vec3 column) {
        double x = fitAxis(border.getMinX(), border.getMaxX(), width, column.x);
        double z = fitAxis(border.getMinZ(), border.getMaxZ(), width, column.z);
        return Double.isFinite(x) && Double.isFinite(z) ? new Vec3(x, column.y, z) : null;
    }

    private static double fitAxis(double min, double max, double width, double coordinate) {
        double low = Math.nextUp(min + width / 2);
        double high = Math.nextDown(max - width / 2);
        if (low > high) return Double.NaN;
        if (coordinate >= low && coordinate <= high) return coordinate;
        double firstCenter = Math.ceil(low - .5) + .5;
        double lastCenter = Math.floor(high - .5) + .5;
        if (firstCenter <= lastCenter) return Mth.clamp(coordinate, firstCenter, lastCenter);
        double middle = low + (high - low) / 2;
        // A shifted border can fit a player's full width inside one block without fitting that block's center.
        double nearest = Double.NaN;
        double distance = Double.POSITIVE_INFINITY;
        for (int block = Mth.floor(low); block <= Mth.floor(high); block++) {
            double innerLow = Math.max(low, Math.nextUp(block + width / 2));
            double innerHigh = Math.min(high, Math.nextDown(block + 1 - width / 2));
            if (innerLow > innerHigh) continue;
            double insideCell = Mth.clamp(coordinate, innerLow, innerHigh);
            if (Math.abs(insideCell - coordinate) < distance) {
                nearest = insideCell;
                distance = Math.abs(insideCell - coordinate);
            }
        }
        return Double.isNaN(nearest) ? middle : nearest;
    }

    static boolean loaded(ServerLevel level, AABB body) {
        var source = level.getChunkSource();
        for (int cx = (Mth.floor(body.minX) - 1) >> 4; cx <= ((lastCell(body.maxX) + 1) >> 4); cx++) {
            for (int cz = (Mth.floor(body.minZ) - 1) >> 4; cz <= ((lastCell(body.maxZ) + 1) >> 4); cz++) {
                if (source.getChunkNow(cx, cz) == null) return false;
            }
        }
        return true;
    }

    static AABB standing(ServerPlayer player, Vec3 at) {
        return player.getDimensions(Pose.STANDING).makeBoundingBox(at);
    }

    static boolean safe(ServerLevel level, ServerPlayer player, Vec3 at) {
        var body = standing(player, at);
        if (!inside(level, body) || !loaded(level, body)) return false;
        int y = Mth.floor(at.y);
        if (at.y != y || !onSurface(level, body, y)) return false;
        for (var feet : footprint(body, y)) {
            var below = feet.below();
            if (BossArenaPortals.blocked(level, below) || !solidFloor(level, below, level.getBlockState(below))) return false;
            for (int by = y; by <= lastCell(body.maxY); by++) {
                var space = feet.atY(by);
                if (BossArenaPortals.blocked(level, space) || SourceBiomePortal.unsafe(level.getBlockState(space))
                        || !level.getFluidState(space.above()).isEmpty()) return false;
                for (var side : Direction.Plane.HORIZONTAL) {
                    var adjacent = level.getBlockState(space.relative(side));
                    if (adjacent.is(Blocks.CACTUS) || !adjacent.getFluidState().isEmpty()) return false;
                }
            }
        }
        return clearBody(level, player, body);
    }

    static boolean onSurface(ServerLevel level, AABB body, int y) {
        for (var feet : footprint(body, y)) {
            if (level.getChunkSource().getChunkNow(feet.getX() >> 4, feet.getZ() >> 4) == null
                    || y < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX(), feet.getZ())) return false;
        }
        return true;
    }

    static boolean clearBody(ServerLevel level, ServerPlayer player, AABB body) {
        return level.noCollision(player, body) && !level.getBlockCollisions(null, body).iterator().hasNext()
            && !level.containsAnyLiquid(body);
    }

    static Vec3 find(ServerPlayer player, Vec3 column) {
        var level = player.serverLevel();
        var body = standing(player, column);
        if (!loaded(level, body)) return null;
        int surface = level.getMinBuildHeight();
        for (var cell : footprint(body, 0)) {
            surface = Math.max(surface, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cell.getX(), cell.getZ()));
        }
        var at = new Vec3(column.x, surface, column.z);
        return prepare(level, player, at) ? at : null;
    }

    static boolean prepare(ServerLevel level, ServerPlayer player, Vec3 at) {
        var body = standing(player, at);
        if (!inside(level, body) || !loaded(level, body) || !level.getEntityCollisions(player, body).isEmpty()) return false;
        int y = Mth.floor(at.y);
        if (at.y != y || !onSurface(level, body, y)) return false;
        var changes = new LinkedHashMap<BlockPos, Change>();
        for (var feet : footprint(body, y)) {
            var floor = feet.below();
            if (BossArenaPortals.blocked(level, floor)) return false;
            var below = level.getBlockState(floor);
            if (!solidFloor(level, floor, below)) {
                if (!BiomePortalLanding.canClear(below)) return false;
                changes.put(floor, new Change(below, Blocks.COBBLESTONE.defaultBlockState()));
            }
            for (int by = y; by <= lastCell(body.maxY); by++) {
                var pos = feet.atY(by);
                if (BossArenaPortals.blocked(level, pos)) return false;
                var state = level.getBlockState(pos);
                if (!SourceBiomePortal.unsafe(state) && !intersects(level, pos, state, body)) continue;
                if (!BiomePortalLanding.canClear(state)) return false;
                changes.put(pos, new Change(state, Blocks.AIR.defaultBlockState()));
            }
        }
        if (changes.isEmpty()) return safe(level, player, at);
        for (var feet : footprint(body, y)) {
            for (int by = y; by <= lastCell(body.maxY); by++) {
                var pos = feet.atY(by);
                var above = pos.above();
                if (hasUnchangedFluid(level, above, changes)) return false;
                for (var side : Direction.Plane.HORIZONTAL) {
                    if (hasUnchangedFluid(level, pos.relative(side), changes)) return false;
                }
            }
        }

        var applied = new LinkedHashMap<BlockPos, Change>();
        boolean success = false;
        try {
            for (var entry : changes.entrySet()) {
                var pos = entry.getKey();
                var edit = entry.getValue();
                if (!level.getWorldBorder().isWithinBounds(pos) || level.getBlockState(pos) != edit.before()
                        || !level.setBlock(pos, edit.after(), Block.UPDATE_ALL)) return false;
                applied.put(pos, edit);
            }
            success = safe(level, player, at);
            return success;
        } finally {
            if (!success) {
                applied.forEach((pos, edit) -> {
                    if (level.getBlockState(pos) == edit.after()) level.setBlock(pos, edit.before(), Block.UPDATE_ALL);
                });
            }
        }
    }

    static boolean solidFloor(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.isFaceSturdy(level, pos, Direction.UP) || SourceBiomePortal.unsafe(state)) return false;
        while (true) {
            boolean sourceGravity = SoilGravity.falls(state);
            if (!sourceGravity && !(state.getBlock() instanceof Fallable)) return true;
            var below = pos.below();
            var support = level.getBlockState(below);
            if (sourceGravity ? SourceSoilSupport246.canFallBelow(support, state, level, below) : FallingBlock.isFree(support)) {
                return false;
            }
            pos = below;
            state = support;
        }
    }

    private static boolean hasUnchangedFluid(ServerLevel level, BlockPos pos, LinkedHashMap<BlockPos, Change> changes) {
        return !changes.containsKey(pos) && !level.getFluidState(pos).isEmpty();
    }

    private static boolean intersects(ServerLevel level, BlockPos pos, BlockState state, AABB body) {
        for (var shape : state.getCollisionShape(level, pos).toAabbs()) {
            if (body.intersects(shape.move(pos))) return true;
        }
        return false;
    }

    private static boolean inside(ServerLevel level, AABB body) {
        return body.minY >= level.getMinBuildHeight() + 1 && body.maxY < level.getMaxBuildHeight()
            && SourceBiomePortal.fitsBorder(level, body);
    }

    private static Iterable<BlockPos> footprint(AABB body, int y) {
        return BlockPos.betweenClosed(Mth.floor(body.minX), y, Mth.floor(body.minZ),
            lastCell(body.maxX), y, lastCell(body.maxZ));
    }

    private static int lastCell(double edge) {
        return Mth.ceil(edge) - 1;
    }

    private BiomePortalBorderLanding() {}
}
