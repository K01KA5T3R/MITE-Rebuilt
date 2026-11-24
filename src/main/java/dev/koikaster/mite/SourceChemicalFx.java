package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
/** New039 bcw steam families. Cosmetic RNG stays on the client as in R195. */
public record SourceChemicalFx(byte kind,int entity,long block) {
 static final byte BLOCK_STEAM=0,BLOCK_STEAM_PARTICLES=1,BLOCK_SMOKE_STEAM=2,ENTITY_STEAM=3,ENTITY_SMOKE_STEAM=4,ENTITY_VANISH=5;
 private static SimpleChannel channel;
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceChemicalFx.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceChemicalFx::write).decoder(SourceChemicalFx::read).consumerMainThread(SourceChemicalFx::handle).add();}
 static void block(ServerLevel level,BlockPos pos,byte kind){channel.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,64,level.dimension())),new SourceChemicalFx(kind,-1,pos.asLong()));}
 static void entity(Entity actor,byte kind){channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->actor),new SourceChemicalFx(kind,actor.getId(),0));}
 private static void write(SourceChemicalFx packet,FriendlyByteBuf buffer){buffer.writeByte(packet.kind);buffer.writeInt(packet.entity);buffer.writeLong(packet.block);}
 private static SourceChemicalFx read(FriendlyByteBuf buffer){byte kind=buffer.readByte();if(kind<0||kind>5)throw new io.netty.handler.codec.DecoderException("Source chemical FX kind");return new SourceChemicalFx(kind,buffer.readInt(),buffer.readLong());}
 private static void handle(SourceChemicalFx packet,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceChemicalClientFx.accept(packet));context.get().setPacketHandled(true);}
}
