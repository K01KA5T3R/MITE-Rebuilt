package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

final class CrabulonArena {
    static final float WIDTH = 8.0f;
    static final float HEIGHT = 7.4f;
    private static final int[] SPAWN_DISTANCES = {8, 7, 9, 6, 10};
    private static final int[] FLOOR_OFFSETS = {0, 1, -1, 2, -2};

    private CrabulonArena() {}

    static Vec3 summonSite(ServerLevel level, ServerPlayer player) {
        if (level.dimension() != WorldEvents.UNDERWORLD || !player.isAlive() || player.isSpectator()) return null;
        Direction front = player.getDirection();
        for (Direction direction : new Direction[]{front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
            for (int distance : SPAWN_DISTANCES) {
                BlockPos column = player.blockPosition().relative(direction, distance);
                for (int offset : FLOOR_OFFSETS) {
                    BlockPos feet = column.above(offset);
                    Vec3 at = Vec3.atBottomCenterOf(feet);
                    if (standable(level, at) && level.mayInteract(player, feet.below())
                            && BossAttackGeometry.visible(level, player, player.getEyePosition(), at.add(0, .9, 0))) {
                        return at;
                    }
                }
            }
        }
        return null;
    }

    static boolean standable(ServerLevel level, Vec3 at) {
        var volume = volume(at);
        if (!clear(level, volume)) return false;
        BlockPos floor = BlockPos.containing(at.add(0, -.03, 0));
        return level.getFluidState(floor).isEmpty()
            && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    static Vec3 nearbyFloor(ServerLevel level, SourceCrabulon boss, Vec3 at) {
        Vec3 floor = BossAttackGeometry.ground(level, boss, at.add(0, .80, 0), 2.65);
        return floor != null && standable(level, floor) ? floor : null;
    }

    static boolean clearLeap(ServerLevel level, SourceCrabulon boss, Vec3 end, double apex) {
        Vec3 start = boss.position();
        double length = start.distanceTo(end);
        if (length < 2 || length > 11) return false;
        int samples = Math.max(4, (int)Math.ceil(length));
        for (int i = 1; i < samples; ++i) {
            double progress = i / (double)samples;
            Vec3 arc = start.lerp(end, progress).add(0, Math.sin(Math.PI * progress) * apex, 0);
            if (!clear(level, volume(arc))) return false;
        }
        return true;
    }

    static boolean wide(ServerLevel level, SourceCrabulon boss, Vec3 forward) {
        Vec3 across = new Vec3(-forward.z, 0, forward.x);
        for (Vec3 direction : new Vec3[]{across, across.scale(-1), forward}) {
            Vec3 candidate = nearbyFloor(level, boss, boss.position().add(direction.scale(4.2)));
            if (candidate == null || !clearLeap(level, boss, candidate, 2.05)) return false;
        }
        return true;
    }

    static int breakObstruction(ServerLevel level, SourceCrabulon boss, Vec3 forward) {
        if (!ForgeEventFactory.getMobGriefingEvent(level, boss)) return 0;
        Vec3 across = new Vec3(-forward.z, 0, forward.x);
        Vec3 front = boss.position().add(forward.scale(WIDTH / 2 + .48));
        var cells = new java.util.LinkedHashSet<BlockPos>();
        for (int column = -1; column <= 1; ++column) {
            for (int height = 0; height <= 1; ++height) {
                cells.add(BlockPos.containing(front.add(across.scale(column * WIDTH * .31)).add(0, height + .18, 0)));
            }
        }
        int destroyed = 0;
        for (BlockPos cell : cells) {
            if (level.hasChunkAt(cell) && breakable(level, boss, cell) && level.destroyBlock(cell, false, boss)) {
                ++destroyed;
            }
        }
        return destroyed;
    }

    private static boolean breakable(ServerLevel level, SourceCrabulon boss, BlockPos cell) {
        var state = level.getBlockState(cell);
        float strength = state.getDestroySpeed(level, cell);
        return !state.isAir() && state.getFluidState().isEmpty() && strength >= 0 && strength <= 5
            && !state.hasBlockEntity() && !state.is(BlockTags.WITHER_IMMUNE)
            && !state.getCollisionShape(level, cell).isEmpty() && state.canEntityDestroy(level, cell, boss)
            && ForgeEventFactory.onEntityDestroyBlock(boss, cell, state);
    }

    private static AABB volume(Vec3 at) {
        double half = WIDTH * .5 + .08;
        return new AABB(at.x - half, at.y + .03, at.z - half, at.x + half, at.y + HEIGHT + .08, at.z + half);
    }

    private static boolean clear(ServerLevel level, AABB volume) {
        var low = BlockPos.containing(volume.minX, volume.minY, volume.minZ);
        var high = BlockPos.containing(volume.maxX, volume.maxY, volume.maxZ);
        return !level.isOutsideBuildHeight(low) && !level.isOutsideBuildHeight(high)
            && level.getWorldBorder().isWithinBounds(low) && level.getWorldBorder().isWithinBounds(high)
            && level.hasChunkAt(low) && level.hasChunkAt(high)
            && level.hasChunkAt(new BlockPos(low.getX(), low.getY(), high.getZ()))
            && level.hasChunkAt(new BlockPos(high.getX(), low.getY(), low.getZ()))
            && level.noCollision(volume) && !level.containsAnyLiquid(volume);
    }
}
