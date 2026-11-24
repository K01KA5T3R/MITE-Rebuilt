package dev.koikaster.mite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

final class WorldContentTransport {
    private static SimpleChannel channel;

    static void register(SimpleChannel value) {
        channel = value;
        channel.messageBuilder(WorldExtraSettings.class, 58, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((settings, buffer) -> buffer.writeByte(settings.encode()))
            .decoder(buffer -> {
                int flags = buffer.readUnsignedByte();
                var settings = WorldExtraSettings.decode(flags, null);
                if (settings == null) throw new IllegalArgumentException("Invalid MITE world settings: " + flags);
                return settings;
            })
            .consumerMainThread((settings, context) -> {
                var connection = context.get().getNetworkManager();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWorldContentMode.receive(connection, settings));
                context.get().setPacketHandled(true);
            })
            .add();
    }

    static void sync(ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), WorldContentMode.settings(player.level()));
    }

    private WorldContentTransport() {}
}
