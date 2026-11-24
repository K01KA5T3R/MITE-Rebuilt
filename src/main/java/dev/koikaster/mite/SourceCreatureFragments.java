package dev.koikaster.mite;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.RegistryObject;
/** Original non-carryable iconcrack sprite; it has no recipe, tab or drop. */
public record SourceCreatureFragments(int entity) {
 public static final RegistryObject<Item> NETHER=Content.ITEMS.register("frags_netherspawn",FragItem::new);
 public static final class FragItem extends Item {FragItem(){super(new Properties());}}
 private static SimpleChannel channel;
 static void registerItems(){}
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(SourceCreatureFragments.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(SourceCreatureFragments::write).decoder(SourceCreatureFragments::read).consumerMainThread(SourceCreatureFragments::handle).add();}
 static void send(Entity entity){channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),new SourceCreatureFragments(entity.getId()));}
 private static void write(SourceCreatureFragments value,FriendlyByteBuf data){data.writeInt(value.entity);}
 private static SourceCreatureFragments read(FriendlyByteBuf data){return new SourceCreatureFragments(data.readInt());}
 private static void handle(SourceCreatureFragments value,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->SourceCreatureFragmentsClient.accept(value));context.get().setPacketHandled(true);}
}
