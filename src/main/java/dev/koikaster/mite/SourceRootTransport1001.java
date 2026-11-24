package dev.koikaster.mite;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
/** Explicit source broken-item identity and a server-authoritative inventory-move intent. */
public final class SourceRootTransport1001 {
 private static SimpleChannel channel;
 private static final AtomicLong nextToken=new AtomicLong();
 private record Pending(long token,int slot,ItemStack previous,Object dimension,long until){}
 private static final Map<ServerPlayer,Pending> pending=Collections.synchronizedMap(new WeakHashMap<>());
 public static void register(SimpleChannel value,int first){
  channel=value;
  value.messageBuilder(Broken.class,first,NetworkDirection.PLAY_TO_CLIENT).encoder(Broken::write).decoder(Broken::read).consumerMainThread(Broken::handle).add();
  value.messageBuilder(Notice.class,first+1,NetworkDirection.PLAY_TO_CLIENT).encoder(Notice::write).decoder(Notice::read).consumerMainThread(Notice::handle).add();
  value.messageBuilder(Move.class,first+2,NetworkDirection.PLAY_TO_SERVER).encoder(Move::write).decoder(Move::read).consumerMainThread(Move::handle).add();
 }
 public static void broken(Player player,Item item,int slot){
  if(player instanceof ServerPlayer server&&server.getGameProfile().getName()!=null){int kind=item==Items.STICK?1:item==Items.BONE?2:0;if(kind!=0)channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),new Broken(player.getId(),player.getUUID(),kind,slot));}
 }
 public static void beforeLastHeld(Player player,ItemStack held){
  if(!(player instanceof ServerPlayer server)||held.isEmpty())return;
  long token=nextToken.incrementAndGet();int slot=player.getInventory().selected;
  pending.put(server,new Pending(token,slot,held.copyWithCount(1),server.level().dimension(),System.currentTimeMillis()+10_000));
  channel.send(PacketDistributor.PLAYER.with(()->server),new Notice(token,slot,held));
 }
 public static void requestMove(long token,int from,int selected){channel.sendToServer(new Move(token,from,selected));}
 public static void applyMove(ServerPlayer player,Move move){
  Pending p=pending.get(player);boolean accepted=false;
  if(p!=null&&p.token==move.token){
   pending.remove(player);
   var inventory=player.getInventory();
   if(!player.isRemoved()&&player.isAlive()&&!player.isSpectator()&&player.containerMenu==player.inventoryMenu&&p.dimension.equals(player.level().dimension())&&System.currentTimeMillis()<=p.until&&move.selected==p.slot&&inventory.selected==p.slot&&move.from>=9&&move.from<36&&inventory.getItem(p.slot).isEmpty()){
    int chosen=SourceRestockRules997.choose(new SourceRestockRules997.Scores(){public boolean previousKnown(){return true;}public int selected(){return p.slot;}public int similarity(int slot){return SourceStockIdentity1026.score(p.previous,inventory.getItem(slot));}});
    if(chosen==move.from){var stack=inventory.getItem(chosen);inventory.setItem(p.slot,stack);inventory.setItem(chosen,ItemStack.EMPTY);inventory.setChanged();accepted=true;}
   }
  }
  // Both accepted moves and stale/rejected optimistic moves finish with the server's slots.
  player.inventoryMenu.sendAllDataToRemote();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.sendAllDataToRemote();
 }
 public record Broken(int entity,UUID uuid,int kind,int slot){
  public static void write(Broken p,FriendlyByteBuf b){b.writeInt(p.entity);b.writeUUID(p.uuid);b.writeByte(p.kind);b.writeByte(p.slot);}
  public static Broken read(FriendlyByteBuf b){return new Broken(b.readInt(),b.readUUID(),b.readUnsignedByte(),b.readByte());}
  static void handle(Broken p,Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientRootItems1001.broken(p));c.get().setPacketHandled(true);}
 }
 public record Notice(long token,int slot,ItemStack previous){
  public Notice {Objects.requireNonNull(previous);previous=previous.copyWithCount(1);}
  @Override public ItemStack previous(){return previous.copy();}
  public static void write(Notice p,FriendlyByteBuf b){b.writeLong(p.token);b.writeByte(p.slot);b.writeNbt(p.previous.save(new net.minecraft.nbt.CompoundTag()));}
  public static Notice read(FriendlyByteBuf b){long token=b.readLong();int slot=b.readByte();var tag=b.readNbt();return new Notice(token,slot,tag==null?ItemStack.EMPTY:ItemStack.of(tag));}
  static void handle(Notice p,Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientRootItems1001.notice(p));c.get().setPacketHandled(true);}
 }
 public record Move(long token,int from,int selected){
  public static void write(Move p,FriendlyByteBuf b){b.writeLong(p.token);b.writeByte(p.from);b.writeByte(p.selected);}
  public static Move read(FriendlyByteBuf b){return new Move(b.readLong(),b.readByte(),b.readByte());}
  static void handle(Move p,Supplier<NetworkEvent.Context> c){var player=c.get().getSender();if(player!=null)applyMove(player,p);c.get().setPacketHandled(true);}
 }
 private SourceRootTransport1001(){}
}
