package dev.koikaster.mite;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

record BossSkullTrail(ResourceLocation dimension, int ownerId, UUID ownerUuid,
                     int projectileId, UUID projectileUuid, long startedAt) {
    private static SimpleChannel channel;
    static void register(SimpleChannel value) {
        channel = value;
        value.messageBuilder(BossSkullTrail.class, 53, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(BossSkullTrail::write).decoder(BossSkullTrail::read)
            .consumerMainThread(BossSkullTrail::receive).add();
    }
    static void sync(ServerLevel level, SourceWitherBoss owner, SourceWitherSkull skull) {
        var packet = new BossSkullTrail(level.dimension().location(), owner.getId(), owner.getUUID(),
            skull.getId(), skull.getUUID(), level.getGameTime());
        for (var player : level.players()) if (player.distanceToSqr(skull) <= 96 * 96)
            channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension); buffer.writeVarInt(ownerId); buffer.writeUUID(ownerUuid);
        buffer.writeVarInt(projectileId); buffer.writeUUID(projectileUuid); buffer.writeLong(startedAt);
    }
    private static BossSkullTrail read(FriendlyByteBuf buffer) {
        return new BossSkullTrail(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readUUID(),
            buffer.readVarInt(), buffer.readUUID(), buffer.readLong());
    }
    private static void receive(BossSkullTrail packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBossCombatVisual.receive(packet));
        context.get().setPacketHandled(true);
    }
}
