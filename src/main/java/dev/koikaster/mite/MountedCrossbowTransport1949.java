package dev.koikaster.mite;
import java.util.function.Supplier;import net.minecraft.network.FriendlyByteBuf;import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;
/** Packet ownership is the current server-side rider, not a client-supplied block location. */
public final class MountedCrossbowTransport1949 {
 private static SimpleChannel channel;
 public static void register(SimpleChannel value){channel=value;value.messageBuilder(Input.class,41,NetworkDirection.PLAY_TO_SERVER).encoder(Input::write).decoder(Input::read).consumerMainThread(Input::handle).add();}
 public static void send(int entity,boolean held){channel.sendToServer(new Input(entity,held));}
 public record Input(int entity,boolean held){
  static void write(Input value,FriendlyByteBuf buffer){buffer.writeVarInt(value.entity);buffer.writeBoolean(value.held);}
  static Input read(FriendlyByteBuf buffer){return new Input(buffer.readVarInt(),buffer.readBoolean());}
  static void handle(Input value,Supplier<NetworkEvent.Context> context){var sender=context.get().getSender();if(sender!=null&&sender.getVehicle() instanceof MountedCrossbow1949.Seat seat&&seat.getId()==value.entity)seat.input(sender,value.held);context.get().setPacketHandled(true);}
 }
 private MountedCrossbowTransport1949(){}
}
