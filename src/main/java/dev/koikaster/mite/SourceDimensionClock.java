package dev.koikaster.mite;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** WorldInfoShared's four independent clocks, with native97A administrative
 * calendar controls and human90B empty-server pause. No player configuration. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceDimensionClock {
 static final class State extends SavedData {
  final long[] game=new long[4],day=new long[4];
  boolean underworldVisited,netherVisited;
  private Presence presence;
  private int presenceTick;
  State(long elapsed,long calendar){Arrays.fill(game,elapsed);Arrays.fill(day,calendar);setDirty();}
  static State load(CompoundTag tag,long elapsed,long calendar){var value=new State(elapsed,calendar);var ages=tag.getLongArray("GameTimes");var dates=tag.getLongArray("DayTimes");if(ages.length==4)for(int i=0;i<4;i++)value.game[i]=Math.max(0,ages[i]);if(dates.length==4)for(int i=0;i<4;i++)value.day[i]=Math.max(0,dates[i]);value.underworldVisited=tag.getBoolean("UnderworldVisited");value.netherVisited=tag.getBoolean("NetherVisited");return value;}
  @Override public CompoundTag save(CompoundTag tag){tag.putInt("Version",1);tag.putLongArray("GameTimes",game);tag.putLongArray("DayTimes",day);tag.putBoolean("UnderworldVisited",underworldVisited);tag.putBoolean("NetherVisited",netherVisited);return tag;}
  Presence presence(MinecraftServer server){int tick=server.getTickCount();if(presence==null||presenceTick!=tick){presence=new Presence(server,this);presenceTick=tick;}return presence;}
  void invalidate(){presence=null;}
 }
 private static final class Presence implements SourceDimensionClockRules.Host {
  final boolean dedicated,connected,nonGhost;
  final int mask;
  final State state;
  Presence(MinecraftServer server,State state){this.state=state;dedicated=server.isDedicatedServer();var players=server.getPlayerList().getPlayers();connected=!players.isEmpty();int bits=0;boolean ordinaryPresence=false;for(var player:players)if(!player.isSpectator()){ordinaryPresence=true;int d=dimension(player.level().dimension());bits|=1<<SourceDimensionClockRules.index(d==Integer.MIN_VALUE?0:d);}mask=bits;nonGhost=ordinaryPresence;}
  public boolean dedicated(){return dedicated;}public boolean players(int dimension){return (mask&(1<<SourceDimensionClockRules.index(dimension)))!=0;}public boolean anyConnected(){return connected;}public boolean onlyGhosts(){return connected&&!nonGhost;}public boolean nonGhostConnected(){return nonGhost;}public boolean underworldVisited(){return state.underworldVisited;}public boolean netherVisited(){return state.netherVisited;}
 }
 static State state(MinecraftServer server){synchronized(server){var world=server.overworld();if(world==null)return null;var master=server.getWorldData().overworldData();return world.getDataStorage().computeIfAbsent(tag->State.load(tag,master.getGameTime(),master.getDayTime()),()->new State(master.getGameTime(),master.getDayTime()),"mite_dimension_clocks");}}
 static int dimension(ResourceKey<Level> key){return key==Level.OVERWORLD?0:key==Level.NETHER?-1:key==Level.END?1:key==WorldEvents.UNDERWORLD?-2:Integer.MIN_VALUE;}
 public static ServerLevelData wrap(Object rawServer,Object rawData,Object rawDimension){var data=(ServerLevelData)rawData;@SuppressWarnings("unchecked") var key=(ResourceKey<Level>)rawDimension;int d=dimension(key);return d==Integer.MIN_VALUE||data instanceof SourceDimensionLevelData?data:new SourceDimensionLevelData((MinecraftServer)rawServer,data,d);}
 private static SourceDimensionLevelData data(ServerLevel level){return level.getLevelData() instanceof SourceDimensionLevelData data?data:null;}
 public static boolean tick(Object raw){var level=(ServerLevel)raw;var data=data(level);if(data==null)return false;var state=data.clocks();if(state==null)return true;if(SourceDimensionClockRules.progress(state.presence(level.getServer()),data.dimension)){long next=data.getGameTime()+1;data.setGameTime(next);if(data.dimension==0)data.getScheduledEvents().tick(level.getServer(),next);if(level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))data.setDayTime(data.getDayTime()+1);}return true;}
 public static boolean chunkAllowed(Object raw){var level=(ServerLevel)raw;var data=data(level);return data==null||data.clocks()==null||SourceDimensionClockRules.progress(data.clocks().presence(level.getServer()),data.dimension);}
 private static boolean randomAllowed(ServerLevel level){var data=data(level);return data==null||data.clocks()==null||SourceDimensionClockRules.randomTicks(data.clocks().presence(level.getServer()),data.dimension);}
 public static void blockRandom(Object rawState,Object rawLevel,Object rawPos,Object rawRandom){var level=(ServerLevel)rawLevel;if(randomAllowed(level))((BlockState)rawState).randomTick(level,(BlockPos)rawPos,(RandomSource)rawRandom);}
 public static void fluidRandom(Object rawState,Object rawLevel,Object rawPos,Object rawRandom){var level=(ServerLevel)rawLevel;if(randomAllowed(level))((FluidState)rawState).randomTick(level,(BlockPos)rawPos,(RandomSource)rawRandom);}
 public static boolean daylight(boolean original,Object raw){return original&&chunkAllowed(raw);}
 private static void invalidate(MinecraftServer server){var data=state(server);if(data!=null)data.invalidate();}
 private static void entered(ServerPlayer player){var state=state(player.getServer());if(state==null)return;int d=dimension(player.level().dimension());if(d==-2&&!state.underworldVisited){state.underworldVisited=true;state.setDirty();}if(d==-1&&!state.netherVisited){state.netherVisited=true;state.setDirty();}state.invalidate();if(data(player.serverLevel())!=null)player.connection.send(new ClientboundSetTimePacket(player.serverLevel().getGameTime(),player.serverLevel().getDayTime(),daylight(player.serverLevel().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),player.serverLevel())));}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)entered(player);}
 @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)entered(player);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)invalidate(player.getServer());}
 @SubscribeEvent public static void gameMode(PlayerEvent.PlayerChangeGameModeEvent event){if(event.getEntity() instanceof ServerPlayer player)invalidate(player.getServer());}
 private SourceDimensionClock(){}
}
