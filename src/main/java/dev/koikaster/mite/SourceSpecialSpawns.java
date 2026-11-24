package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraftforge.registries.ForgeRegistries;
/** R195 js.getSuitableCreature, tm.isValidLightLevel and rp.getCanSpawnHere.
 * Absolute source Y cutoffs are retained; modern terrain is not rescaled. */
public final class SourceSpecialSpawns {
 private static final ThreadLocal<SelectedPlacement> PLACEMENT=new ThreadLocal<>();

 private static final class SelectedPlacement {
  final String name;
  final ServerLevelAccessor level;
  final BlockPos position;
  boolean consumed;

  SelectedPlacement(String name,ServerLevelAccessor level,BlockPos position){
   this.name=name;
   this.level=level;
   this.position=position.immutable();
  }
 }

 static boolean checkSelectedPlacement(EntityType<?> type,ServerLevelAccessor level,BlockPos pos,RandomSource random){
  var id=ForgeRegistries.ENTITY_TYPES.getKey(type);
  if(id==null||!id.getNamespace().equals(MiteReforged.ID)){
   return SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,random);
  }
  var previous=PLACEMENT.get();
  PLACEMENT.set(new SelectedPlacement(id.getPath(),level,pos));
  try{
   return SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,random);
  }finally{
   if(previous==null)PLACEMENT.remove();else PLACEMENT.set(previous);
  }
 }

 static boolean takeSelection(String name,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos){
  var selection=PLACEMENT.get();
  if(selection==null||selection.consumed||reason!=MobSpawnType.NATURAL||selection.level!=level
      ||!selection.position.equals(pos)||!selection.name.equals(name))return false;
  // Forge publishes the placement event after its registered default predicate.
  selection.consumed=true;
  return true;
 }

 public static boolean sourceDay(Level level){return SourceMoonCycle.daytime(level.getDayTime());}
 public static boolean bloodUp(Level level){return !sourceDay(level)&&OriginalWeather.blood(level);}
 public static boolean blueUp(Level level){return Settings.MOONS&&level.dimension()==Level.OVERWORLD&&SourceMoonCycle.blue(level.getDayTime(),true);}
 public static boolean selection(String name,ServerLevelAccessor level,BlockPos pos){
  if(level.getLevel().dimension()!=Level.OVERWORLD)return true;
  boolean blood=bloodUp(level.getLevel());
  return switch(name){
   case "ghoul"->pos.getY()<=56||blood;
   case "wight"->pos.getY()<=48||blood&&wightFreezing(level,pos);
   case "revenant"->pos.getY()<=44||blood;
   case "invisible_stalker","earth_elemental"->pos.getY()<=40;
   case "shadow"->pos.getY()<=32||blood&&shadowDesert(level,pos);
   case "bone_lord"->pos.getY()<=32||blood;
   case "demon_spider","phase_spider","hellhound"->pos.getY()<=32;
   default->true;
  };
 }
 private static int classicBiomeAt(ServerLevelAccessor level,BlockPos pos){
  var source=level.getLevel().getChunkSource().getGenerator().getBiomeSource();
  return source instanceof ClassicOverworldBiomeSource classic?classic.originalBlockBiome(pos.getX(),pos.getZ()):-1;
 }
 private static boolean shadowDesert(ServerLevelAccessor level,BlockPos pos){
  int biome=classicBiomeAt(level,pos);
  if(biome<0)return level.getBiome(pos).is(Biomes.DESERT);
  return classicDesert(biome);
 }
 private static boolean classicDesert(int biome) {
  return biome == OriginalOverworldBiomes.DESERT || biome == OriginalOverworldBiomes.DESERT_HILLS
   || biome == OriginalOverworldBiomes.DESERT_RIVER;
 }
 private static boolean wightFreezing(ServerLevelAccessor level,BlockPos pos){
  int biome=classicBiomeAt(level,pos);
  return biome<0?level.getBiome(pos).value().getBaseTemperature()<=.15f:ClassicSnowCover.freezing(biome);
 }
 private static boolean receivesSpawnWeather(ServerLevelAccessor level, BlockPos pos) {
  if (OriginalWeather.blood(level.getLevel())) return true;
  int biome = classicBiomeAt(level, pos);
  return biome < 0 ? level.getBiome(pos).value().hasPrecipitation() : !classicDesert(biome);
 }
 public static boolean light(ServerLevelAccessor level,BlockPos position,RandomSource random){
  var pos=SourcePhaseNativeWorld.liquid(level.getBlockState(position))?position.above():position;
  if(level.getBrightness(LightLayer.SKY,pos)>random.nextInt(32))return false;
  var world=level.getLevel();boolean weather=receivesSpawnWeather(level,pos);
  float angle=LegacyLighting.celestialAngle(world.getDayTime(),1,world.dimension()==Level.NETHER?-1:world.dimension()==Level.END?1:world.dimension()==WorldEvents.UNDERWORLD?-2:0);
  float dark=LegacyLighting.clamp(1-(LegacyLighting.cos(angle*(float)Math.PI*2)*2+.5f));
  if(weather){float brightness=1-dark;brightness=(float)(brightness*(1-(double)(world.getRainLevel(1)*5)/16));brightness=(float)(brightness*(1-(double)(world.getThunderLevel(1)*5)/16));dark=1-brightness;}
  int subtraction=world.isThundering()&&weather?10:(int)(dark*11);
  return level.getRawBrightness(pos,subtraction)<=random.nextInt(world.dimension()==Level.OVERWORLD&&level.canSeeSky(position)?8:5);
 }
 public static boolean allow(String name,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
  return allow(name,level,reason,pos,random,takeSelection(name,level,reason,pos));
 }
 static boolean allow(String name,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random,boolean selected){
  if(level.getDifficulty()==Difficulty.PEACEFUL)return false;
  if(!selected&&reason!=MobSpawnType.SPAWNER&&!selection(name,level,pos))return false;
  if(reason!=MobSpawnType.SPAWNER&&!light(level,pos,random))return false;
  var support=level.getBlockState(pos.below());
  if(name.equals("clay_golem")&&!support.is(Blocks.CLAY))return false;
  if(name.equals("earth_elemental")&&!(support.is(Blocks.STONE)||support.is(Blocks.DEEPSLATE)||support.is(Blocks.OBSIDIAN)||support.is(Blocks.NETHERRACK)||support.is(Blocks.END_STONE)))return false;
  if(level.getBiome(pos).is(Biomes.MUSHROOM_FIELDS))return false;
  return name.equals("hellhound")?level.getFluidState(pos).isEmpty()&&level.getFluidState(pos.above()).isEmpty():!name.equals("fire_elemental")||level.getFluidState(pos).isEmpty();
 }
 public static boolean direWolf(ServerLevelAccessor level,MobSpawnType reason,BlockPos pos){
  // rp uses saved light without sky subtraction, so this is not a daytime gate.
  // aci has a separate initial-worldgen path, and skips restocking light on blue nights.
  if(!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)||reason!=MobSpawnType.SPAWNER&&reason!=MobSpawnType.CHUNK_GENERATION&&!blueUp(level.getLevel())&&level.getRawBrightness(pos,0)<=8)return false;
  return level.getFluidState(pos).isEmpty()&&level.getFluidState(pos.above()).isEmpty();
 }
 private SourceSpecialSpawns(){}
}
