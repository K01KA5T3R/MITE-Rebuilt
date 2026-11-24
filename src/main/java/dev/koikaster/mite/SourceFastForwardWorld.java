package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native transport for js. Chunk persistence and actual ticking admission
 * remain in their native owners. The source kernel is used without editing. */
final class SourceFastForwardWorld implements SourceFastForwardRules.Host {
 private final ServerLevel level;private final SourceHistoricalTicks.Frame frame;private final long knownFrom;private final List<Chunk> chunks;private final Map<BlockState,TickBlock> blocks=new IdentityHashMap<>();
 private SourceFastForwardWorld(ServerLevel level,List<LevelChunk> chunks,SourceHistoricalTicks.Frame frame,long knownFrom){this.level=level;this.frame=frame;this.knownFrom=knownFrom;this.chunks=chunks.stream().map(Chunk::new).toList();}
 static void run(ServerLevel level,List<LevelChunk> chunks){
  if(chunks.isEmpty())return;
  OriginalRandomTables.initialize(level.getSeed());
  var timeline=SourceFastForwardTimeline220.current(level);
  long game=Math.max(0,level.getGameTime()-1),calendar=timeline.calendar(game);
  try(var frame=SourceHistoricalTicks.open(level,calendar,game,level.oRainLevel,level.rainLevel,level.oThunderLevel,level.thunderLevel,level.getSkyDarken(),timeline::calendar)){
   SourceFastForwardRules.active(new SourceFastForwardWorld(level,chunks,frame,Math.min(game,timeline.knownFrom())));
  }
 }
 public boolean tournamentPrevents(){return SourceTournament.mode(level.getServer())!=null;}public boolean skip(){return level.getServer().isDedicatedServer()&&level.dimension()==Level.OVERWORLD;}public int dimension(){return level.dimension()==Level.NETHER?-1:level.dimension()==Level.END?1:level.dimension()==WorldEvents.UNDERWORLD?-2:level.dimension()==Level.OVERWORLD?0:2;}
 public long time(){return frame.time();}public void time(long value){frame.time(value);}public Object environment(){return frame.environment();}public void restoreEnvironment(Object value){frame.restore(value);}public void rain(float old,float now){frame.rain(old,now);}public void thunder(float old,float now){frame.thunder(old,now);}public void skyDarken(int value){frame.darken(value);}
 public SourceFastForwardRules.Weather weather(){var creation=SourceMoonWeather.visualCreationTime(level);if(creation.isEmpty())throw new IllegalStateException("Source weather metadata missing before chunk tick");var event=SourceMoonWeatherRules.at(SourceMoonWeatherRules.today(creation.getAsLong(),0,SourceMoonCycle.dayOfWorld(frame.calendar())),frame.calendar());return event==null?null:elapsed->event.storming(frame.calendarAt(elapsed));}
 public int computeSkyDarken(){float angle=LegacyLighting.celestialAngle(frame.calendar(),1,dimension());float dark=LegacyLighting.clamp(1-(LegacyLighting.cos(angle*(float)Math.PI*2)*2+.5f)),light=1-dark;light=(float)(light*(1-(double)(level.getRainLevel(1)*5)/16));light=(float)(light*(1-(double)(level.getThunderLevel(1)*5)/16));return(int)((1-light)*11);}
 public void updateSkylight(SourceFastForwardRules.Chunk ignored){level.getChunkSource().getLightEngine().tryScheduleUpdate();}
 public Iterable<? extends SourceFastForwardRules.Player> players(){return level.players().stream().map(player->new SourceFastForwardRules.Player(){public int x(){return player.chunkPosition().x;}public int z(){return player.chunkPosition().z;}}).toList();}
 public boolean chance16(){return OriginalRandomTables.chance_in_16[++OriginalRandomTables.random_number_index&32767];}public int samplePosition(){return((SourceFastForwardNativeAccess.Sample)(Object)level).mite$nextBlockSample();}public int height(int x,int z){return level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z);}
 private boolean bounded(BlockPos p){return !level.isOutsideBuildHeight(p)&&level.hasChunkAt(p);}
 private boolean water(BlockPos p){return level.getFluidState(p).is(FluidTags.WATER);}
 public boolean freeze(int x,int y,int z){var p=new BlockPos(x,y,z);if(!bounded(p)||level.getBiome(p).value().getBaseTemperature()>.15f||level.getBrightness(LightLayer.BLOCK,p)>=10)return false;var state=level.getBlockState(p);if(!state.is(Blocks.WATER)||state.getValue(LiquidBlock.LEVEL)!=0)return false;for(var side:Direction.Plane.HORIZONTAL)if(!level.hasChunkAt(p.relative(side)))return false;return !water(p.west())||!water(p.east())||!water(p.north())||!water(p.south());}
 public void ice(int x,int y,int z){level.setBlock(new BlockPos(x,y,z),Blocks.ICE.defaultBlockState(),3);}
 public boolean precipitating(){return level.isRaining();}
 public boolean snowfall(int x,int y,int z){var p=new BlockPos(x,y,z);if(!bounded(p)||level.getBiome(p).value().getBaseTemperature()>.15f||level.getBrightness(LightLayer.BLOCK,p)>=10)return false;var below=level.getBlockState(p.below());var state=level.getBlockState(p);if(below.is(Blocks.FARMLAND)&&!state.is(Blocks.PUMPKIN_STEM)&&!state.is(Blocks.ATTACHED_PUMPKIN_STEM))return true;return state.isAir()&&!below.is(Blocks.ICE)&&Blocks.SNOW.defaultBlockState().canSurvive(level,p);}
 public void placeSnow(int x,int y,int z){var p=new BlockPos(x,y,z);var state=level.getBlockState(p);if(state.getBlock() instanceof CropBlock||state.getBlock() instanceof StemBlock||state.getBlock() instanceof AttachedStemBlock)SourceSnowfallDrops.drop(level,p,state);level.setBlock(p,Blocks.SNOW.defaultBlockState(),3);}
 public boolean biomeRain(int x,int z){var biome=level.getBiome(new BlockPos(x,height(x,z),z)).value();return(biome.hasPrecipitation()||OriginalWeather.blood(level))&&biome.getBaseTemperature()>.15f;}
 private TickBlock wrap(BlockState state){return state.isAir()&&state.getFluidState().isEmpty()?null:blocks.computeIfAbsent(state,TickBlock::new);}
 public SourceFastForwardRules.Block block(int x,int y,int z){return wrap(level.getBlockState(new BlockPos(x,y,z)));}
 public float serverLoad(){var server=level.getServer();if(server.getTickCount()<100)return-1;long total=0;for(long nanos:server.tickTimes)total+=nanos;return(float)(total/1_000_000.0)/5000f;}
 public int timeOfDay(){return(int)(frame.calendar()%24000);}public Iterable<Chunk> activeChunks(){return chunks;}public Chunk chunk(SourceFastForwardRules.Position position){return(Chunk)position;}public void forwarding(boolean value){frame.forwarding(value);}
 public int earliestRelease(){return 195;}public void flushLegacy(){throw new IllegalStateException("Raw pre-R195 save format cannot be a native chunk");}public void ahead(long chunk,long world){throw new IllegalStateException("Chunk random time ahead of world: "+chunk+">"+world);}
 private final class Chunk implements SourceFastForwardRules.Chunk {
  final LevelChunk chunk;final SourceChunkClock clock;Chunk(LevelChunk chunk){this.chunk=chunk;clock=(SourceChunkClock)(Object)chunk;if(clock.mite$randomTickTime()!=0&&clock.mite$randomTickTime()<knownFrom)clock.mite$randomTickTime(knownFrom);}public int x(){return chunk.getPos().x;}public int z(){return chunk.getPos().z;}public long lastTime(){return clock.mite$randomTickTime();}public void lastTime(long value){clock.mite$randomTickTime(value);}public void saveOnCompletion(){clock.mite$saveAfterForward(true);}
  public SourceFastForwardRules.Section[] sections(){var actual=chunk.getSections();var result=new SourceFastForwardRules.Section[actual.length];for(int i=0;i<actual.length;i++){final int y=level.getSectionYFromSectionIndex(i)*16;final var section=actual[i];if(section!=null)result[i]=new SourceFastForwardRules.Section(){public boolean randomTicks(){return section.isRandomlyTicking();}public int baseY(){return y;}public SourceFastForwardRules.Block block(int x,int y,int z){return wrap(section.getBlockState(x,y,z));}};}return result;}
 }
 private final class TickBlock implements SourceFastForwardRules.Block {
  final BlockState state;TickBlock(BlockState state){this.state=state;}public boolean randomTicks(){return state.isRandomlyTicking()||state.getFluidState().isRandomlyTicking();}
  public void tick(SourceFastForwardRules.Host ignored,int x,int y,int z){var p=new BlockPos(x,y,z);if(state.isRandomlyTicking())state.randomTick(level,p,level.random);var fluid=state.getFluidState();if(fluid.isRandomlyTicking())fluid.randomTick(level,p,level.random);}
  public void fillRain(SourceFastForwardRules.Host ignored,int x,int y,int z){state.getBlock().handlePrecipitation(state,level,new BlockPos(x,y,z),Biome.Precipitation.RAIN);}
 }
}
