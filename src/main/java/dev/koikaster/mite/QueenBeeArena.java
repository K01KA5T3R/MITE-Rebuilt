package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class QueenBeeArena {
    static final float WIDTH = 3.35f;
    static final float HEIGHT = 2.68f;
    private static final double SPIRAL_RADIUS = 3.6;

    record Entrance(Vec3 center, double crownHeight, double angle) {}

    private QueenBeeArena() {}

    static Entrance entrance(ServerLevel level, ServerPlayer player) {
        if (!player.onGround() || !level.getBiome(player.blockPosition()).is(BiomeTags.IS_JUNGLE)) return null;
        Vec3 heading = Vec3.directionFromRotation(0, player.getYRot());
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        for (Vec3 offset : List.of(Vec3.ZERO, heading.scale(3.5), side.scale(3), side.scale(-3), heading.scale(-3))) {
            Vec3 point = player.position().add(offset);
            Vec3 floor = floor(level, player, point.add(0, 2.2, 0), 4.6);
            if (floor == null || !level.getBiome(BlockPos.containing(floor)).is(BiomeTags.IS_JUNGLE)
                    || !level.canSeeSky(BlockPos.containing(floor.add(0, .2, 0)))
                    || !clear(level, null, floor.add(0, .35, 0))) continue;
            List<Vec3> openGround = new ArrayList<>(8);
            for (int i = 0; i < 8; ++i) {
                Vec3 sample = floor.add(BossAttackGeometry.flatDirection(i * Math.PI / 4).scale(4.2));
                Vec3 support = floor(level, player, sample.add(0, 2.2, 0), 4.6);
                if (support != null && level.canSeeSky(BlockPos.containing(support.add(0, .25, 0)))
                        && clear(level, null, support.add(0, .4, 0))) openGround.add(support);
            }
            if (openGround.size() < 6) continue;
            double crown = nearbyCrown(level, floor);
            if (crown + HEIGHT + .25 >= level.getMaxBuildHeight()) continue;
            double angle = Math.atan2(heading.z, heading.x) + Math.PI;
            Entrance entrance = new Entrance(floor, crown, angle);
            Vec3 last = introPoint(entrance, 0);
            boolean clearSpiral = clear(level, null, last);
            for (int sample = 1; clearSpiral && sample <= 20; ++sample) {
                Vec3 next = introPoint(entrance, sample / 20.0);
                clearSpiral = clearPath(level, null, last, next);
                last = next;
            }
            if (clearSpiral) return entrance;
        }
        return null;
    }

    static Vec3 introPoint(Entrance entrance, double progress) {
        double radius = SPIRAL_RADIUS * (1 - progress);
        double angle = entrance.angle() + 2 * Math.PI * progress;
        double eased = progress * progress * (3 - 2 * progress);
        double y = entrance.crownHeight() * (1 - eased) + (entrance.center().y + 1.3) * eased;
        Vec3 center = entrance.center();
        return new Vec3(center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius);
    }

    private static double nearbyCrown(ServerLevel level, Vec3 floor) {
        double result = floor.y + 10;
        for (int radius = 6; radius <= 12; radius += 3) {
            for (int i = 0; i < 12; ++i) {
                BlockPos sample = BlockPos.containing(floor.add(BossAttackGeometry.flatDirection(i * Math.PI / 6).scale(radius)));
                if (!level.hasChunkAt(sample)) continue;
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample.getX(), sample.getZ());
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sample.getX(), sample.getZ());
                for (int y = top - 1; y >= Math.max(surface - 1, top - 4); --y) {
                    var state = level.getBlockState(sample.atY(y));
                    if (state.is(Blocks.JUNGLE_LEAVES) || state.is(BlockTags.JUNGLE_LOGS)) {
                        result = Math.max(result, y + 3.5);
                        break;
                    }
                }
            }
        }
        return result;
    }

    static Vec3 floor(ServerLevel level, Entity context, Vec3 start, double depth) {
        if (!level.hasChunkAt(BlockPos.containing(start))) return null;
        Vec3 floor = BossAttackGeometry.ground(level, context, start, depth);
        if (floor == null) return null;
        var state = level.getBlockState(BlockPos.containing(floor.add(0, -.06, 0)));
        return state.is(BlockTags.LEAVES) || !state.getFluidState().isEmpty() ? null : floor;
    }

    static boolean clearPath(ServerLevel level, Entity bee, Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int)Math.ceil(from.distanceTo(to) / .85));
        for (int i = 1; i <= samples; ++i) if (!clear(level, bee, from.lerp(to, (double)i / samples))) return false;
        return true;
    }

    private static boolean clear(ServerLevel level, Entity bee, Vec3 point) {
        double half = WIDTH / 2.0;
        var body = new AABB(point.x - half, point.y + .04, point.z - half,
            point.x + half, point.y + HEIGHT, point.z + half);
        if (body.minY < level.getMinBuildHeight() || body.maxY >= level.getMaxBuildHeight()
                || !level.hasChunksAt(BlockPos.containing(body.minX, body.minY, body.minZ),
                                      BlockPos.containing(body.maxX, body.maxY, body.maxZ))) return false;
        return level.getWorldBorder().isWithinBounds(body) && !level.containsAnyLiquid(body)
            && !level.getBlockCollisions(bee, body).iterator().hasNext();
    }
}
