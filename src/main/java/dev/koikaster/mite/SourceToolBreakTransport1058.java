package dev.koikaster.mite;
import java.util.UUID;import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.fml.DistExecutor;import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;import net.minecraftforge.registries.ForgeRegistries;
/** Generic player item-breaking identity; the sealed stick/bone message remains unchanged. */
public final class SourceToolBreakTransport1058 {
 private static SimpleChannel channel;
 public static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(Packet.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(Packet::write).decoder(Packet::read).consumerMainThread(Packet::handle).add();}
 public static void broken(Player player,Item item,int slot){if(player instanceof ServerPlayer)channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),new Packet(player.getId(),player.getUUID(),item,slot));}
 public record Packet(int entity,UUID uuid,Item item,int slot){
  public static void write(Packet value,FriendlyByteBuf buffer){buffer.writeInt(value.entity);buffer.writeUUID(value.uuid);buffer.writeResourceLocation(java.util.Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(value.item)));buffer.writeByte(value.slot);}
  public static Packet read(FriendlyByteBuf buffer){return new Packet(buffer.readInt(),buffer.readUUID(),ForgeRegistries.ITEMS.getValue(buffer.readResourceLocation()),buffer.readByte());}
  static void handle(Packet value,Supplier<NetworkEvent.Context> context){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientRootItems1001.toolBroken(value));context.get().setPacketHandled(true);}
 }
 private SourceToolBreakTransport1058(){}
}
