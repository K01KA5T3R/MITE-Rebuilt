package dev.koikaster.mite;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

record BossMusicPacket(UUID encounter, Action action, BossMusicTrack track, ResourceLocation dimension, int gain, int ticks) {
    enum Action { START, STOP, DUCK }

    private static final int ID = 47;
    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(BossMusicPacket.class, ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(BossMusicPacket::write).decoder(BossMusicPacket::read)
            .consumerMainThread(BossMusicPacket::receive).add();
    }

    static void start(ServerPlayer player, UUID encounter, BossMusicTrack track, ResourceLocation dimension) {
        send(player, new BossMusicPacket(encounter, Action.START, track, dimension, 100, 0));
    }

    static void stop(ServerPlayer player, UUID encounter) {
        send(player, new BossMusicPacket(encounter, Action.STOP, null, null, 100, 0));
    }

    static void duck(ServerPlayer player, UUID encounter, int gain, int ticks) {
        send(player, new BossMusicPacket(encounter, Action.DUCK, null, null, gain, ticks));
    }

    private static void send(ServerPlayer player, BossMusicPacket packet) {
        if (player.connection.isAcceptingMessages()) channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private void write(FriendlyByteBuf out) {
        out.writeUUID(encounter);
        out.writeEnum(action);
        switch (action) {
            case START -> {
                out.writeEnum(track);
                out.writeResourceLocation(dimension);
            }
            case DUCK -> {
                out.writeByte(gain);
                out.writeVarInt(ticks);
            }
            case STOP -> { }
        }
    }

    private static BossMusicPacket read(FriendlyByteBuf in) {
        UUID encounter = in.readUUID();
        Action action = in.readEnum(Action.class);
        return switch (action) {
            case START -> new BossMusicPacket(encounter, action, in.readEnum(BossMusicTrack.class), in.readResourceLocation(), 100, 0);
            case STOP -> new BossMusicPacket(encounter, action, null, null, 100, 0);
            case DUCK -> new BossMusicPacket(encounter, action, null, null, in.readUnsignedByte(), in.readVarInt());
        };
    }

    private static void receive(BossMusicPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBossMusic.receive(packet));
        context.get().setPacketHandled(true);
    }
}
