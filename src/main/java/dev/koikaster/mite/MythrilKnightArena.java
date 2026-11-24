package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class MythrilKnightArena {
    private static final double RADIUS = 32;
    private static final double HALF_HEIGHT = 16;
    private static final int[] DISTANCES = {9, 8, 10, 11, 7};
    private static final int[] HEIGHTS = {0, 1, -1};

    private MythrilKnightArena() {}

    static Vec3 summonSite(ServerLevel level, ServerPlayer player) {
        if (level.dimension() != Level.OVERWORLD || !player.isAlive() || player.isSpectator()) return null;
        var front = player.getDirection();
        for (int distance : DISTANCES) {
            var column = player.blockPosition().relative(front, distance);
            for (int height : HEIGHTS) {
                var feet = column.offset(0, height, 0);
                if (suitable(level, player, feet)) return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    private static boolean suitable(ServerLevel level, ServerPlayer player, BlockPos feet) {
        if (!level.hasChunkAt(feet) || !level.canSeeSky(feet) || level.isOutsideBuildHeight(feet.above(5))
                || !level.mayInteract(player, feet.below()) || !hasFooting(level, feet)) return false;
        var site = Vec3.atBottomCenterOf(feet);
        if (!clear(level, site) || !BossAttackGeometry.visible(level, player, player.getEyePosition(), site.add(0, 1.25, 0))) return false;
        int openSides = 0;
        for (var direction : Direction.Plane.HORIZONTAL) {
            if (clear(level, Vec3.atBottomCenterOf(feet.relative(direction, 3)))) ++openSides;
        }
        return openSides >= 2;
    }

    private static boolean hasFooting(ServerLevel level, BlockPos feet) {
        var floor = feet.below();
        return level.getFluidState(floor).isEmpty() && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    private static boolean clear(ServerLevel level, Vec3 site) {
        var mount = EntityType.RAVAGER.getDimensions();
        var width = mount.width * .5 + .1;
        var volume = new AABB(site.x - width, site.y, site.z - width,
            site.x + width, site.y + mount.height + EntityType.PILLAGER.getDimensions().height, site.z + width);
        return level.hasChunksAt(BlockPos.containing(volume.minX, volume.minY, volume.minZ),
                   BlockPos.containing(volume.maxX, volume.maxY, volume.maxZ))
            && level.getWorldBorder().isWithinBounds(volume) && level.noCollision(volume)
            && !level.containsAnyLiquid(volume);
    }

    static boolean includes(Vec3 center, Vec3 at) {
        return BossArenaPortals.withinCylinder(center, at, RADIUS * RADIUS, HALF_HEIGHT);
    }
}
