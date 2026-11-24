package dev.koikaster.mite;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** aps initiate -> bcw start -> bdi twenty ticks -> ka execute -> finished.
 * No target coordinates or XP charge are accepted from the client. The pending
 * operation belongs to the actual server player object and its source world. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourcePortalTransition {
 private record Pending(UUID token,ServerLevel level,BlockPos entrance,Vec3 destination,long started,boolean rune,BiomePortalStone biome,boolean matchingBiome){}
 private static final Map<ServerPlayer,Pending> PENDING=new WeakHashMap<>();
 private static final int BIOME_RECHECK_INTERVAL = 4;
 private static SimpleChannel channel;
 public static void register(SimpleChannel value,int firstId){
  channel=value;
  value.messageBuilder(Start.class,firstId,NetworkDirection.PLAY_TO_CLIENT).encoder(Start::write).decoder(Start::read).consumerMainThread(Start::handle).add();
  value.messageBuilder(Execute.class,firstId+1,NetworkDirection.PLAY_TO_SERVER).encoder(Execute::write).decoder(Execute::read).consumerMainThread(Execute::handle).add();
  value.messageBuilder(Finished.class,firstId+2,NetworkDirection.PLAY_TO_CLIENT).encoder(Finished::write).decoder(Finished::read).consumerMainThread(Finished::handle).add();
 }
 public static boolean pending(ServerPlayer player){
  var pending=PENDING.get(player);if(pending==null)return false;
  if(pending.level()!=player.serverLevel()||pending.level().getGameTime()-pending.started()>200||!player.isAlive()
      ||routeLocked(pending.level(),pending.entrance(),pending.destination())
      ||pending.biome()!=null&&!SourceBiomePortal.stillInside(player,pending.entrance(),pending.biome())){
   PENDING.remove(player);
   if(pending.biome()!=null&&player.isAlive())player.setPortalCooldown(20);
   sendFinished(player,pending.token());return false;
  }
  return true;
 }
 public static void beginSpawn(ServerPlayer player,BlockPos entrance){
  if(pending(player)||player.isOnPortalCooldown()||BossArenaPortals.blocked(player.serverLevel(),entrance))return;
  var level=player.serverLevel();var spawn=level.getSharedSpawnPos();level.getChunkAt(spawn);
  int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,spawn.getX(),spawn.getZ())-1;
  // Original getTopSolidOrLiquidBlockMITE(false): include leaves and liquids.
  while(y>level.getMinBuildHeight()){
   var pos=new BlockPos(spawn.getX(),y,spawn.getZ());var state=level.getBlockState(pos);
   if(SourcePhaseNativeWorld.solid(state)||!state.getFluidState().isEmpty())break;--y;
  }
  var destination=new Vec3(spawn.getX()+.5,y+1.1,spawn.getZ()+.5);
  begin(player,destination,entrance,false,null,false);
 }
 public static void beginRune(ServerPlayer player,Vec3 destination,BlockPos entrance){
  begin(player,destination,entrance,true,null,false);
 }
 static void beginBiome(ServerPlayer player,Vec3 destination,BlockPos entrance,BiomePortalStone biome,boolean matchingBiome){
  begin(player,destination,entrance,false,biome,matchingBiome);
 }
 private static void begin(ServerPlayer player,Vec3 destination,BlockPos entrance,boolean rune,BiomePortalStone biome,boolean matchingBiome){
  if(pending(player))return;
  var level=player.serverLevel();
  if(routeLocked(level,entrance,destination))return;
  var token=UUID.randomUUID();
  PENDING.put(player,new Pending(token,level,entrance.immutable(),destination,level.getGameTime(),rune,biome,matchingBiome));
  channel.send(PacketDistributor.PLAYER.with(()->player),new Start(token));
 }
 private static boolean routeLocked(ServerLevel level,BlockPos entrance,Vec3 destination){
  return BossArenaPortals.blocked(level,BlockPos.containing(destination))||BossArenaPortals.blockedNetherEntrance(level,entrance);
 }
 static void cancelLocked(ServerLevel level){
  for(var entries=PENDING.entrySet().iterator();entries.hasNext();){
   var entry=entries.next();
   var pending=entry.getValue();
   if(pending.level()!=level||!routeLocked(level,pending.entrance(),pending.destination()))continue;
   var player=entry.getKey();
   entries.remove();
   if(player!=null&&player.connection!=null&&player.connection.isAcceptingMessages())sendFinished(player,pending.token());
  }
 }
 @SubscribeEvent
 public static void recheckBiomeLanding(TickEvent.PlayerTickEvent event) {
  if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
  var active = PENDING.get(player);
  if (active == null || active.biome() == null) return;
  if (active.level() == player.serverLevel() && active.level().getGameTime() % BIOME_RECHECK_INTERVAL != 0) return;
  if (pending(player)) refreshBiomeLanding(player, active);
 }

 private static Pending refreshBiomeLanding(ServerPlayer player, Pending current) {
  if (SourceBiomePortal.safeLanding(player, current.matchingBiome() ? current.biome() : null, current.destination())) {
   return current;
  }
  var replacement = SourceBiomePortal.preparedReplacement(player, current.entrance(), current.biome());
  if (replacement != null) {
   // Start carries a token only. Keep the same client timer while the server changes the coordinates.
   var updated = new Pending(current.token(), current.level(), current.entrance(), replacement,
       current.started(), current.rune(), current.biome(), true);
   PENDING.put(player, updated);
   return updated;
  }

  PENDING.remove(player);
  sendFinished(player, current.token());
  SourceBiomePortal.retryLanding(player, current.entrance(), current.biome());
  return null;
 }

 public static void execute(ServerPlayer player, UUID token) {
  if (!pending(player)) return;
  var active = PENDING.get(player);
  if (!active.token().equals(token)) return;
  if (active.biome() != null) {
   active = refreshBiomeLanding(player, active);
   if (active == null) return;
  }

  PENDING.remove(player); // Consume before teleport; a duplicate cannot execute twice.
  var at = active.destination();
  if (active.biome() != null) SourceBiomePortal.loadArrival(player, at);
  player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot());
  player.connection.resetPosition();
  player.fallDistance = 0;
  if (active.rune()) SourceRuneGate.award(player);
  else player.setPortalCooldown();
  sendFinished(player, token);
 }
 private static void sendFinished(ServerPlayer player,UUID token){channel.send(PacketDistributor.PLAYER.with(()->player),new Finished(token));}
 static void request(UUID token){channel.sendToServer(new Execute(token));}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)PENDING.remove(player);}
 public record Start(UUID token){
  static void write(Start value,FriendlyByteBuf buffer){buffer.writeUUID(value.token());}
  static Start read(FriendlyByteBuf buffer){return new Start(buffer.readUUID());}
  static void handle(Start value,Supplier<NetworkEvent.Context> context){SourcePortalTransitionClient.start(value.token());context.get().setPacketHandled(true);}
 }
 public record Execute(UUID token){
  static void write(Execute value,FriendlyByteBuf buffer){buffer.writeUUID(value.token());}
  static Execute read(FriendlyByteBuf buffer){return new Execute(buffer.readUUID());}
  static void handle(Execute value,Supplier<NetworkEvent.Context> context){var player=context.get().getSender();if(player!=null)execute(player,value.token());context.get().setPacketHandled(true);}
 }
 public record Finished(UUID token){
  static void write(Finished value,FriendlyByteBuf buffer){buffer.writeUUID(value.token());}
  static Finished read(FriendlyByteBuf buffer){return new Finished(buffer.readUUID());}
  static void handle(Finished value,Supplier<NetworkEvent.Context> context){SourcePortalTransitionClient.finished(value.token());context.get().setPacketHandled(true);}
 }
 private SourcePortalTransition(){}
}
