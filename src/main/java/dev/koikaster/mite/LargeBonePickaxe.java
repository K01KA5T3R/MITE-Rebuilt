package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;
import java.util.WeakHashMap;

final class LargeBonePickaxe extends PickaxeItem {
    private static final ThreadLocal<SweepStep> SWEEP = new ThreadLocal<>();
    private static final Map<ServerPlayer, MiningFace> FACES = new WeakHashMap<>();

    private record MiningFace(Level level, BlockPos pos, Direction face, ItemStack tool) {}
    private record SweepStep(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack tool, SweepStep outer) {}

    LargeBonePickaxe() {
        super(Tiers.IRON,1,-2.8f,new Properties()
            .durability(LegacyToolWear.maxDamage(LegacyToolWear.Kind.PICKAXE,"iron")).setNoRepair());
    }

    @Override public boolean isEnchantable(ItemStack stack) { return false; }

    @Override public int getEnchantmentValue() { return 0; }

    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book) { return false; }

    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient) { return false; }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity user) {
        if (user instanceof ServerPlayer player) EnchantmentEffects.noteBonePickaxeUse(player, level, pos, stack);
        return true;
    }

    static ItemStack miningReference(ItemStack stack) {
        return matches(stack) ? new ItemStack(Items.IRON_PICKAXE) : stack;
    }

    static boolean matches(ItemStack stack) { return stack.getItem() instanceof LargeBonePickaxe; }

    static void action(ServerPlayer player, BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face) {
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            var mining = FACES.get(player);
            if (mining != null && mining.pos().equals(pos)) FACES.remove(player);
        } else if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            FACES.remove(player);
            var tool = player.getMainHandItem();
            if (tool.getItem() instanceof LargeBonePickaxe && !player.isCreative() && !player.isSpectator()) {
                FACES.put(player, new MiningFace(player.level(), pos.immutable(), face, tool));
            }
        }
    }

    static void afterCentralBreak(ServerPlayer player, ServerLevel level, BlockPos center, BlockState before,
                                  ItemStack tool, boolean removed) {
        if (activeSweep(player) != null) return;
        var mining = FACES.get(player);
        if (mining != null && mining.pos().equals(center)) FACES.remove(player);
        if (!removed || tool == null || !matches(tool) || player.isCreative() || player.isSpectator()
                || before.getDestroySpeed(level, center) <= 0) return;
        try {
            if (mining != null && mining.level() == level && mining.pos().equals(center) && mining.tool() == tool
                    && wields(player, level, tool) && eligible(player, level, center, before)) {
                sweep(player, level, center, mining.face(), tool);
            }
        } finally {
            if (!tool.isEmpty() && carries(player, tool)) {
                tool.hurtAndBreak(1, player, owner -> {
                    if (owner.getMainHandItem() == tool) owner.broadcastBreakEvent(EquipmentSlot.MAINHAND);
                });
            }
        }
    }

    private static boolean carries(ServerPlayer player, ItemStack tool) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot) == tool) return true;
        }
        return player.containerMenu.getCarried() == tool;
    }

    private static boolean wields(ServerPlayer player, ServerLevel level, ItemStack tool) {
        return player.serverLevel() == level && player.getMainHandItem() == tool && !tool.isEmpty()
            && !player.isCreative() && !player.isSpectator();
    }

    private static SweepStep activeSweep(ServerPlayer player) {
        for (var step = SWEEP.get(); step != null; step = step.outer()) {
            if (step.player() == player) return step;
        }
        return null;
    }

    static boolean allowSweepTool(ServerPlayer player, Level level, BlockPos pos, ItemStack stack) {
        var step = activeSweep(player);
        if (step == null || !step.pos().equals(pos)) return true;
        if (level != step.level() || player.serverLevel() != step.level()) return false;
        if (step.tool() == stack && wields(player, step.level(), stack)) return true;
        // Forge's break preflight sent an optimistic fluid-state packet before its listeners ran.
        if (player.connection != null) player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        return false;
    }

    private static void sweep(ServerPlayer player, ServerLevel level, BlockPos center, Direction face, ItemStack tool) {
        Direction first = face.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
        Direction second = face.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        var outer = SWEEP.get();
        for (int row = -1; row <= 1; row++) {
            for (int column = -1; column <= 1; column++) {
                if (row == 0 && column == 0) continue;
                if (!wields(player, level, tool)) return;
                var pos = center.relative(first, row).relative(second, column);
                if (!level.hasChunkAt(pos)) continue;
                if (!eligible(player, level, pos, level.getBlockState(pos))) continue;
                if (!wields(player, level, tool)) return;
                SWEEP.set(new SweepStep(player, level, pos, tool, outer));
                try {
                    player.gameMode.destroyBlock(pos);
                } finally {
                    if (outer == null) SWEEP.remove();
                    else SWEEP.set(outer);
                }
            }
        }
    }

    private static boolean eligible(ServerPlayer player,ServerLevel level,BlockPos pos,BlockState state) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
            && level.mayInteract(player,pos) && !state.hasBlockEntity() && state.is(BlockTags.MINEABLE_WITH_PICKAXE)
            && state.getDestroySpeed(level,pos)>0 && HarvestRules.canHarvest(player,state,level,pos);
    }
}
