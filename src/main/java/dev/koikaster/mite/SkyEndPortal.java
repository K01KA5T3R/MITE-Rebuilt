package dev.koikaster.mite;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class SkyEndPortal extends Block {
    private static final VoxelShape X_SHAPE = box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = box(6, 0, 0, 10, 16, 16);
    private static final Map<ServerPlayer, Travel> PENDING = new WeakHashMap<>();

    SkyEndPortal(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return portalShape(state.getValue(BlockStateProperties.HORIZONTAL_AXIS));
    }

    private static VoxelShape portalShape(Direction.Axis axis) {
        return axis == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction face, BlockState adjacent, LevelAccessor level,
                                  BlockPos pos, BlockPos neighbor) {
        return findFrame(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_AXIS), true) == null
            ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity actor) {
        if (!(level instanceof ServerLevel source) || !WorldContentMode.rebuilt(level)
                || source.dimension() != Level.OVERWORLD || !actor.canChangeDimensions()) return;
        var axis = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        if (!actor.getBoundingBox().intersects(portalShape(axis).bounds().move(pos))) return;
        var frame = findFrame(source, pos, axis, true);
        if (frame == null || frame.locked(source)) return;
        var end = source.getServer().getLevel(Level.END);
        if (end != null) transfer(actor, source, end, frame);
    }

    private record Frame(BlockPos bottom, Direction along) {
        private Frame { bottom = bottom.immutable(); }

        BlockPos at(int x, int y) { return bottom.relative(along, x).above(y); }
        Direction.Axis axis() { return along.getAxis(); }

        boolean locked(ServerLevel level) {
            return BossArenaPortals.blockedPortal(level, new BlockUtil.FoundRectangle(bottom, 2, 3), axis());
        }

        boolean touches(Entity actor) {
            AABB slab = portalShape(axis()).bounds().move(bottom);
            return actor.getBoundingBox().intersects(slab.expandTowards(along.getStepX(), 2, along.getStepZ()));
        }

        boolean complete(LevelAccessor level, boolean lit) {
            for (int x = -1; x <= 2; x++) {
                for (int y = -1; y <= 3; y++) {
                    boolean side = x == -1 || x == 2;
                    boolean cap = y == -1 || y == 3;
                    if (side && cap) continue;
                    var pos = at(x, y);
                    if (!level.getWorldBorder().isWithinBounds(pos)) return false;
                    var state = level.getBlockState(pos);
                    if (side || cap) {
                        if (!state.is(CoreProgression.ENDER_OBSIDIAN.get())) return false;
                    } else if (lit) {
                        if (!state.is(CoreProgression.SKY_PORTAL.get())
                            || state.getValue(BlockStateProperties.HORIZONTAL_AXIS) != axis()) return false;
                    } else if (!state.isAir() && !(state.getBlock() instanceof BaseFireBlock)) {
                        return false;
                    }
                }
            }
            return true;
        }

        void light(Level level) {
            var state = CoreProgression.SKY_PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis());
            for (int x = 0; x < 2; x++) for (int y = 0; y < 3; y++) level.setBlock(at(x, y), state, 18);
        }
    }

    private static Frame findFrame(LevelAccessor level, BlockPos inside, Direction.Axis axis, boolean lit) {
        int row = inside.getY() - (CoreProgression.SKY_FRAME_Y - 3);
        if (row < 0 || row >= 3 || level.isOutsideBuildHeight(CoreProgression.SKY_FRAME_Y)) return null;
        var direction = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        var bottom = inside.below(row);
        var first = new Frame(bottom, direction);
        if (first.complete(level, lit)) return first;
        var second = new Frame(bottom.relative(direction.getOpposite()), direction);
        return second.complete(level, lit) ? second : null;
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (!WorldContentMode.rebuilt(level)) return;
        var stack = event.getItemStack();
        var clicked = event.getPos();
        if (stack.is(Items.ENDER_EYE) && level.getBlockState(clicked).is(Blocks.END_PORTAL_FRAME)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (level.dimension() != Level.OVERWORLD) return;
        if (!stack.is(Items.FLINT_AND_STEEL) || event.getFace() == null
            || !level.getBlockState(clicked).is(CoreProgression.ENDER_OBSIDIAN.get())) return;
        var player = event.getEntity();
        var ignition = clicked.relative(event.getFace());
        if (!level.mayInteract(player, clicked) || !player.mayUseItemAt(ignition, event.getFace(), stack)) return;
        var frame = findFrame(level, ignition, Direction.Axis.X, false);
        if (frame == null) frame = findFrame(level, ignition, Direction.Axis.Z, false);
        if (frame == null) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (!level.isClientSide) {
            frame.light(level);
            level.playSound(null, ignition, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                level.random.nextFloat() * 0.4F + 0.8F);
            if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, ignition, stack);
            stack.hurtAndBreak(1, player, actor -> actor.broadcastBreakEvent(event.getHand()));
        }
    }

    private record Travel(ServerLevel source, ServerLevel end, Frame frame) {
        boolean locked(ServerLevel level) {
            if (level == source) return frame.locked(source);
            return level == end && BossArenaPortals.blocked(end, ServerLevel.END_SPAWN_POINT.below());
        }
    }

    static void cancelLocked(ServerLevel level) {
        PENDING.values().removeIf(travel -> travel.locked(level));
    }

    private static void transfer(Entity actor, ServerLevel source, ServerLevel end, Frame frame) {
        if (!(actor instanceof ServerPlayer player)) {
            if (!BossArenaPortals.blocked(end, ServerLevel.END_SPAWN_POINT)) actor.changeDimension(end);
            return;
        }
        var travel = new Travel(source, end, frame);
        if (travel.locked(end) || PENDING.putIfAbsent(player, travel) != null) return;
        // Player movement can keep writing coordinates after the collision callback.
        var server = source.getServer();
        server.tell(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
            if (PENDING.get(player) != travel) return;
            PENDING.remove(player);
            if (player.serverLevel() == source && player.isAlive() && player.canChangeDimensions()
                && !travel.locked(source) && !travel.locked(end)
                && frame.touches(player) && frame.complete(source, true)) player.changeDimension(end);
        }));
    }
}
