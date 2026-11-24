package dev.koikaster.mite;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AdvancedEnchantNetwork {
 private static SimpleChannel channel;
 public record Request(int menu,long session,int revision,ResourceLocation enchantment,int rank,boolean confirm){}
 public record Snapshot(int menu,long session,int revision,int shelves,int maximumTier,int xp,ResourceLocation selected,int rank,AdvancedEnchanting.Quote quote,List<AdvancedEnchanting.Row> rows){public Snapshot{rows=List.copyOf(rows);}}
 public static void register(SimpleChannel c){channel=c;c.messageBuilder(Request.class,25,NetworkDirection.PLAY_TO_SERVER).encoder(AdvancedEnchantNetwork::writeRequest).decoder(AdvancedEnchantNetwork::readRequest).consumerMainThread(AdvancedEnchantNetwork::handleRequest).add();c.messageBuilder(Snapshot.class,26,NetworkDirection.PLAY_TO_CLIENT).encoder(AdvancedEnchantNetwork::writeSnapshot).decoder(AdvancedEnchantNetwork::readSnapshot).consumerMainThread(AdvancedEnchantNetwork::handleSnapshot).add();}
 public static void send(ServerPlayer player,Snapshot snapshot){channel.send(PacketDistributor.PLAYER.with(()->player),snapshot);}
 public static void request(Snapshot snapshot,ResourceLocation id,int rank,boolean confirm){if(snapshot!=null&&id!=null)channel.sendToServer(new Request(snapshot.menu(),snapshot.session(),snapshot.revision(),id,rank,confirm));}
 static void writeRequest(Request p,FriendlyByteBuf b){b.writeVarInt(p.menu());b.writeLong(p.session());b.writeInt(p.revision());b.writeResourceLocation(p.enchantment());b.writeVarInt(p.rank());b.writeBoolean(p.confirm());}
 static Request readRequest(FriendlyByteBuf b){return new Request(b.readVarInt(),b.readLong(),b.readInt(),b.readResourceLocation(),b.readVarInt(),b.readBoolean());}
 private static void handleRequest(Request p,Supplier<NetworkEvent.Context> ctx){var player=ctx.get().getSender();if(player!=null&&player.containerMenu instanceof AdvancedEnchantMenu menu)menu.request(player,p);ctx.get().setPacketHandled(true);}
 static void writeSnapshot(Snapshot p,FriendlyByteBuf b){b.writeVarInt(p.menu());b.writeLong(p.session());b.writeInt(p.revision());b.writeVarInt(p.shelves());b.writeVarInt(p.maximumTier());b.writeInt(p.xp());b.writeBoolean(p.selected()!=null);if(p.selected()!=null)b.writeResourceLocation(p.selected());b.writeVarInt(p.rank());b.writeVarInt(p.quote().levels());b.writeInt(p.quote().points());b.writeInt(p.quote().ordinaryFloor());b.writeEnum(p.quote().reason());b.writeVarInt(p.rows().size());for(var row:p.rows()){b.writeResourceLocation(row.id());b.writeVarInt(row.minimum());b.writeVarInt(row.maximum());b.writeVarInt(row.current());b.writeEnum(row.reason());}}
 static Snapshot readSnapshot(FriendlyByteBuf b){int menu=b.readVarInt();long session=b.readLong();int revision=b.readInt(),shelves=b.readVarInt(),max=b.readVarInt(),xp=b.readInt();var selected=b.readBoolean()?b.readResourceLocation():null;int rank=b.readVarInt();var quote=new AdvancedEnchanting.Quote(b.readVarInt(),b.readInt(),b.readInt(),b.readEnum(AdvancedEnchanting.Reason.class));int count=b.readVarInt();if(count<0||count>4096)throw new io.netty.handler.codec.DecoderException("Advanced enchanting row limit");var rows=new ArrayList<AdvancedEnchanting.Row>();for(int i=0;i<count;i++)rows.add(new AdvancedEnchanting.Row(b.readResourceLocation(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readEnum(AdvancedEnchanting.Reason.class)));return new Snapshot(menu,session,revision,shelves,max,xp,selected,rank,quote,rows);}
 private static void handleSnapshot(Snapshot p,Supplier<NetworkEvent.Context> ctx){net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->AdvancedEnchantScreen.accept(p));ctx.get().setPacketHandled(true);}
 private AdvancedEnchantNetwork(){}
}
