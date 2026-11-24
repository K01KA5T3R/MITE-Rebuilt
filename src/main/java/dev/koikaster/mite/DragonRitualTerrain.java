package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.RegistryObject;

/** Tracks temporary ridges by encounter. Each placement also owns a native scheduled expiry. */
final class DragonRitualTerrain extends SavedData {
    static final int LIFETIME = 240;
    private static final String DATA_KEY = "mite_dragon_ritual_terrain";
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    private static final int[][] SEAMS = {{4, 2}, {4, 3}, {-4, -2}, {-4, -3}, {2, -4}, {3, -4}, {-2, 4}, {-3, 4}};
    private static final int[] HEIGHTS = {0, -1, 1, -2, 2};
    static final RegistryObject<Block> RIDGE = Content.BLOCKS.register("dragon_ritual_ridge", Ridge::new);

    private final Map<BlockPos, Entry> entries = new HashMap<>();

    static void register() {}

    static List<BlockPos> plan(ServerLevel level, SourceEnderDragon dragon, BlockPos altar) {
        if (!ForgeEventFactory.getMobGriefingEvent(level, dragon)) return List.of();
        var result = new ArrayList<BlockPos>(SEAMS.length);
        for (int[] seam : SEAMS) {
            for (int height : HEIGHTS) {
                BlockPos at = altar.offset(seam[0], height - 2, seam[1]);
                if (canPlace(level, at)) {
                    result.add(at);
                    break;
                }
            }
        }
        return result;
    }

    static List<BlockPos> available(ServerLevel level, SourceEnderDragon dragon, List<BlockPos> planned) {
        if (planned.isEmpty() || !ForgeEventFactory.getMobGriefingEvent(level, dragon)) return List.of();
        return planned.stream().filter(at -> canPlace(level, at)).toList();
    }

    static AABB bounds(BlockPos at) {
        return SHAPE.bounds().move(at);
    }

    private static boolean canPlace(ServerLevel level, BlockPos at) {
        return level.hasChunkAt(at) && level.getWorldBorder().isWithinBounds(at)
            && level.getBlockState(at).is(Blocks.AIR) && level.isEmptyBlock(at.above())
            && level.getBlockState(at.below()).is(Blocks.END_STONE)
            && level.getEntities((Entity) null, bounds(at)).isEmpty();
    }

    static void place(ServerLevel level, UUID encounter, List<BlockPos> positions) {
        if (positions.isEmpty()) return;
        var terrain = state(level);
        long until = level.getGameTime() + LIFETIME;
        for (BlockPos at : positions) {
            if (!canPlace(level, at) || !level.setBlockAndUpdate(at, RIDGE.get().defaultBlockState())) continue;
            terrain.entries.put(at, new Entry(encounter, until));
            terrain.setDirty();
        }
    }

    static void tickArena(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) state(level).restoreDue(level);
    }

    static void endEncounter(ServerLevel level, UUID encounter) {
        if (encounter == null) return;
        var terrain = state(level);
        for (var row : terrain.entries.entrySet()) {
            if (!row.getValue().owner.equals(encounter)) continue;
            row.setValue(new Entry(encounter, 0));
            terrain.setDirty();
        }
        terrain.restoreDue(level);
    }

    private void restoreDue(ServerLevel level) {
        long now = level.getGameTime();
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var row = iterator.next();
            BlockPos at = row.getKey();
            if (row.getValue().until > now || !level.hasChunkAt(at) || !restore(level, at)) continue;
            iterator.remove();
            setDirty();
        }
    }

    private static boolean restore(ServerLevel level, BlockPos at) {
        return !level.getBlockState(at).is(RIDGE.get()) || level.removeBlock(at, false);
    }

    private static void scheduled(ServerLevel level, BlockPos at) {
        if (!DragonRitual.managed(level)) {
            restore(level, at);
            return;
        }
        var terrain = state(level);
        var entry = terrain.entries.get(at);
        long remaining = entry == null ? 0 : entry.until - level.getGameTime();
        if (remaining > 0) {
            level.scheduleTick(at, RIDGE.get(), (int) Math.min(remaining, LIFETIME));
        } else if (!restore(level, at)) {
            level.scheduleTick(at, RIDGE.get(), 20);
        } else if (terrain.entries.remove(at) != null) {
            terrain.setDirty();
        }
    }

    private static DragonRitualTerrain state(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DragonRitualTerrain::load, DragonRitualTerrain::new, DATA_KEY);
    }

    private static DragonRitualTerrain load(CompoundTag tag) {
        var terrain = new DragonRitualTerrain();
        for (var raw : tag.getList("Ridges", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            if (row.contains("Position", Tag.TAG_LONG) && row.hasUUID("Encounter")) {
                terrain.entries.put(BlockPos.of(row.getLong("Position")), new Entry(row.getUUID("Encounter"), row.getLong("Until")));
            }
        }
        return terrain;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        for (var row : entries.entrySet()) {
            var data = new CompoundTag();
            data.putLong("Position", row.getKey().asLong());
            data.putUUID("Encounter", row.getValue().owner);
            data.putLong("Until", row.getValue().until);
            list.add(data);
        }
        tag.put("Ridges", list);
        return tag;
    }

    private record Entry(UUID owner, long until) {}

    private static final class Ridge extends Block {
        Ridge() {
            super(Properties.of().mapColor(MapColor.SAND).strength(1.5f, 3).noOcclusion().noLootTable()
                .pushReaction(PushReaction.BLOCK));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
            super.onPlace(state, level, pos, previous, moving);
            if (level instanceof ServerLevel server && !previous.is(this)) server.scheduleTick(pos, this, LIFETIME);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            scheduled(level, pos);
        }
    }
}
