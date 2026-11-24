package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
/** Source packet85 reversal updates acceleration as well as velocity on tracking clients. */
public record SourceFireballReversal058(int entity,int owner,double x,double y,double z) {
 private static SimpleChannel channel;
 public static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceFireballReversal058.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceFireballReversal058::write).decoder(SourceFireballReversal058::read).consumerMainThread(SourceFireballReversal058::handle).add();}
 static void send(SourceLargeFireball actor,Entity owner,Vec3 motion){channel.send(PacketDistributor.TRACKING_ENTITY.with(()->actor),new SourceFireballReversal058(actor.getId(),owner.getId(),motion.x,motion.y,motion.z));}
 private static void write(SourceFireballReversal058 v,FriendlyByteBuf b){b.writeVarInt(v.entity);b.writeVarInt(v.owner);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
 private static SourceFireballReversal058 read(FriendlyByteBuf b){return new SourceFireballReversal058(b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble());}
 private static void handle(SourceFireballReversal058 v,Supplier<NetworkEvent.Context> context){var level=net.minecraft.client.Minecraft.getInstance().level;if(level!=null&&level.getEntity(v.entity) instanceof SourceLargeFireball actor)actor.applyReversal(level.getEntity(v.owner),new Vec3(v.x,v.y,v.z));context.get().setPacketHandled(true);}
}
