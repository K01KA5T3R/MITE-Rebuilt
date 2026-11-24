package dev.koikaster.mite;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static dev.koikaster.mite.SourceSleepControlRules.*;

/** R195 server bed/consciousness owner. A world tick supplies the source cohort;
 * the literal client100/110 counter supplies only sleeping/fully-awake signals.
 * The staged historical-tick owner still must be integrated before release. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceSleepServer {
 private static final class Entry {Phase phase=Phase.FULLY_AWAKE;long epoch;net.minecraft.resources.ResourceKey<Level> dimension;}
 private static final Map<ServerPlayer,Entry> STATES=new WeakHashMap<>();
 private static final Map<ServerLevel,WorldHost> WORLDS=new IdentityHashMap<>();
 private static final AtomicLong EPOCH=new AtomicLong();
 public static boolean owns(Object raw){return raw instanceof Player player&&player.level().dimension()==Level.OVERWORLD;}
 public static boolean ownsWorld(Object raw){return raw instanceof Level level&&level.dimension()==Level.OVERWORLD;}
 public static boolean ownsBedEvent(Object raw){return raw instanceof PlayerSleepInBedEvent event&&owns(event.getEntity());}
 public static Phase phase(ServerPlayer player){var entry=STATES.get(player);return entry==null?Phase.FULLY_AWAKE:entry.phase;}
 static long epoch(ServerPlayer player){var entry=STATES.get(player);return entry==null?0:entry.epoch;}
 public static boolean asleep(Object raw){var player=(Player)raw;return player instanceof ServerPlayer server&&owns(player)?phase(server)==Phase.SLEEPING:player.isSleeping();}
 public static int timer(int original,Object raw){return raw instanceof Player player&&player.level().isClientSide&&owns(player)?SourceSleepClient.timer(original,raw):original;}
 private static void time(ServerPlayer player){var level=player.serverLevel();player.connection.send(new ClientboundSetTimePacket(level.getGameTime(),level.getDayTime(),SourceDimensionClock.daylight(level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT),level)));}
 private static void transition(ServerPlayer player,Phase target,boolean force){
  var entry=STATES.computeIfAbsent(player,p->new Entry());if(!force&&entry.phase==target)return;
  if(player.connection!=null&&(entry.phase==Phase.FALLING_ASLEEP||entry.phase==Phase.SLEEPING))time(player);
  entry.phase=target;entry.dimension=player.level().dimension();entry.epoch=EPOCH.incrementAndGet();SourceSleepNetwork.phase(player,entry.epoch,target);
 }
 public static void signal(ServerPlayer player,long epoch,Signal signal){
  var entry=STATES.get(player);if(entry==null||entry.epoch!=epoch||epoch<=0||!owns(player)||entry.dimension!=player.level().dimension()||!player.isSleeping())return;
  if(signal==Signal.SLEEPING&&entry.phase==Phase.FALLING_ASLEEP&&!OriginalCurseEffects.has(player,12,true))entry.phase=Phase.SLEEPING;
  else if(signal==Signal.FULLY_AWAKE&&entry.phase==Phase.WAKING_UP)entry.phase=Phase.FULLY_AWAKE;
 }
 public static Either<Player.BedSleepingProblem,Unit> tryBed(Object raw,Object rawPos){
  var player=(ServerPlayer)raw;var pos=(BlockPos)rawPos;var level=player.serverLevel();
  var eventResult=net.minecraftforge.event.ForgeEventFactory.onPlayerSleepInBed(player,Optional.of(pos));if(eventResult!=null)return Either.left(eventResult);
  if(player.isSleeping()||!player.isAlive()||!level.dimensionType().bedWorks()||!(level.getBlockState(pos).getBlock() instanceof BedBlock))return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
  if(SourceSleepBedThreat.nearest(level,pos)!=null)return deny(player,"bed_not_safe");
  if(SourceSpawnOutdoors.isOutdoors(level,pos))return deny(player,"bed_shelter");
  if(player.getFoodData().getFoodLevel()==0)return deny(player,"bed_too_hungry");
  if(!SourceTroopPlacement.passable(level,pos.above(),false))return deny(player,"bed_obstructed");
  if(player.hasEffect(MobEffects.POISON))return deny(player,"bed_poisoned");
  if(SourceSleepBedThreat.digging(level,pos))return deny(player,"bed_digging");
  player.setRespawnPosition(level.dimension(),pos,player.getYRot(),false,false);
  player.startSleeping(pos);level.updateSleepingPlayerList();transition(player,Phase.FULLY_AWAKE,true);
  return Either.right(Unit.INSTANCE);
 }
 private static Either<Player.BedSleepingProblem,Unit> deny(ServerPlayer player,String key){notice(player,key);return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);}
 private static void notice(ServerPlayer player,String key){player.displayClientMessage(Component.translatable("message.mite_reforged."+key),true);}
 public static boolean vanillaBlock(boolean original,Object raw){if(!(raw instanceof ServerLevel level)||!ownsWorld(level))return original;tick(level);return false;}
 public static void tick(ServerLevel level){
  var world=WORLDS.computeIfAbsent(level,WorldHost::new);var players=level.players();
  int percentage=level.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
  if(percentage!=100){
   var alive=players.stream().filter(p->!p.isSpectator()&&!(p.getHealth()<=0)).toList();
   int needed=Math.max(1,(int)Math.ceil(alive.size()*percentage/100.0));
   if(alive.stream().filter(p->world.nativeWorld.person(p).inBed()).count()<needed){world.cohort=players;wakeAllPlayersGently(world);return;}
   world.cohort=players.stream().filter(p->p.isSpectator()||p.getHealth()<=0||world.nativeWorld.person(p).inBed()).toList();
  }else world.cohort=players;
  SourceSleepControlRules.tick(world);
 }
 private static final class Participant implements SourceSleepControlRules.Player {
  final ServerPlayer player;final WorldHost world;Participant(ServerPlayer player,WorldHost world){this.player=player;this.world=world;}
  public boolean ghost(){return player.isSpectator();}public boolean excluded(){return false;}public float health(){return player.getHealth();}
  public boolean inBed(){return world.nativeWorld.person(player).inBed();}public boolean sleeping(){return SourceSleepServer.phase(player)==Phase.SLEEPING;}
  public Phase phase(){return SourceSleepServer.phase(player);}
  public void startFallingAsleep(){transition(player,Phase.FALLING_ASLEEP,false);}public void wakeGently(){transition(player,Phase.WAKING_UP,false);}
 }
 private static final class WorldHost implements SourceSleepControlRules.World {
  final ServerLevel level;final SourceSleepKernel kernel=new SourceSleepKernel();final SourceSleepNativeWorld nativeWorld;List<ServerPlayer> cohort=List.of();
  WorldHost(ServerLevel level){this.level=level;nativeWorld=new SourceSleepNativeWorld(level,p->phase(p)==Phase.SLEEPING,SourceSleepSpecies::accepts){@Override public List<? extends SourceSleepKernel.Sleeper> players(){return cohort.stream().map(this::person).toList();}};}
  public List<? extends SourceSleepControlRules.Player> players(){return cohort.stream().map(p->new Participant(p,this)).toList();}
  public boolean bloodMoon(){return SourceMoonCycle.blood(level.getDayTime(),false);}public boolean tournament(){return SourceTournament.mode(level.getServer())!=null;}public long dayTime(){return level.getDayTime();}public boolean daylightCycle(){return level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);}
  public void runSleepTicks(int ticks){
   kernel.runSleepTicks(nativeWorld,ticks);
   for(var player:level.players())if(player.connection!=null){
    time(player);
    player.connection.send(new ClientboundGameEventPacket(level.isRaining()?ClientboundGameEventPacket.START_RAINING:ClientboundGameEventPacket.STOP_RAINING,0));
    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,level.getRainLevel(1)));
    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,level.getThunderLevel(1)));
   }
  }
 }
 @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){
  if(event.phase!=TickEvent.Phase.START||!(event.player instanceof ServerPlayer player)||!owns(player)||!player.isSleeping())return;
  var pos=player.getSleepingPos();String why=null;var level=player.serverLevel();
  if(pos.isEmpty()||!(level.getBlockState(pos.get()).getBlock() instanceof BedBlock)){player.stopSleepInBed(true,true);return;}
  Mob threat=SourceSleepBedThreat.nearest(level,pos.get());
  if(threat!=null)why=phase(player)==Phase.SLEEPING?"bed_mobs":"bed_not_safe";
  else if(SourceSleepBedThreat.digging(level,pos.get()))why="bed_digging";
  else if(player.getFoodData().getFoodLevel()==0)why=phase(player)==Phase.SLEEPING?"bed_hungry":"bed_too_hungry";
  if(why!=null){player.stopSleepInBed(true,true);notice(player,why);}
 }
 @SubscribeEvent public static void sleepingTime(SleepingTimeCheckEvent event){if(owns(event.getEntity()))event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);}
 @SubscribeEvent public static void wake(PlayerWakeUpEvent event){if(event.getEntity() instanceof ServerPlayer player&&owns(player))transition(player,Phase.FULLY_AWAKE,false);}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)STATES.remove(player);}
 @SubscribeEvent public static void changed(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player){transition(player,Phase.FULLY_AWAKE,true);STATES.remove(player);}}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player&&owns(player))transition(player,Phase.FULLY_AWAKE,true);}
 @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){WORLDS.remove(level);STATES.keySet().removeIf(p->p.level()==level);}}
 private SourceSleepServer(){}
}
