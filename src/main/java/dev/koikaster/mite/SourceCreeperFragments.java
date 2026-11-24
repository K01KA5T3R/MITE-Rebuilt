package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.RegistryObject;
/** Independent new Creeper FX envelope; Netherspawn's completed channel stays unchanged. */
public record SourceCreeperFragments(int entity,boolean infernal) {
 public static final RegistryObject<Item> NORMAL=Content.ITEMS.register("frags_creeper",SourceCreatureFragments.FragItem::new),INFERNAL=Content.ITEMS.register("frags_infernal_creeper",SourceCreatureFragments.FragItem::new);
 private static SimpleChannel channel;
 static void registerItems(){}
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceCreeperFragments.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceCreeperFragments::write).decoder(SourceCreeperFragments::read).consumerMainThread(SourceCreeperFragments::handle).add();}
 static void send(SourceCreeper actor){channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->actor),new SourceCreeperFragments(actor.getId(),actor.infernal()));}
 private static void write(SourceCreeperFragments value,FriendlyByteBuf data){data.writeInt(value.entity);data.writeBoolean(value.infernal);}
 private static SourceCreeperFragments read(FriendlyByteBuf data){return new SourceCreeperFragments(data.readInt(),data.readBoolean());}
 private static void handle(SourceCreeperFragments value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceCreeperFragmentsClient.accept(value));context.get().setPacketHandled(true);}
}
