package dev.koikaster.mite;

import java.util.List;

/** js's bed quorum/night owner and bdi's client fade state machine. The native
 * sleeping-percentage override is applied by the server adapter before this
 * literal source cohort is supplied, not by changing the R195 sleep loop. */
public final class SourceSleepControlRules {
 public enum Phase {FULLY_AWAKE,FALLING_ASLEEP,SLEEPING,WAKING_UP}
 public enum Signal {NONE,SLEEPING,FULLY_AWAKE}
 public static final class State {
  public Phase phase=Phase.FULLY_AWAKE;public int counter;
  public State(){}public State(Phase phase,int counter){this.phase=phase;this.counter=counter;}
 }
 public interface Player {
  boolean ghost();boolean excluded();float health();boolean inBed();boolean sleeping();Phase phase();
  void startFallingAsleep();void wakeGently();
 }
 public interface World {
  List<? extends Player> players();boolean bloodMoon();boolean tournament();long dayTime();boolean daylightCycle();
  void runSleepTicks(int ticks);
 }
 public static int adjusted(long time){return (int)((Math.floorMod(time,24000)+6000)%24000);}
 public static int untilSunrise(long time){int adjusted=adjusted(time);return adjusted<5000?5000-adjusted:5000-adjusted+24000;}
 public static boolean sleepWindow(long time){int adjusted=adjusted(time);return adjusted<4000||adjusted>=21000;}
 public static boolean hasNonGhostPlayers(World world){for(var player:world.players())if(!player.ghost()&&!player.excluded())return true;return false;}
 public static boolean allPlayersInBedOrDead(World world){
  if(!hasNonGhostPlayers(world))return false;
  boolean any=false;for(var player:world.players()){
   if(player.ghost()||player.excluded()||player.health()<=0)continue;
   if(!player.inBed())return false;any=true;
  }return any;
 }
 public static boolean allPlayersAsleepOrDead(World world){
  if(!hasNonGhostPlayers(world))return false;
  boolean any=false;for(var player:world.players()){
   if(player.ghost()||player.excluded()||player.health()<=0)continue;
   if(!player.sleeping())return false;any=true;
  }return any;
 }
 public static void wakeAllPlayersGently(World world){for(var player:world.players())if(!player.ghost()&&!player.excluded()&&(player.phase()==Phase.FALLING_ASLEEP||player.phase()==Phase.SLEEPING))player.wakeGently();}
 public static void signalAllPlayersToStartFallingAsleep(World world){for(var player:world.players())if(!player.ghost()&&!player.excluded()&&(player.phase()==Phase.WAKING_UP||player.phase()==Phase.FULLY_AWAKE))player.startFallingAsleep();}
 public static void tick(World world){
  if(hasNonGhostPlayers(world)){
   boolean prevented=world.bloodMoon()||world.tournament();
   if(!prevented&&allPlayersInBedOrDead(world)&&sleepWindow(world.dayTime())){
    if(allPlayersAsleepOrDead(world)){
     if(world.daylightCycle())world.runSleepTicks(untilSunrise(world.dayTime()));
     else wakeAllPlayersGently(world);
    }else signalAllPlayersToStartFallingAsleep(world);
   }else wakeAllPlayersGently(world);
  }
 }
 /** bcw's signals retain the current fade counter when reversing direction. */
 public static void falling(State state){state.phase=Phase.FALLING_ASLEEP;}
 public static void waking(State state){state.phase=Phase.WAKING_UP;}
 public static Signal clientTick(State state,boolean cannotSleep){
  if(state.phase==Phase.FALLING_ASLEEP&&cannotSleep)state.phase=state.counter==0?Phase.FULLY_AWAKE:Phase.WAKING_UP;
  if(state.phase==Phase.FALLING_ASLEEP){
   if(++state.counter>=100){state.phase=Phase.SLEEPING;state.counter=110;return Signal.SLEEPING;}
  }else if(state.phase==Phase.WAKING_UP&&--state.counter<=0){state.phase=Phase.FULLY_AWAKE;state.counter=0;return Signal.FULLY_AWAKE;}
  return Signal.NONE;
 }
 private SourceSleepControlRules(){}
}
