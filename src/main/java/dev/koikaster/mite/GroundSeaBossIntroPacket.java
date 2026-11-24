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

record GroundSeaBossIntroPacket(UUID encounter, ResourceLocation dimension, long startedAt, Kind kind) {
    enum Kind { GIANT, ELDER, CRABULON, MYTHRIL_KNIGHT, REDSTONE_GOLEM, GIANT_SKELETON }

    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(GroundSeaBossIntroPacket.class, 49, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(GroundSeaBossIntroPacket::write).decoder(GroundSeaBossIntroPacket::read)
            .consumerMainThread(GroundSeaBossIntroPacket::receive).add();
    }

    static void send(ServerPlayer player, UUID encounter, long startedAt, boolean sea) {
        send(player, encounter, startedAt, sea ? Kind.ELDER : Kind.GIANT);
    }

    static void send(ServerPlayer player, UUID encounter, long startedAt, Kind kind) {
        channel.send(PacketDistributor.PLAYER.with(() -> player),
            new GroundSeaBossIntroPacket(encounter, player.level().dimension().location(), startedAt, kind));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(encounter);
        buffer.writeResourceLocation(dimension);
        buffer.writeLong(startedAt);
        buffer.writeEnum(kind);
    }

    private static GroundSeaBossIntroPacket read(FriendlyByteBuf buffer) {
        return new GroundSeaBossIntroPacket(buffer.readUUID(), buffer.readResourceLocation(), buffer.readLong(), buffer.readEnum(Kind.class));
    }

    private static void receive(GroundSeaBossIntroPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientDragonIntro.receiveGroundSea(packet));
        context.get().setPacketHandled(true);
    }
}
