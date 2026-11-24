package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** jo/hn -> Packet80(eo) -> bln: outdoors receivers within64, old eighth-block
 * position and63-step pitch quantization. Source outdoors side-opening geometry
 * is represented by the existing modern sky adapter, explicitly tracked inR16. */
public record SourceWitchCackle(int x,int y,int z,float volume,int pitch) {
 private static SimpleChannel channel;
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceWitchCackle.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceWitchCackle::write).decoder(SourceWitchCackle::read).consumerMainThread(SourceWitchCackle::handle).add();}
 static void send(SourceWitch actor,float volume,float pitch){
  if(actor.isSilent()||!(actor.level() instanceof ServerLevel level))return;
  var packet=new SourceWitchCackle((int)(actor.getX()*8),(int)(actor.getY()*8),(int)(actor.getZ()*8),volume,Math.max(0,Math.min(255,(int)(pitch*63f))));
  for(var player:level.players())if(player.distanceToSqr(actor)<4096&&level.canSeeSky(player.blockPosition()))channel.send(PacketDistributor.PLAYER.with(()->player),packet);
 }
 private static void write(SourceWitchCackle p,FriendlyByteBuf b){b.writeInt(p.x);b.writeInt(p.y);b.writeInt(p.z);b.writeFloat(p.volume);b.writeByte(p.pitch);}
 private static SourceWitchCackle read(FriendlyByteBuf b){return new SourceWitchCackle(b.readInt(),b.readInt(),b.readInt(),b.readFloat(),b.readUnsignedByte());}
 private static void handle(SourceWitchCackle p,Supplier<NetworkEvent.Context> context){SourceWitchClientAudio.accept(p);context.get().setPacketHandled(true);}
}
