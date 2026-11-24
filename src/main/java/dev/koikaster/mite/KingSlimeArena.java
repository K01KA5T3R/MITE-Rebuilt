package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Locates an open Overworld surface site for a Slime Crown used in the air. */
final class KingSlimeArena {
    private static final int RADIUS = 4;
    private static final int HEADROOM = 8;
    private static final int[] FLOOR_SEARCH = {0, 1, -1, 2, -2};

    private KingSlimeArena() {}

    static Vec3 summonSite(ServerLevel level, ServerPlayer player) {
        if (level.dimension() != Level.OVERWORLD || player.isSpectator() || !player.isAlive()) return null;
        Vec3 forward = Vec3.atLowerCornerOf(player.getDirection().getNormal());
        for (int distance : new int[]{6, 5, 7}) {
            BlockPos anchor = BlockPos.containing(player.position().add(forward.scale(distance)));
            for (int offset : FLOOR_SEARCH) {
                BlockPos feet = anchor.above(offset);
                if (clearSurface(level, player, feet)) return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    private static boolean clearSurface(ServerLevel level, ServerPlayer player, BlockPos feet) {
        if (level.isOutsideBuildHeight(feet.below()) || level.isOutsideBuildHeight(feet.above(HEADROOM - 1))
                || !level.getWorldBorder().isWithinBounds(feet.offset(-RADIUS, 0, -RADIUS))
                || !level.getWorldBorder().isWithinBounds(feet.offset(RADIUS, 0, RADIUS))
                || !level.hasChunkAt(feet.offset(-RADIUS, 0, -RADIUS))
                || !level.hasChunkAt(feet.offset(RADIUS, 0, -RADIUS))
                || !level.hasChunkAt(feet.offset(-RADIUS, 0, RADIUS))
                || !level.hasChunkAt(feet.offset(RADIUS, 0, RADIUS))
                || !level.canSeeSky(feet) || !level.mayInteract(player, feet.below())) return false;

        var center = Vec3.atBottomCenterOf(feet);
        var space = new AABB(center.x - RADIUS, center.y, center.z - RADIUS,
            center.x + RADIUS, center.y + HEADROOM, center.z + RADIUS);
        if (!level.noCollision(space) || level.containsAnyLiquid(space) || !support(level, feet.below())) return false;

        for (var side : Direction.Plane.HORIZONTAL) {
            var edge = feet.relative(side, RADIUS - 1).below();
            if (!support(level, edge) && !support(level, edge.below())) return false;
        }
        return true;
    }

    private static boolean support(ServerLevel level, BlockPos floor) {
        return level.getFluidState(floor).isEmpty()
            && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
}
