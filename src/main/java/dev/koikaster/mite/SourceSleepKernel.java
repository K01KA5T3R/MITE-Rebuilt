package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;

/** R195 js sleep simulation, nearby-mob search, placement and final path-node
 * selection. The adapter supplies native storage, shapes, pathfinding and RNG;
 * it must not replace this loop with a vanilla calendar jump. Not yet wired. */
public final class SourceSleepKernel {
 public enum Notice {HUNGRY,MOBS}
 public record Point(int x,int y,int z){}
 public record Position(double x,double y,double z){}
 public interface Rng {int nextInt();int nextInt(int bound);float nextFloat();}
 public interface Sleeper {
  boolean ghost();boolean excluded();float health();
  void foodTick();boolean hasFoodEnergy();boolean sleeping();boolean inBed();
  void wake(boolean getOut,Mob cause);void notice(Notice notice);
  void wellRested();void clearEffects();
  Point bed();double x();double y();double z();
 }
 public interface Mob {
  float health();boolean hostile();boolean enderman();float eyeHeight();
  void move(double x,double y,double z);void move(double x,double y,double z,float yaw,float pitch);
  void synchronizePreviousPosition(boolean value);boolean canSpawnHere(boolean checkLight);
  void refreshDespawn(int cap);void initialize();void spawn();void ambientSound();
 }
 public interface Path {Point end();int size();Point node(int index);}
 public interface World {
  List<? extends Sleeper> players();Rng random();
  List<? extends Mob> nearby(Sleeper player,int horizontal,int vertical);
  Path path(Mob mob,Point target,int maximumLength);
  boolean line(Mob mob,Position eye,Position target);
  /** Null is the source's absent block; otherwise the local block top. */
  Double blockTop(int x,int y,int z);
  boolean passable(int x,int y,int z,boolean includeLiquids);
  boolean loaded(int x,int y,int z);double nearestPlayerDistanceSquared(int x,int y,int z);
  boolean outdoors(int x,int y,int z);float brightness(int x,int y,int z);
  boolean canSpawnAt(int x,int y,int z,boolean checkNearPlayers,double[] resultingY);
  Object suitableCreature(int x,int y,int z);boolean excludedCreature(Object type);
  Mob construct(Object type)throws Exception;void constructionError(Exception error);
  int tableMax(int index);int tableSevenMinusThree(int index);
  void advanceTotalWorldTime(long ticks);void calculateInitialWeather();
 }
 private Mob wakeCausingEntity;
 private static boolean eligible(Sleeper player){return !player.ghost()&&!player.excluded()&&!(player.health()<=0);}
 private static double distance(int x,int y,int z){return (double)x*x+(double)y*y+(double)z*z;}

 // The R195 order and single random draw also apply to larger dedicated servers.
 public Sleeper randomPlayer(World world){
  var players=world.players();
  if(players.isEmpty())return null;
  var candidates=new ArrayList<Sleeper>();
  for(var player:players)if(eligible(player))candidates.add(player);
  if(candidates.isEmpty())return null;
  return candidates.get(world.random().nextInt(candidates.size()));
 }
 public Mob tryPlaceNewSuitableMob(World world,int x,int y,int z,int minimum,int maximum){
  int minSquared=minimum*minimum,maxSquared=maximum*maximum,index=world.random().nextInt();
  for(int attempt=0;attempt<16;++attempt){
   int dx=world.tableMax(++index&32767)%(maximum*2+1)-maximum;
   int dy=world.tableSevenMinusThree(++index&32767);
   int dz=world.tableMax(++index&32767)%(maximum*2+1)-maximum;
   int trialX=x+dx,trialY=y+dy,trialZ=z+dz;
   for(int i=0;i<8&&world.passable(trialX,trialY-1,trialZ,false);++i)--trialY;
   for(int i=0;i<8&&!world.passable(trialX,trialY,trialZ,false);++i)++trialY;
   double nearest=world.nearestPlayerDistanceSquared(trialX,trialY,trialZ);
   if(nearest<minSquared||nearest>maxSquared||!world.loaded(trialX,trialY-1,trialZ)||!world.loaded(trialX,trialY,trialZ)||!world.loaded(trialX,trialY+1,trialZ)||world.passable(trialX,trialY-1,trialZ,true)||!world.passable(trialX,trialY,trialZ,false)||!world.passable(trialX,trialY+1,trialZ,false)||(!world.outdoors(trialX,trialY,trialZ)?world.brightness(trialX,trialY,trialZ)>.1f:world.brightness(trialX,trialY,trialZ)>.4f))continue;
   var resultingY=new double[1];Object type;
   if(!world.canSpawnAt(trialX,trialY,trialZ,false,resultingY)||(type=world.suitableCreature(trialX,trialY,trialZ))==null||world.excludedCreature(type))continue;
   Mob mob;try{mob=world.construct(type);}catch(Exception error){world.constructionError(error);continue;}
   mob.move((float)trialX+.5f,resultingY[0],(float)trialZ+.5f,world.random().nextFloat()*360f,0);
   if(!mob.canSpawnHere(false))continue;
   return mob;
  }
  return null;
 }
 public Sleeper pathNearbyMobToRandomSleepingPlayer(World world){
  var player=randomPlayer(world);if(player==null)return null;
  List<? extends Mob> mobs=null;int distance=0;
  while(distance<64&&(mobs=world.nearby(player,distance+=16,distance/4)).size()<16){}
  var tried=new boolean[mobs.size()];int attempts=Math.min(mobs.size(),16);
  for(int attempt=0;attempt<attempts;++attempt){
   int index=world.random().nextInt(mobs.size());if(tried[index])continue;
   var mob=mobs.get(index);
   if(mob.health()>0&&mob.hostile()&&!mob.enderman()&&tryPathMobToSleepingPlayer(world,mob,player,distance,true))return player;
   tried[index]=true;
  }
  return null;
 }
 public boolean tryPathMobToSleepingPlayer(World world,Mob mob,Sleeper player,int maximumLength,boolean syncPrevious){
  var bed=player.bed();var path=world.path(mob,bed,maximumLength);if(path==null)return false;
  var end=path.end();if(distance(end.x-bed.x,end.y-bed.y,end.z-bed.z)>2)return false;
  Point point=null;int start=Math.max(path.size()-8,0);
  for(int i=start;i<path.size();++i){
   point=path.node(i);
   var target=new Position((float)bed.x+.5f,(float)bed.y+1.5f,(float)bed.z+.5f);
   var eye=new Position((float)point.x+.5f,(float)point.y+mob.eyeHeight(),(float)point.z+.5f);
   if(world.line(mob,eye,target))break;
   point=null;
  }
  if(point==null)point=path.node(start);
  mob.synchronizePreviousPosition(syncPrevious);
  var top=world.blockTop(point.x,point.y-1,point.z);
  mob.move((float)point.x+.5f,top==null?point.y:(double)(point.y-1)+top,(float)point.z+.5f);
  wakeCausingEntity=mob;return true;
 }
 public boolean runSleepTicks(World world,int ticks){
  int slept;boolean woke=false;
  for(slept=0;slept<ticks&&!woke;++slept){
   for(var player:world.players()){
    if(!eligible(player))continue;
    player.foodTick();
    if(!player.hasFoodEnergy()){player.wake(true,null);player.notice(Notice.HUNGRY);woke=true;continue;}
    if(player.sleeping())continue;
    woke=true;
   }
   if(woke||world.random().nextInt(1000)!=0)continue;
   var playerToWake=pathNearbyMobToRandomSleepingPlayer(world);Sleeper player;Mob mob;
   if(playerToWake==null&&(player=randomPlayer(world))!=null&&(mob=tryPlaceNewSuitableMob(world,(int)Math.floor(player.x()),(int)Math.floor(player.y()),(int)Math.floor(player.z()),8,48))!=null){
    mob.refreshDespawn(-2400);
    if(tryPathMobToSleepingPlayer(world,mob,player,32,false))playerToWake=player;
    if(playerToWake!=null||world.random().nextInt(2)==0){mob.initialize();mob.spawn();}
   }
   if(playerToWake==null)continue;
   wakeCausingEntity.ambientSound();playerToWake.wake(true,wakeCausingEntity);playerToWake.notice(Notice.MOBS);woke=true;
  }
  if(slept==ticks&&!woke&&slept>=6000)for(var player:world.players())if(eligible(player)&&player.inBed())player.wellRested();
  world.advanceTotalWorldTime(slept);
  if(slept>=1000)for(var player:world.players())if(eligible(player)&&player.inBed())player.clearEffects();
  world.calculateInitialWeather();return slept==ticks;
 }
}
