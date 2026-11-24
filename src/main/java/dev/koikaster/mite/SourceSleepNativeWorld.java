package dev.koikaster.mite;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

/** Native engine boundary for the literal sleep kernel. Staged: the source
 * consciousness controller and complete per-species spawn acceptance must be
 * bound explicitly before this class can become the live sleep owner. */
public class SourceSleepNativeWorld implements SourceSleepKernel.World {
 final ServerLevel level;
 private final Predicate<ServerPlayer> sleeping;
 private final Predicate<Mob> spawnAcceptance;
 private final Map<ServerPlayer,Person> people=new IdentityHashMap<>();
 private final Map<Mob,Body> bodies=new IdentityHashMap<>();
 private final SourceSpawnWorld spawning;
 private final SourceSleepKernel.Rng random;
 public SourceSleepNativeWorld(ServerLevel level,Predicate<ServerPlayer> sleeping,Predicate<Mob> spawnAcceptance){
  if(level.dimension()!=Level.OVERWORLD)throw new IllegalArgumentException("Sleep world must allow beds");
  this.level=level;this.sleeping=Objects.requireNonNull(sleeping);this.spawnAcceptance=Objects.requireNonNull(spawnAcceptance);
  spawning=SourceNaturalSpawning.state(level).world;OriginalRandomTables.initialize(level.getSeed());
  random=new SourceSleepKernel.Rng(){public int nextInt(){return level.random.nextInt();}public int nextInt(int n){return level.random.nextInt(n);}public float nextFloat(){return level.random.nextFloat();}};
 }
 public final class Person implements SourceSleepKernel.Sleeper {
  final ServerPlayer player;Person(ServerPlayer player){this.player=player;}
  public boolean ghost(){return player.isSpectator();}public boolean excluded(){return false;}public float health(){return player.getHealth();}
  public void foodTick(){SurvivalFood.tick(player.getFoodData(),player);}public boolean hasFoodEnergy(){return SurvivalFood.hasFoodEnergy(player);}
  public boolean sleeping(){return sleeping.test(player);}public boolean inBed(){return player.getSleepingPos().filter(pos->level.getBlockState(pos).getBlock() instanceof BedBlock).isPresent();}
  public void wake(boolean getOut,SourceSleepKernel.Mob cause){
   var bed=player.getSleepingPos();if(getOut)player.stopSleepInBed(true,true);
   if(cause instanceof Body body){var mob=body.mob;player.lookAt(EntityAnchorArgument.Anchor.EYES,mob.position().add(0,mob.getBbHeight()*.75f,0));}
   else if(getOut&&bed.isPresent())player.lookAt(EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(bed.get()));
  }
  public void notice(SourceSleepKernel.Notice notice){player.displayClientMessage(Component.translatable(notice==SourceSleepKernel.Notice.HUNGRY?"message.mite_reforged.bed_hungry":"message.mite_reforged.bed_mobs"),true);}
  public void wellRested(){SourceReferencedBooks.award(player,"well_rested");}
  public void clearEffects(){player.removeAllEffects();}
  public SourceSleepKernel.Point bed(){var p=player.getSleepingPos().orElseThrow();return new SourceSleepKernel.Point(p.getX(),p.getY(),p.getZ());}
  public double x(){return player.getX();}public double y(){return player.getY();}public double z(){return player.getZ();}
 }
 Person person(ServerPlayer player){return people.computeIfAbsent(player,Person::new);}
 Body body(Mob mob){return bodies.computeIfAbsent(mob,Body::new);}
 @Override public List<? extends SourceSleepKernel.Sleeper> players(){return level.players().stream().map(this::person).toList();}
 @Override public SourceSleepKernel.Rng random(){return random;}
 @Override public List<? extends SourceSleepKernel.Mob> nearby(SourceSleepKernel.Sleeper raw,int horizontal,int vertical){var player=((Person)raw).player;return level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(horizontal,vertical,horizontal),mob->SourceSleepSpecies.hostile(mob)).stream().map(this::body).toList();}
 /** Each query uses a fresh search and temporarily closes door opening. Reusing
  * navigation.createPath can return the active cached route with wrong flags.
  * The actor's active route, target and navigation clock are never replaced. */
 @Override public SourceSleepKernel.Path path(SourceSleepKernel.Mob raw,SourceSleepKernel.Point target,int maximum){
  var mob=((Body)raw).mob;var evaluator=mob.getNavigation().getNodeEvaluator();boolean open=evaluator.canOpenDoors();
  var origin=mob.blockPosition();int radius=maximum+8;var region=new PathNavigationRegion(level,origin.offset(-radius,-radius,-radius),origin.offset(radius,radius,radius));
  net.minecraft.world.level.pathfinder.Path result;
  try{evaluator.setCanOpenDoors(false);result=new PathFinder(evaluator,Math.max(1,maximum*16)).findPath(region,mob,Set.of(new BlockPos(target.x(),target.y(),target.z())),maximum,0,1);}
  finally{evaluator.setCanOpenDoors(open);}
  if(result==null||result.getEndNode()==null)return null;
  return new SourceSleepKernel.Path(){private SourceSleepKernel.Point point(Node node){return new SourceSleepKernel.Point(node.x,node.y,node.z);}public SourceSleepKernel.Point end(){return point(result.getEndNode());}public int size(){return result.getNodeCount();}public SourceSleepKernel.Point node(int i){return point(result.getNode(i));}};
 }
 @Override public boolean line(SourceSleepKernel.Mob raw,SourceSleepKernel.Position eye,SourceSleepKernel.Position target){return OriginalVision.line(((Body)raw).mob,new Vec3(eye.x(),eye.y(),eye.z()),new Vec3(target.x(),target.y(),target.z()),false);}
 @Override public Double blockTop(int x,int y,int z){var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);if(state.isAir())return null;var shape=state.getShape(level,pos);return shape.isEmpty()?0:shape.max(Direction.Axis.Y);}
 @Override public boolean passable(int x,int y,int z,boolean liquids){return SourceTroopPlacement.passable(level,new BlockPos(x,y,z),liquids);}
 @Override public boolean loaded(int x,int y,int z){var pos=new BlockPos(x,y,z);return !level.isOutsideBuildHeight(pos)&&level.getChunkSource().getChunkNow(x>>4,z>>4)!=null;}
 @Override public double nearestPlayerDistanceSquared(int x,int y,int z){double closest=Double.MAX_VALUE;for(var player:level.players())if(!player.isSpectator()){double distance=OriginalEnvironmentPaths.sq(x-player.getBlockX(),y-player.getBlockY(),z-player.getBlockZ());if(distance<closest)closest=distance;}return closest>65536?Double.MAX_VALUE:closest;}
 @Override public boolean outdoors(int x,int y,int z){return SourceSpawnOutdoors.isOutdoors(level,new BlockPos(x,y,z));}
 @Override public float brightness(int x,int y,int z){return LegacyLighting.brightness(level.getMaxLocalRawBrightness(new BlockPos(x,y,z)),0);}
 @Override public boolean canSpawnAt(int x,int y,int z,boolean initial,double[] result){return SourceSpawningKernel.canSpawnAt(spawning,SourceSpawningKernel.Group.HOSTILE,x,y,z,initial,result);}
 @Override public Object suitableCreature(int x,int y,int z){return spawning.suitable(SourceSpawningKernel.Group.HOSTILE,x,y,z);}
 @Override public boolean excludedCreature(Object raw){return SourceSleepSpecies.excluded(raw);}
 @Override public SourceSleepKernel.Mob construct(Object raw){var entity=((EntityType<?>)raw).create(level);if(!(entity instanceof Mob mob))throw new IllegalArgumentException("Expected a source living creature");return body(mob);}
 @Override public void constructionError(Exception error){com.mojang.logging.LogUtils.getLogger().error("R195 sleep creature construction",error);}
 @Override public int tableMax(int index){return OriginalRandomTables.int_max[index];}@Override public int tableSevenMinusThree(int index){return OriginalRandomTables.int_7_minus_3[index];}
 @Override public void advanceTotalWorldTime(long ticks){((ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+ticks);level.setDayTime(level.getDayTime()+ticks);}
 @Override public void calculateInitialWeather(){SourceMoonWeather.beforeCycle(level);boolean rain=level.getLevelData().isRaining();level.rainLevel=rain?1:0;level.thunderLevel=rain&&level.getLevelData().isThundering()?1:0;level.updateSkyBrightness();}
 public final class Body implements SourceSleepKernel.Mob {
  Mob mob;boolean sync;Body(Mob mob){this.mob=mob;}
  public float health(){return mob.getHealth();}public boolean hostile(){return SourceSleepSpecies.hostile(mob);}public boolean enderman(){return mob instanceof EnderMan;}public float eyeHeight(){return mob.getEyeHeight();}
  public void move(double x,double y,double z){mob.setPos(x,y,z);if(sync)mob.setOldPosAndRot();}
  public void move(double x,double y,double z,float yaw,float pitch){mob.moveTo(x,y,z,yaw,pitch);}
  public void synchronizePreviousPosition(boolean value){sync=value;}
  public boolean canSpawnHere(boolean checkLight){if(checkLight)throw new IllegalArgumentException("Sleep placement uses the source no-light query");return spawnAcceptance.test(mob);}
  public void refreshDespawn(int cap){OriginalDespawn.refresh(mob,cap);}
  public void initialize(){mob=SourceSpawnInitialization.finish(mob,level,MobSpawnType.EVENT,null).actor();}
  public void spawn(){if(!mob.isSpawnCancelled())level.addFreshEntityWithPassengers(mob);}
  public void ambientSound(){mob.playAmbientSound();}
 }
}
