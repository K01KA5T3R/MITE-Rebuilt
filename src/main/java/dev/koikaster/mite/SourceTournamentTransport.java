package dev.koikaster.mite;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

final class SourceTournamentTransport {
    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(Share.class, 54, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.remaining()))
                .decoder(Share::read).consumerMainThread(Share::handle).add();
        value.messageBuilder(Score.class, 55, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Score::write).decoder(Score::read).consumerMainThread(Score::handle).add();
        value.messageBuilder(Prize.class, 56, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Prize::write).decoder(Prize::read).consumerMainThread(Prize::handle).add();
    }

    static void send(ServerPlayer player, int remaining) {
        if (player.connection == null || player instanceof FakePlayer) return;
        channel.send(PacketDistributor.PLAYER.with(() -> player), new Share(remaining));
    }

    private record Share(int remaining) {
        static Share read(FriendlyByteBuf buffer) {
            return new Share(buffer.readVarInt());
        }

        static void handle(Share packet, Supplier<NetworkEvent.Context> supplied) {
            var context = supplied.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientTournamentTime.receive(context.getNetworkManager(), packet.remaining()));
            context.setPacketHandled(true);
        }
    }

    static void sendScore(ServerPlayer player, int points, boolean showChange) {
        if (player.connection == null || player instanceof FakePlayer) return;
        channel.send(PacketDistributor.PLAYER.with(() -> player), new Score(points, showChange));
    }

    private record Score(int points, boolean showChange) {
        static void write(Score packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.points());
            buffer.writeBoolean(packet.showChange());
        }

        static Score read(FriendlyByteBuf buffer) {
            return new Score(buffer.readVarInt(), buffer.readBoolean());
        }

        static void handle(Score packet, Supplier<NetworkEvent.Context> supplied) {
            var context = supplied.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientTournamentScore.receive(context.getNetworkManager(), packet.points(), packet.showChange()));
            context.setPacketHandled(true);
        }
    }

    static void sendPrize(ServerPlayer player, int code) {
        if (player.connection == null || player instanceof FakePlayer) return;
        channel.send(PacketDistributor.PLAYER.with(() -> player), new Prize(code));
    }

    private record Prize(int code) {
        static void write(Prize packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.code());
        }

        static Prize read(FriendlyByteBuf buffer) {
            return new Prize(buffer.readVarInt());
        }

        static void handle(Prize packet, Supplier<NetworkEvent.Context> supplied) {
            var context = supplied.get();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientTournamentPrize.receive(context.getNetworkManager(), packet.code()));
            context.setPacketHandled(true);
        }
    }

    private SourceTournamentTransport() {}
}

