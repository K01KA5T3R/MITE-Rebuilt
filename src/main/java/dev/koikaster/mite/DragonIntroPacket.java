package dev.koikaster.mite;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

record DragonIntroPacket(UUID dragon, long startedAt) {
    private static final int PACKET_ID = 46;
    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(DragonIntroPacket.class, PACKET_ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(DragonIntroPacket::write).decoder(DragonIntroPacket::read)
            .consumerMainThread(DragonIntroPacket::receive).add();
    }

    static void send(ServerPlayer player, UUID dragon, long startedAt) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new DragonIntroPacket(dragon, startedAt));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(dragon);
        buffer.writeLong(startedAt);
    }

    private static DragonIntroPacket read(FriendlyByteBuf buffer) {
        return new DragonIntroPacket(buffer.readUUID(), buffer.readLong());
    }

    private static void receive(DragonIntroPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDragonIntro.receive(packet));
        context.get().setPacketHandled(true);
    }
}
