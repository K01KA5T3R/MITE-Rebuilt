package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
/** bcw EnumEntityFX.summoned: ten old explode particles, without a hiss. */
public record SourceSummonedFx(int entity) {
 static SimpleChannel channel;
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceSummonedFx.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceSummonedFx::write).decoder(SourceSummonedFx::read).consumerMainThread(SourceSummonedFx::handle).add();}
 static void send(Entity entity){channel.send(PacketDistributor.TRACKING_ENTITY.with(()->entity),new SourceSummonedFx(entity.getId()));}
 private static void write(SourceSummonedFx value,FriendlyByteBuf buffer){buffer.writeInt(value.entity);}
 private static SourceSummonedFx read(FriendlyByteBuf buffer){return new SourceSummonedFx(buffer.readInt());}
 private static void handle(SourceSummonedFx value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceSummonedClientEffects.spawn(value.entity));context.get().setPacketHandled(true);}
}
