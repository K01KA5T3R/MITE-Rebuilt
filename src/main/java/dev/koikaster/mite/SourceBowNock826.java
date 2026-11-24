package dev.koikaster.mite;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArrowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

/** wp and Packet85 nocked_arrow: source state belongs to the player, not the item stack. */
public final class SourceBowNock826 {
 private static final Map<Player,Item> NOCKED=new com.google.common.collect.MapMaker().weakKeys().makeMap();
 private static SimpleChannel channel;
 static void register(SimpleChannel value,int id){channel=value;value.messageBuilder(State.class,id,NetworkDirection.PLAY_TO_CLIENT).encoder(State::write).decoder(State::read).consumerMainThread(State::handle).add();}
 public static Item get(Player player){return NOCKED.get(player);}
 public static void set(Player player,Item arrow){if(arrow==null)NOCKED.remove(player);else NOCKED.put(player,arrow);}
 public static void sync(Player player){
  Item arrow=get(player);var id=arrow==null?null:ForgeRegistries.ITEMS.getKey(arrow);
  if(!player.level().isClientSide&&id!=null)channel.send(PacketDistributor.TRACKING_ENTITY.with(()->player),new State(player.getId(),player.getUUID(),id));
 }
 public record State(int entity,UUID uuid,ResourceLocation arrow){
  public static void write(State state,FriendlyByteBuf data){data.writeVarInt(state.entity);data.writeUUID(state.uuid);data.writeResourceLocation(state.arrow);}
  public static State read(FriendlyByteBuf data){return new State(data.readVarInt(),data.readUUID(),data.readResourceLocation());}
  static void handle(State state,Supplier<NetworkEvent.Context> context){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->SourceBowClient826.accept(state));context.get().setPacketHandled(true);}
 }
 public static void apply(Player target,State state){
  if(!target.level().isClientSide||target.getId()!=state.entity||!target.getUUID().equals(state.uuid))return;
  var item=ForgeRegistries.ITEMS.getValue(state.arrow);if(item instanceof ArrowItem)set(target,item);
 }
 private SourceBowNock826(){}
}
