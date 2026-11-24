package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
/** Source packet61's additive, undampened player impulse. */
public record SourceExplosionImpulse(float x,float y,float z) {
 private static SimpleChannel channel;
 public static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceExplosionImpulse.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceExplosionImpulse::write).decoder(SourceExplosionImpulse::read).consumerMainThread(SourceExplosionImpulse::handle).add();}
 static void send(ServerPlayer player,Vec3 impulse){channel.send(PacketDistributor.PLAYER.with(()->player),new SourceExplosionImpulse((float)impulse.x,(float)impulse.y,(float)impulse.z));}
 private static void write(SourceExplosionImpulse value,FriendlyByteBuf data){data.writeFloat(value.x);data.writeFloat(value.y);data.writeFloat(value.z);}
 private static SourceExplosionImpulse read(FriendlyByteBuf data){return new SourceExplosionImpulse(data.readFloat(),data.readFloat(),data.readFloat());}
 private static void handle(SourceExplosionImpulse value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->{var player=net.minecraft.client.Minecraft.getInstance().player;if(player!=null)player.setDeltaMovement(player.getDeltaMovement().add(value.x,value.y,value.z));});context.get().setPacketHandled(true);}
}
