package dev.koikaster.mite;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Adds time earned while a second player is actively breaking the same block. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class CooperativeMining2133 {
    private static final class Attempt {
        final ServerLevel world;
        final BlockPos pos;
        final BlockState state;
        final ItemStack tool;
        final int slot;
        final int sequence;
        float bonus;
        long lastAward = Long.MIN_VALUE;

        Attempt(ServerPlayer player, BlockPos pos, BlockState state, int sequence) {
            world = player.serverLevel();
            this.pos = pos.immutable();
            this.state = state;
            tool = player.getMainHandItem().copy();
            slot = player.getInventory().selected;
            this.sequence = sequence;
        }

        boolean usable(ServerPlayer player) {
            return player.serverLevel() == world && eligible(player)
                && player.getInventory().selected == slot && ItemStack.isSameItemSameTags(tool, player.getMainHandItem())
                && world.hasChunkAt(pos) && world.getBlockState(pos) == state
                && OriginalReach.canSelectBlock(player, pos, 1f);
        }
    }

    private static final class Mining {
        Attempt active;
        Attempt delayed;
        boolean tickingDelayed;
    }

    private static final Map<ServerPlayer, Mining> ATTEMPTS = new WeakHashMap<>();
    private static SimpleChannel channel;

    private static boolean eligible(ServerPlayer player) {
        return WorldContentMode.rebuilt(player.level()) && !(player instanceof FakePlayer)
            && player.isAlive() && !player.isSpectator() && !player.isCreative()
            && player.connection != null && player.connection.connection.isConnected()
            && !Incapacitation2123.incapacitated(player);
    }

    private static boolean container(ServerLevel world, BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof EnderChestBlock || state.getBlock() instanceof WorldlyContainerHolder
            || state.getMenuProvider(world, pos) != null) return true;
        if (!state.hasBlockEntity()) return false;
        var entity = world.getBlockEntity(pos);
        return entity instanceof Container || entity instanceof MenuProvider || entity instanceof Grave.Entity
            || entity != null && entity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }

    // Invoked only at the native assignment that accepts a timed START.
    public static void accepted(Object rawPlayer, Object rawPos, int sequence, float rate) {
        var player = (ServerPlayer) rawPlayer;
        var pos = (BlockPos) rawPos;
        var mining = ATTEMPTS.get(player);
        if (mining != null) mining.active = null;
        if (!(rate > 0f && rate < 1f) || !eligible(player)) return;
        var world = player.serverLevel();
        var state = world.getBlockState(pos);
        if (state.isAir() || container(world, state, pos)) return;
        if (mining == null) {
            mining = new Mining();
            ATTEMPTS.put(player, mining);
        }
        mining.active = new Attempt(player, pos, state, sequence);
    }

    // The delayed vanilla STOP may coexist with a later START on a different block.
    public static void sync(Object rawPlayer, boolean active, Object activePos, boolean delayed, Object delayedPos) {
        var player = (ServerPlayer) rawPlayer;
        var mining = ATTEMPTS.get(player);
        if (mining == null) return;
        mining.tickingDelayed = delayed;
        if (mining.delayed != null && (!delayed || !mining.delayed.pos.equals(delayedPos) || !mining.delayed.usable(player))) {
            mining.delayed = null;
        }
        var held = mining.active;
        if (held != null && (!active || !held.pos.equals(activePos) || !held.usable(player))) {
            if (delayed && mining.delayed == null && held.pos.equals(delayedPos) && held.usable(player)) {
                mining.delayed = held;
            }
            mining.active = null;
        }
        if (mining.active == null && mining.delayed == null) ATTEMPTS.remove(player);
    }

    public static float tickProgress(float original, float rate, Object rawPlayer, Object rawState, Object rawPos, boolean delayed) {
        if (!(rate > 0f)) return original;
        var player = (ServerPlayer) rawPlayer;
        var mining = ATTEMPTS.get(player);
        if (mining == null) return original;
        var attempt = delayed ? mining.delayed : mining.active;
        if (attempt == null || attempt.state != rawState || !attempt.pos.equals(rawPos) || !attempt.usable(player)) return original;
        long tick = attempt.world.getGameTime();
        if (!delayed && attempt.lastAward != tick && hasPartner(player, attempt)) {
            attempt.lastAward = tick;
            attempt.bonus += rate;
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                new Credit(attempt.world.dimension().location(), attempt.pos, attempt.sequence, attempt.bonus));
        }
        return original + attempt.bonus;
    }

    private static boolean hasPartner(ServerPlayer player, Attempt attempt) {
        for (var entry : ATTEMPTS.entrySet()) {
            var peer = entry.getKey();
            var mining = entry.getValue();
            var held = mining.active;
            if (peer != null && peer != player && held != null && !mining.tickingDelayed && held.world == attempt.world
                && held.state == attempt.state && held.pos.equals(attempt.pos) && held.usable(peer)) {
                float rate = held.state.getDestroyProgress(peer, held.world, held.pos);
                if (rate > 0f && rate < 1f) return true;
            }
        }
        return false;
    }

    public static float stopProgress(float original, Object rawPlayer, Object rawPos) {
        if (!(original > 0f)) return original;
        var player = (ServerPlayer) rawPlayer;
        var mining = ATTEMPTS.get(player);
        if (mining == null) return original;
        var held = mining.active;
        if (held == null || !held.pos.equals(rawPos)) held = mining.delayed;
        return held != null && held.pos.equals(rawPos) && held.usable(player) ? original + held.bonus : original;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ATTEMPTS.remove(player);
    }

    public static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(Credit.class, 45, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Credit::write).decoder(Credit::read).consumerMainThread(Credit::handle).add();
    }

    public record Credit(ResourceLocation dimension, BlockPos pos, int sequence, float bonus) {
        static void write(Credit credit, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(credit.dimension);
            buffer.writeBlockPos(credit.pos);
            buffer.writeVarInt(credit.sequence);
            buffer.writeFloat(credit.bonus);
        }

        static Credit read(FriendlyByteBuf buffer) {
            return new Credit(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readFloat());
        }

        static void handle(Credit credit, Supplier<NetworkEvent.Context> context) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCooperativeMining2133.credit(credit));
            context.get().setPacketHandled(true);
        }
    }

    private CooperativeMining2133() {}
}

