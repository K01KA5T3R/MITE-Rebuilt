package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID, value=Dist.CLIENT)
public final class ClientCooperativeMining2133 {
    private static final class Attempt {
        final ClientLevel world;
        final LocalPlayer player;
        final BlockPos pos;
        final int sequence;
        float received;
        float applied;

        Attempt(Minecraft minecraft, ServerboundPlayerActionPacket packet) {
            world = minecraft.level;
            player = minecraft.player;
            pos = packet.getPos().immutable();
            sequence = packet.getSequence();
        }

        boolean current(Minecraft minecraft, Object pos) {
            return world == minecraft.level && player == minecraft.player && this.pos.equals(pos);
        }
    }

    private static Attempt attempt;

    public static void outgoing(Object rawPacket) {
        if (!(rawPacket instanceof ServerboundPlayerActionPacket packet)) return;
        var minecraft = Minecraft.getInstance();
        switch (packet.getAction()) {
            case START_DESTROY_BLOCK -> {
                if (minecraft.level != null && minecraft.player != null) attempt = new Attempt(minecraft, packet);
            }
            case STOP_DESTROY_BLOCK, ABORT_DESTROY_BLOCK -> {
                if (attempt != null && attempt.current(minecraft, packet.getPos())) attempt = null;
            }
            default -> {}
        }
    }

    static void credit(CooperativeMining2133.Credit credit) {
        var held = attempt;
        if (held != null && held.current(Minecraft.getInstance(), credit.pos()) && held.sequence == credit.sequence()
            && held.world.dimension().location().equals(credit.dimension()) && Float.isFinite(credit.bonus())) {
            held.received = Math.max(held.received, credit.bonus());
        }
    }

    public static float progress(float base, Object pos) {
        var held = attempt;
        if (held == null || !(base > 0f) || !held.current(Minecraft.getInstance(), pos)) return base;
        float extra = held.received - held.applied;
        held.applied = held.received;
        return base + extra;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        attempt = null;
    }

    private ClientCooperativeMining2133() {}
}

