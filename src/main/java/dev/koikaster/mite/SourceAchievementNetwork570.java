package dev.koikaster.mite;
import java.util.*;import java.util.function.Supplier;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.server.level.ServerPlayer;import net.minecraftforge.common.util.FakePlayer;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;import net.minecraftforge.network.*;import net.minecraftforge.network.simple.SimpleChannel;
/** No client-selected achievement ID, player ID, progress value or reward is accepted. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class SourceAchievementNetwork570 {
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Content.id("source_achievements"),()->"2","2"::equals,"2"::equals);
 private static final Map<ServerPlayer,Integer> QUERIES=new WeakHashMap<>();
 @SubscribeEvent public static void setup(FMLCommonSetupEvent event){CHANNEL.messageBuilder(Request.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Request::write).decoder(Request::read).consumerMainThread(Request::handle).add();CHANNEL.messageBuilder(Snapshot.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Snapshot::write).decoder(Snapshot::read).consumerMainThread(Snapshot::handle).add();}
 public static void request(boolean inventoryOpened){CHANNEL.sendToServer(new Request(inventoryOpened));}
 public static void send(ServerPlayer player){if(player.connection==null||player instanceof FakePlayer)return;CHANNEL.send(PacketDistributor.PLAYER.with(()->player),snapshot(player));}
 public static Snapshot snapshot(ServerPlayer player){
  var level=player.serverLevel();
  var world=SourceAchievementWorld561.get(level);
  boolean history=SourceAchievementEra570.get(level).completeHistory;
  boolean r196=WorldContentMode.r196(level);
  boolean extras=WorldContentMode.rebuilt(level);
  int[] own=Arrays.stream(SourceAchievementState561.completed(PlayerState.data(player)))
   .filter(id->SourceAchievementPolicy567.active(id,r196,extras)).toArray();
  var rows=new ArrayList<First>();
  world.records().forEach((id,record)->{
   if(SourceAchievementPolicy567.active(id,r196,extras))rows.add(new First(id,history?record.username():"",history?record.day():0));
  });
  return new Snapshot(player.getUUID(),own,rows);
 }
 public static void clear(ServerPlayer player){QUERIES.remove(player);}
 public record Request(boolean inventoryOpened){
  static void write(Request p,FriendlyByteBuf b){b.writeBoolean(p.inventoryOpened);}static Request read(FriendlyByteBuf b){return new Request(b.readBoolean());}
  static void handle(Request p,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null){boolean granted=p.inventoryOpened&&player.containerMenu==player.inventoryMenu&&SourceAchievementService570.grant(player,"openInventory");if(!granted){var last=QUERIES.get(player);if(last==null||player.tickCount<last||player.tickCount-last>=20){QUERIES.put(player,player.tickCount);send(player);}}}context.get().setPacketHandled(true);}
 }
 public record First(int id,String username,int day){public First{if(!SourceAchievementPolicy567.active(id)||username==null||username.length()>64)throw new IllegalArgumentException("Source first-completer record");}}
 public record Snapshot(UUID player,int[] own,List<First> world){
  public Snapshot{Objects.requireNonNull(player);own=own.clone();world=List.copyOf(world);if(own.length>SourceAchievementPolicy567.MAX_ACTIVE||world.size()>SourceAchievementPolicy567.MAX_ACTIVE)throw new IllegalArgumentException("Source achievement roster bound");var ids=new HashSet<Integer>();for(int id:own)if(!SourceAchievementPolicy567.active(id)||!ids.add(id))throw new IllegalArgumentException("Source personal achievement ID");ids.clear();for(var row:world)if(!ids.add(row.id()))throw new IllegalArgumentException("Duplicate source world record");}
  @Override public int[] own(){return own.clone();}
  static void write(Snapshot p,FriendlyByteBuf b){b.writeUUID(p.player);b.writeByte(p.own.length);for(int id:p.own)b.writeVarInt(id);b.writeByte(p.world.size());for(var r:p.world){b.writeVarInt(r.id);b.writeUtf(r.username,64);b.writeInt(r.day);}}
  static Snapshot read(FriendlyByteBuf b){var uuid=b.readUUID();int n=b.readUnsignedByte();if(n>SourceAchievementPolicy567.MAX_ACTIVE)throw new io.netty.handler.codec.DecoderException("Source personal roster bound");int[] own=new int[n];for(int i=0;i<n;i++)own[i]=b.readVarInt();n=b.readUnsignedByte();if(n>SourceAchievementPolicy567.MAX_ACTIVE)throw new io.netty.handler.codec.DecoderException("Source world roster bound");var rows=new ArrayList<First>();for(int i=0;i<n;i++)rows.add(new First(b.readVarInt(),b.readUtf(64),b.readInt()));return new Snapshot(uuid,own,rows);}
  static void handle(Snapshot p,Supplier<NetworkEvent.Context> context){var c=context.get();ClientSourceAchievements570.receive(p,c.getNetworkManager());c.setPacketHandled(true);}
 }
 private SourceAchievementNetwork570(){}
}
