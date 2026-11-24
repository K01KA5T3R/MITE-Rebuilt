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

record WitherIntroPacket(UUID encounter, ResourceLocation dimension, long startedAt) {
    static final int DURATION = 76;
    private static final int PACKET_ID = 48;
    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(WitherIntroPacket.class, PACKET_ID, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(WitherIntroPacket::write).decoder(WitherIntroPacket::read)
            .consumerMainThread(WitherIntroPacket::receive).add();
    }

    static void send(ServerPlayer player, UUID encounter, ResourceLocation dimension, long startedAt) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), new WitherIntroPacket(encounter, dimension, startedAt));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(encounter);
        buffer.writeResourceLocation(dimension);
        buffer.writeLong(startedAt);
    }

    private static WitherIntroPacket read(FriendlyByteBuf buffer) {
        return new WitherIntroPacket(buffer.readUUID(), buffer.readResourceLocation(), buffer.readLong());
    }

    private static void receive(WitherIntroPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDragonIntro.receiveWither(packet));
        context.get().setPacketHandled(true);
    }
}
