package dev.koikaster.mite;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** A separate, versioned channel leaves the existing base channel untouched.
 * Epoch and dimension bind the two source client signals to one sleep phase. */
public final class SourceSleepNetwork {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Content.id("sleep"),()->"1","1"::equals,"1"::equals);
 private static boolean registered;
 public static void init(){
  if(registered)return;registered=true;
  CHANNEL.messageBuilder(Phase.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder(Phase::write).decoder(Phase::read).consumerMainThread(Phase::handle).add();
  CHANNEL.messageBuilder(Signal.class,1,NetworkDirection.PLAY_TO_SERVER).encoder(Signal::write).decoder(Signal::read).consumerMainThread(Signal::handle).add();
 }
 public static void phase(ServerPlayer player,long epoch,SourceSleepControlRules.Phase phase){if(player.connection!=null)player.connection.send(CHANNEL.toVanillaPacket(new Phase(player.getId(),player.level().dimension().location(),epoch,phase),NetworkDirection.PLAY_TO_CLIENT));}
 public static void signal(long epoch,SourceSleepControlRules.Signal signal){if(signal!=SourceSleepControlRules.Signal.NONE)CHANNEL.sendToServer(new Signal(epoch,signal));}
 public record Phase(int entity,ResourceLocation dimension,long epoch,SourceSleepControlRules.Phase phase){
  static void write(Phase packet,FriendlyByteBuf buffer){buffer.writeVarInt(packet.entity);buffer.writeResourceLocation(packet.dimension);buffer.writeVarLong(packet.epoch);buffer.writeEnum(packet.phase);}
  static Phase read(FriendlyByteBuf buffer){return new Phase(buffer.readVarInt(),buffer.readResourceLocation(),buffer.readVarLong(),buffer.readEnum(SourceSleepControlRules.Phase.class));}
  static void handle(Phase packet,Supplier<NetworkEvent.Context> context){SourceSleepClient.receive(packet);context.get().setPacketHandled(true);}
 }
 public record Signal(long epoch,SourceSleepControlRules.Signal signal){
  static void write(Signal packet,FriendlyByteBuf buffer){buffer.writeVarLong(packet.epoch);buffer.writeEnum(packet.signal);}
  static Signal read(FriendlyByteBuf buffer){return new Signal(buffer.readVarLong(),buffer.readEnum(SourceSleepControlRules.Signal.class));}
  static void handle(Signal packet,Supplier<NetworkEvent.Context> context){var sender=context.get().getSender();if(sender!=null)SourceSleepServer.signal(sender,packet.epoch,packet.signal);context.get().setPacketHandled(true);}
 }
 private SourceSleepNetwork(){}
}
