package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RedstoneGolemSummon {
    private static final ThreadLocal<Set<PendingSummon>> PENDING = ThreadLocal.withInitial(HashSet::new);
    private static final List<Direction> AXES = List.of(Direction.EAST, Direction.SOUTH);

    private RedstoneGolemSummon() {}

    public static boolean trySpawn(Object rawLevel, Object rawHead) {
        if (!(rawLevel instanceof ServerLevel level) || !(rawHead instanceof BlockPos head)
                || !WorldContentMode.rebuiltBosses(level) || level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.hasChunkAt(head) || !level.canSeeSky(head.above())) return false;
        BlockState pumpkin = level.getBlockState(head);
        if (!pumpkin.is(Blocks.CARVED_PUMPKIN) && !pumpkin.is(Blocks.JACK_O_LANTERN)) return false;
        BlockPos core = head.below();
        BlockPos bottom = head.below(2);
        if (!level.getBlockState(core).is(Blocks.REDSTONE_BLOCK)
                || !level.getBlockState(bottom).is(Blocks.IRON_BLOCK) || !supported(level, bottom)) return false;
        for (Direction axis : AXES) {
            BlockPos one = core.relative(axis);
            BlockPos two = core.relative(axis.getOpposite());
            if (!level.getBlockState(one).is(Blocks.IRON_BLOCK) || !level.getBlockState(two).is(Blocks.IRON_BLOCK)
                    || !level.isEmptyBlock(one.below()) || !level.isEmptyBlock(two.below())
                    || !level.isEmptyBlock(one.above()) || !level.isEmptyBlock(two.above())) continue;
            var structure = List.of(head, core, bottom, one, two).stream()
                .map(pos -> new Cell(pos.immutable(), level.getBlockState(pos))).toList();
            if (spawn(level, bottom, axis, structure)) return true;
        }
        return false;
    }

    private static boolean spawn(ServerLevel level, BlockPos bottom, Direction axis, List<Cell> structure) {
        if (!level.getEntitiesOfClass(SourceRedstoneGolem.class,
                new AABB(bottom).inflate(48, 18, 48), SourceRedstoneGolem::isAlive).isEmpty()
                || !hasRoom(level, bottom, structure)) return false;
        var pending = PENDING.get();
        var key = new PendingSummon(level, bottom);
        if (!pending.add(key)) return false;
        try {
            var boss = RedstoneGolemContent.GOLEM.get().create(level);
            if (boss == null) return false;
            var position = Vec3.atBottomCenterOf(bottom);
            boss.moveTo(position.x, position.y, position.z, axis.getClockWise().toYRot(), 0);
            boss.setPersistenceRequired();
            if (!level.getEntities(boss, boss.getBoundingBox()).isEmpty() || !level.addFreshEntity(boss)) return false;
            boolean committed = false;
            try {
                if (boss.isRemoved() || level.getEntity(boss.getUUID()) != boss
                        || boss.position().distanceToSqr(position) > 1.0e-6
                        || !level.getEntities(boss, boss.getBoundingBox()).isEmpty()
                        || !supported(level, bottom) || !level.canSeeSky(bottom.above(3))
                        || !hasRoom(level, bottom, structure)
                        || structure.stream().anyMatch(cell -> !cell.unchanged(level))) return false;
                committed = clearStructure(level, structure);
                if (!committed) return false;
                for (var cell : structure) {
                    level.levelEvent(2001, cell.pos, Block.getId(cell.state));
                    level.blockUpdated(cell.pos, Blocks.AIR);
                }
                level.gameEvent(boss, GameEvent.ENTITY_PLACE, position);
                return true;
            } finally {
                if (!committed) boss.discard();
            }
        } finally {
            pending.remove(key);
            if (pending.isEmpty()) PENDING.remove();
        }
    }

    private static boolean clearStructure(ServerLevel level, List<Cell> structure) {
        var air = Blocks.AIR.defaultBlockState();
        var removed = new ArrayList<Cell>(structure.size());
        boolean cleared = false;
        try {
            for (var cell : structure) {
                if (!cell.unchanged(level) || !level.setBlock(cell.pos, air, Block.UPDATE_CLIENTS)) return false;
                removed.add(cell);
            }
            cleared = structure.stream().allMatch(cell -> level.getBlockState(cell.pos) == air);
            return cleared;
        } finally {
            if (!cleared) {
                for (int i = removed.size() - 1; i >= 0; i--) {
                    var cell = removed.get(i);
                    if (level.getBlockState(cell.pos) == air) level.setBlock(cell.pos, cell.state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static boolean supported(ServerLevel level, BlockPos bottom) {
        var below = bottom.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static boolean hasRoom(ServerLevel level, BlockPos bottom, List<Cell> structure) {
        var positions = new HashSet<BlockPos>(structure.size());
        for (var cell : structure) positions.add(cell.pos);
        for (var cell : BlockPos.betweenClosed(bottom.offset(-2, 0, -2), bottom.offset(2, 4, 2))) {
            if (!level.hasChunkAt(cell) || !level.getWorldBorder().isWithinBounds(cell)
                    || level.isOutsideBuildHeight(cell)) return false;
            if (positions.contains(cell)) continue;
            var state = level.getBlockState(cell);
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, cell).isEmpty()) return false;
        }
        return true;
    }

    private record Cell(BlockPos pos, BlockState state) {
        boolean unchanged(ServerLevel level) {
            return level.getBlockState(pos) == state;
        }
    }

    private record PendingSummon(ServerLevel level, BlockPos bottom) {}
}
