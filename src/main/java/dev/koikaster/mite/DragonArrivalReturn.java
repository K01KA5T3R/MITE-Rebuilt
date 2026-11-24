package dev.koikaster.mite;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A pre-fight return portal beside the End arrival platform. */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class DragonArrivalReturn {
    private static final String DATA_KEY = "mite_dragon_arrival_return";
    private static final BlockPos ARRIVAL = ServerLevel.END_SPAWN_POINT;
    private static final Map<ServerPlayer, Travel> PENDING = new WeakHashMap<>();

    private DragonArrivalReturn() {}

    @SubscribeEvent
    public static void arrive(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTo() == Level.END
                && WorldContentMode.rebuilt(player.serverLevel())) {
            ensure(player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.serverLevel().dimension() == Level.END
                && WorldContentMode.rebuilt(player.serverLevel())) {
            ensure(player.serverLevel());
        }
    }

    static BlockPos ensure(ServerLevel end) {
        var gate = gate(end);
        if (gate.position != null) {
            if (end.getBlockState(gate.position).is(Blocks.END_PORTAL)) return gate.position;
            if (landingOpen(end, gate.position) && putPortal(end, gate.position)) return gate.position;
        }

        // The vanilla platform occupies offsets -2 through +2. A frame at
        // distance 4 touches its edge and remains outside the rebuild box.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos center = ARRIVAL.relative(side, 4).below();
            if (!open(end, center)) continue;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
                if (end.isEmptyBlock(pos.below())) end.setBlockAndUpdate(pos.below(), Blocks.BEDROCK.defaultBlockState());
                if (!pos.equals(center)) end.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
            }
            if (putPortal(end, center)) return remember(gate, center);
        }

        // An old build can surround the platform. A one-block entrance at the
        // platform's edge stays reachable and is repaired after vanilla rebuilds it.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos edge = ARRIVAL.relative(side, 2).below();
            if (landingOpen(end, edge) && putPortal(end, edge)) return remember(gate, edge);
        }
        return null;
    }

    private static BlockPos remember(Gate gate, BlockPos position) {
        gate.position = position;
        gate.setDirty();
        return position;
    }

    private static boolean putPortal(ServerLevel end, BlockPos position) {
        try (var ignored = SourcePortalPlacementRules.allow()) {
            return end.setBlockAndUpdate(position, Blocks.END_PORTAL.defaultBlockState());
        }
    }

    private static boolean landingOpen(ServerLevel end, BlockPos position) {
        return end.getWorldBorder().isWithinBounds(position)
            && end.getBlockState(position.below()).isFaceSturdy(end, position.below(), Direction.UP)
            && end.isEmptyBlock(position) && end.isEmptyBlock(position.above());
    }

    private static boolean open(ServerLevel end, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 2, 1))) {
            if (!end.getWorldBorder().isWithinBounds(pos) || !end.isEmptyBlock(pos)) return false;
        }
        return true;
    }

    private record Travel(ServerLevel end, BlockPos entry, ServerLevel overworld, Vec3 spawn) {
        boolean locked(ServerLevel level) {
            if (level == end) return BossArenaPortals.blocked(end, entry);
            return level == overworld && BossArenaPortals.blocked(overworld, BlockPos.containing(spawn));
        }

        boolean touches(ServerPlayer player) {
            var state = end.getBlockState(entry);
            return state.is(Blocks.END_PORTAL) && Shapes.joinIsNotEmpty(
                Shapes.create(player.getBoundingBox().move(-entry.getX(), -entry.getY(), -entry.getZ())),
                state.getShape(end, entry), BooleanOp.AND);
        }
    }

    static void cancelLocked(ServerLevel level) {
        PENDING.values().removeIf(travel -> travel.locked(level));
    }

    /** Called at the native EndPortalBlock destination lookup, after portal collision checks. */
    static boolean travel(ServerLevel end, BlockPos pos, Entity entity) {
        if (end.dimension() != Level.END || !WorldContentMode.rebuilt(end)
                || pos.distManhattan(ARRIVAL) > 5 || !pos.equals(gate(end).position)) return false;
        if (!(entity instanceof ServerPlayer player) || player.isOnPortalCooldown()
                || BossArenaPortals.blocked(end, pos) || PENDING.containsKey(player)) return true;

        var server = end.getServer();
        var overworld = server.overworld();
        var travel = new Travel(end, pos.immutable(), overworld, safeSpawn(overworld));
        if (travel.locked(overworld)) return true;
        PENDING.put(player, travel);
        server.tell(new TickTask(server.getTickCount(), () -> finish(player, travel)));
        return true;
    }

    private static void finish(ServerPlayer player, Travel travel) {
        if (PENDING.get(player) != travel) return;
        PENDING.remove(player);
        var end = travel.end();
        if (player.serverLevel() != end || !player.connection.isAcceptingMessages()
                || !player.isAlive() || !player.canChangeDimensions() || player.isOnPortalCooldown()
                || travel.locked(end) || travel.locked(travel.overworld())
                || !travel.entry().equals(gate(end).position) || !travel.touches(player)) return;

        // A changed spawn or landing surface needs fresh portal contact.
        var spawn = travel.spawn();
        if (!spawn.equals(safeSpawn(travel.overworld()))) return;
        if (player.teleportTo(travel.overworld(), spawn.x, spawn.y, spawn.z, Set.of(), player.getYRot(), player.getXRot())) {
            player.setPortalCooldown();
            player.fallDistance = 0;
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static Vec3 safeSpawn(ServerLevel overworld) {
        var origin = overworld.getSharedSpawnPos();
        var chunk = new ChunkPos(origin);
        var safe = PlayerRespawnLogic.getSpawnPosInChunk(overworld, chunk);
        if (safe != null) return Vec3.atBottomCenterOf(safe);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            safe = PlayerRespawnLogic.getSpawnPosInChunk(overworld, new ChunkPos(origin.relative(side, 16)));
            if (safe != null) return Vec3.atBottomCenterOf(safe);
        }
        return Vec3.atBottomCenterOf(overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin));
    }

    private static Gate gate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Gate::load, Gate::new, DATA_KEY);
    }

    private static final class Gate extends SavedData {
        private BlockPos position;

        private static Gate load(CompoundTag tag) {
            var gate = new Gate();
            if (tag.contains("Position", Tag.TAG_LONG)) gate.position = BlockPos.of(tag.getLong("Position"));
            return gate;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            if (position != null) tag.putLong("Position", position.asLong());
            return tag;
        }
    }
}
