package dev.koikaster.mite;

import java.util.List;
import java.util.Map;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;

/** R195 thin snow, snow slab and full block share one eight-layer material. */
public final class SourceSnowMaterials {
    private static final int SLAB_DEPTH = 4;
    private static final int BLOCK_DEPTH = 8;

    static final class SlabItem extends BlockItem {
        SlabItem() {
            super(Blocks.SNOW, new Item.Properties().stacksTo(8));
        }
        @Override public String getDescriptionId() { return "item.mite_reforged.snow_slab"; }
        @Override
        public void registerBlocks(Map<Block, Item> blockToItem, Item item) {
            // Thin snow remains the canonical item for the existing snow block.
        }

        @Override
        public void removeFromBlockToItemMap(Map<Block, Item> blockToItem, Item item) {
        }

        @Override
        protected BlockState getPlacementState(BlockPlaceContext context) {
            var state = layers(SLAB_DEPTH);
            return canPlace(context, state) ? state : null;
        }
    }

    private static BlockState layers(int depth) {
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, depth);
    }

    private static int depth(ItemStack stack) {
        if (stack.is(Items.SNOW)) return 1;
        if (stack.is(Items.SNOW_BLOCK)) return BLOCK_DEPTH;
        return stack.is(Content.SNOW_SLAB.get()) ? SLAB_DEPTH : 0;
    }

    /** Merge at the hit or adjacent snow inside the existing Forge placement transaction. */
    public static InteractionResult tryMerge(Object rawContext) {
        var context = (BlockPlaceContext) rawContext;
        var stack = context.getItemInHand();
        var player = context.getPlayer();
        int added = depth(stack);
        if (added == 0 || player == null || player.isSpectator()) return null;

        var level = context.getLevel();
        var clicked = context.getClickedPos();
        var face = context.getClickedFace();
        var hit = context.replacingClickedOnBlock() ? clicked : clicked.relative(face.getOpposite());
        var target = hit;
        var previous = level.getBlockState(target);
        if (!partial(previous)) {
            if (context.replacingClickedOnBlock()) return null;
            target = hit.relative(face);
            previous = level.getBlockState(target);
            if (!partial(previous)) return null;
        }

        int total = previous.getValue(SnowLayerBlock.LAYERS) + added;
        var base = total >= BLOCK_DEPTH ? Blocks.SNOW_BLOCK.defaultBlockState() : layers(total);
        if (!canEdit(context, target, base) || !base.canSurvive(level, target)) return InteractionResult.FAIL;
        var above = target.above();
        var overflow = total > BLOCK_DEPTH ? layers(total - BLOCK_DEPTH) : null;
        if (overflow != null) {
            var aboveState = level.getBlockState(above);
            if (!canEdit(context, above, overflow) || aboveState.is(Blocks.SNOW)
                    || aboveState.hasBlockEntity() || !aboveState.canBeReplaced()) return InteractionResult.FAIL;
        }

        if (!level.setBlock(target, base, 11)) return InteractionResult.FAIL;
        if (overflow != null && !level.setBlock(above, overflow, 11)) {
            level.setBlock(target, previous, 11);
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, target, stack);
        var sound = base.getSoundType(level, target, player);
        level.playSound(player, target, sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * .8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(player, base));
        if (!player.getAbilities().instabuild) SourcePlacement1075.consume(stack, 1);
        return SourcePlacement1075.success(level.isClientSide, context);
    }

    private static boolean partial(BlockState state) {
        return state.is(Blocks.SNOW) && state.getValue(SnowLayerBlock.LAYERS) < BLOCK_DEPTH;
    }

    private static boolean canEdit(BlockPlaceContext context, BlockPos pos, BlockState state) {
        var level = context.getLevel();
        var player = context.getPlayer();
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
            && level.mayInteract(player, pos) && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())
            && level.isUnobstructed(state, pos, CollisionContext.of(player));
    }

    static void loot(List<ItemStack> loot, LootContext context, BlockState state) {
        boolean full = state.is(Blocks.SNOW_BLOCK);
        if (!full && !state.is(Blocks.SNOW)) return;
        loot.removeIf(stack -> stack.is(Items.SNOWBALL) || stack.is(Items.SNOW_BLOCK) || stack.is(Items.SNOW));
        boolean playerHarvest = context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player
            && !context.hasParam(LootContextParams.EXPLOSION_RADIUS);
        if (!full && !playerHarvest) return;
        var tool = context.getParamOrNull(LootContextParams.TOOL);
        int depth = full ? BLOCK_DEPTH : state.getValue(SnowLayerBlock.LAYERS);
        if (playerHarvest && tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            // Previous worlds can contain the native eight-layer state.
            Item recovered = depth == BLOCK_DEPTH ? Items.SNOW_BLOCK
                : depth == SLAB_DEPTH ? Content.SNOW_SLAB.get() : Items.SNOW;
            loot.add(new ItemStack(recovered));
        } else {
            loot.add(new ItemStack(Items.SNOWBALL, depth));
        }
    }

    private SourceSnowMaterials() {
    }
}
