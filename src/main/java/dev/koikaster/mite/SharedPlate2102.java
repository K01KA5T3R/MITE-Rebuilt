package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegistryObject;

public final class SharedPlate2102 {
    private static final RegistryObject<Block> BLOCK = Content.BLOCKS.register("shared_plate", Plate::new);
    private static final RegistryObject<Item> ITEM = Content.item("shared_plate", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    static final RegistryObject<BlockEntityType<PlateEntity>> ENTITY = Content.BLOCK_ENTITIES.register("shared_plate",
        () -> BlockEntityType.Builder.of(PlateEntity::new, BLOCK.get()).build(null));

    public static void register() {
        Content.ALL_BLOCKS.put("shared_plate", BLOCK);
    }

    private static boolean food(ItemStack stack) {
        return !stack.isEmpty() && stack.isEdible() && stack.getUseAnimation() == UseAnim.EAT;
    }

    private static final class Plate extends BaseEntityBlock {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 2, 14);

        private Plate() {
            super(Properties.copy(Blocks.CLAY).strength(.4f).noOcclusion());
        }

        @Override public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new PlateEntity(pos, state);
        }

        @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return Block.canSupportCenter(level, pos.below(), Direction.UP);
        }

        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            var state = defaultBlockState();
            var pos = context.getClickedPos();
            var level = context.getLevel();
            return level.getFluidState(pos).isEmpty() && canSurvive(state, level, pos) ? state : null;
        }

        @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            if (direction == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
            return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        }

        @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!WorldContentMode.rebuilt(level) || hand != InteractionHand.MAIN_HAND || player.isSpectator()
                    || !(level.getBlockEntity(pos) instanceof PlateEntity plate)) {
                return InteractionResult.PASS;
            }
            var held = player.getItemInHand(hand);
            var stored = plate.contents;
            if (food(held) && (stored.isEmpty() || ItemStack.isSameItemSameTags(held, stored))) {
                int transfer = Math.min(held.getCount(), held.getMaxStackSize() - stored.getCount());
                if (transfer <= 0) return InteractionResult.PASS;
                if (!level.isClientSide) {
                    if (stored.isEmpty()) plate.contents = held.copyWithCount(transfer);
                    else stored.grow(transfer);
                    if (!player.isCreative()) held.shrink(transfer);
                    plate.changed();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!held.isEmpty() || stored.isEmpty() || player.isUsingItem()) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;

            var serving = stored.copyWithCount(1);
            if (ForgeEventFactory.onItemUseStart(player, serving, serving.getUseDuration()) <= 0) return InteractionResult.PASS;
            var original = serving.copy();
            stored.shrink(1);
            if (stored.isEmpty()) plate.contents = ItemStack.EMPTY;
            plate.changed();
            var remainder = ForgeEventFactory.onItemUseFinish(player, original, 0, serving.finishUsingItem(level, player));
            if (!remainder.isEmpty() && !ItemStack.isSameItemSameTags(original, remainder)) {
                ItemHandlerHelper.giveItemToPlayer(player, remainder);
            }
            return InteractionResult.CONSUME;
        }

        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (!level.isClientSide && !state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof PlateEntity plate) {
                var remaining = plate.contents;
                plate.contents = ItemStack.EMPTY;
                if (!remaining.isEmpty()) popResource(level, pos, remaining);
            }
            super.onRemove(state, level, pos, next, moving);
        }
    }

    static final class PlateEntity extends BlockEntity {
        private ItemStack contents = ItemStack.EMPTY;

        private PlateEntity(BlockPos pos, BlockState state) {
            super(ENTITY.get(), pos, state);
        }

        ItemStack displayedFood() {
            return contents;
        }

        @Override public void load(CompoundTag tag) {
            super.load(tag);
            var saved = ItemStack.of(tag.getCompound("Food"));
            contents = food(saved) ? saved.copyWithCount(Math.min(saved.getCount(), saved.getMaxStackSize())) : ItemStack.EMPTY;
        }

        @Override protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!contents.isEmpty()) tag.put("Food", contents.save(new CompoundTag()));
        }

        @Override public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        private void changed() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private SharedPlate2102() {}
}
