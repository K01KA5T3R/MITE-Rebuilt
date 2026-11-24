package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import net.minecraft.core.Vec3i;

public final class UnderworldMineshaftStructure extends MineshaftStructure {
    static final Codec<UnderworldMineshaftStructure> CODEC = simpleCodec(UnderworldMineshaftStructure::new);

    public UnderworldMineshaftStructure(StructureSettings settings) {
        super(settings, Type.NORMAL);
    }

    @Override
    public StructureType<?> type() {
        return SourceUnderworldContent.MINESHAFT_TYPE.get();
    }

    @Override
    public void afterPlace(WorldGenLevel world, StructureManager structures, ChunkGenerator generator,
                           RandomSource random, BoundingBox clip, ChunkPos chunk, PiecesContainer pieces) {
        if (!UnderworldExplorationRules.enabled(world.getLevel()) || pieces.isEmpty()) return;
        for (var bay : chooseBays(pieces)) {
            if (!clip.isInside(bay.side()) || !canPlace(world, clip, bay)) continue;

            if (!world.setBlock(bay.side(), Blocks.SPAWNER.defaultBlockState(), 2)) continue;
            if (world.getBlockEntity(bay.side()) instanceof SpawnerBlockEntity spawner) {
                spawner.setEntityId(EntityType.CAVE_SPIDER, RandomSource.create(world.getSeed() ^ bay.side().asLong()));
            } else {
                throw new IllegalStateException("Underworld mineshaft lost its native spawner at " + bay.side());
            }
        }
    }

    static List<Bay> chooseBays(PiecesContainer pieces) {
        var candidates = new ArrayList<Bay>();
        int corridorCount = 0;
        for (var piece : pieces.pieces()) {
            if (piece.getType() != StructurePieceType.MINE_SHAFT_CORRIDOR) continue;
            corridorCount++;
            Bay bay = corridorBay(piece);
            if (bay != null) candidates.add(bay);
        }
        int budget = UnderworldExplorationRules.spawnerBudget(corridorCount);
        if (budget == 0 || candidates.isEmpty()) return List.of();

        // The first native component is the start room. All chunks derive the same set of slots.
        var entrance = pieces.pieces().get(0).getBoundingBox().getCenter();
        candidates.sort(Comparator.comparingDouble((Bay bay) -> horizontalDistance(bay.side(), entrance)).reversed()
            .thenComparingLong(bay -> bay.side().asLong()));
        var selected = new ArrayList<Bay>(budget);
        int gap = UnderworldExplorationRules.MIN_SPAWNER_HORIZONTAL_GAP;
        for (var bay : candidates) {
            if (selected.stream().anyMatch(other -> horizontalDistance(other.side(), bay.side()) < gap * gap)) continue;
            selected.add(bay);
            if (selected.size() == budget) break;
        }
        return selected;
    }

    private static Bay corridorBay(StructurePiece piece) {
        Direction direction = piece.getOrientation();
        if (direction == null || !direction.getAxis().isHorizontal()) return null;
        BoundingBox box = piece.getBoundingBox();
        int length = direction.getAxis() == Direction.Axis.X ? box.getXSpan() : box.getZSpan();
        if (length < 5) return null;

        // Native supports stand at local 2 + 5n. Use the adjacent side, off the walking lane.
        int offset = length / 10 * 5 + 1;
        int lateral = ((box.minX() ^ box.minZ()) & 1) == 0 ? 0 : 2;
        BlockPos side = corridorPosition(box, direction, lateral, offset);
        BlockPos center = corridorPosition(box, direction, 1, offset);
        return new Bay(side, center, direction);
    }

    static BlockPos corridorPosition(BoundingBox box, Direction direction, int lateral, int length) {
        return switch (direction) {
            case NORTH -> new BlockPos(box.minX() + lateral, box.minY(), box.maxZ() - length);
            case SOUTH -> new BlockPos(box.minX() + lateral, box.minY(), box.minZ() + length);
            case WEST -> new BlockPos(box.maxX() - length, box.minY(), box.minZ() + lateral);
            case EAST -> new BlockPos(box.minX() + length, box.minY(), box.minZ() + lateral);
            default -> throw new IllegalArgumentException("Not a horizontal mineshaft corridor: " + direction);
        };
    }

    private static double horizontalDistance(Vec3i first, Vec3i second) {
        double x = (double) first.getX() - second.getX();
        double z = (double) first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private static boolean canPlace(WorldGenLevel world, BoundingBox clip, Bay bay) {
        var side = bay.side();
        var center = bay.center();
        var support = side.relative(bay.forward());
        var before = center.relative(bay.forward().getOpposite());
        var after = center.relative(bay.forward());
        if (!clip.isInside(support) || !clip.isInside(before) || !clip.isInside(after)) return false;
        if (!world.isEmptyBlock(side) || !world.isEmptyBlock(side.above())
                || !walkable(world, before) || !walkable(world, center) || !walkable(world, after)) return false;
        if (!world.getBlockState(side.below()).isFaceSturdy(world, side.below(), Direction.UP)) return false;
        if (!world.getBlockState(support).is(BlockTags.WOODEN_FENCES)) return false;
        // A rail-bearing walking lane is legal, but the spawner itself always occupies air at the side.
        return !world.getBlockState(side.relative(bay.forward().getOpposite())).is(Blocks.SPAWNER);
    }

    private static boolean walkable(WorldGenLevel world, BlockPos pos) {
        var at = world.getBlockState(pos);
        return (at.isAir() || at.is(BlockTags.RAILS)) && world.isEmptyBlock(pos.above())
            && world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP);
    }

    record Bay(BlockPos side, BlockPos center, Direction forward) {}
}
