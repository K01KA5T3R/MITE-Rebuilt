package dev.koikaster.mite;
import java.util.function.Supplier;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.resources.ResourceLocation;import net.minecraft.server.level.ServerPlayer;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.fml.DistExecutor;import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;
public final class LocalWeatherTransport1981 {
 private static SimpleChannel channel;
 public static void register(SimpleChannel value){channel=value;value.messageBuilder(Snapshot.class,42,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::write).decoder(Snapshot::read).consumerMainThread(Snapshot::handle).add();}
 public static void send(ServerPlayer player,LocalWeather1981.State state){channel.send(PacketDistributor.PLAYER.with(()->player),new Snapshot(player.level().dimension().location(),state.kind().ordinal(),state.strength()));}
 public record Snapshot(ResourceLocation dimension,int kind,float strength){
  public Snapshot{if(kind<0||kind>=LocalWeather1981.Kind.values().length||!Float.isFinite(strength))throw new IllegalArgumentException("Invalid regional weather");strength=Math.max(0,Math.min(1,strength));}
  static void write(Snapshot value,FriendlyByteBuf buffer){buffer.writeResourceLocation(value.dimension);buffer.writeByte(value.kind);buffer.writeFloat(value.strength);}
  static Snapshot read(FriendlyByteBuf buffer){return new Snapshot(buffer.readResourceLocation(),buffer.readUnsignedByte(),buffer.readFloat());}
  static void handle(Snapshot value,Supplier<NetworkEvent.Context> context){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientLocalWeather1981.receive(value));context.get().setPacketHandled(true);}
 }
 private LocalWeatherTransport1981(){}
}
