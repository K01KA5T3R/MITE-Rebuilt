package dev.koikaster.mite;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
/** aci.performWorldGenSpawning at the real native initial-population entry.
 * Original species run once here; the native method retains modern species. */
public final class SourceWorldgenSpawning {
 public static void run(Object rawRegion,Object rawBiome,Object rawChunk,Object rawRandom){
  populate((ServerLevelAccessor)rawRegion,rawBiome,rawChunk,rawRandom,SourceSpawningKernel.Group.ANIMAL);
 }
 public static void runAquatic(Object rawRegion,Object rawBiome,Object rawChunk,Object rawRandom){
  var region=(ServerLevelAccessor)rawRegion;
  var level=region.getLevel();
  if(!Level.OVERWORLD.equals(level.dimension())
      ||level.getChunkSource().getGenerator() instanceof ClassicOverworldChunkGenerator)return;
  populate(region,rawBiome,rawChunk,rawRandom,SourceSpawningKernel.Group.AQUATIC);
 }
 private static void populate(ServerLevelAccessor region,Object rawBiome,Object rawChunk,Object rawRandom,SourceSpawningKernel.Group group){
  var biome=(Holder<Biome>)rawBiome;
  var chunk=(ChunkPos)rawChunk;
  var random=(RandomSource)rawRandom;
  SourceSpawningKernel.worldgen(new SourceWorldgenWorld(region),biome.value(),group,chunk.getMinBlockX(),chunk.getMinBlockZ(),16,16,adapt(random));
 }
 static SourceSpawningKernel.Rng adapt(RandomSource random){return new SourceSpawningKernel.Rng(){public int nextInt(int n){return random.nextInt(n);}public float nextFloat(){return random.nextFloat();}public boolean nextBoolean(){return random.nextBoolean();}};}
 private SourceWorldgenSpawning(){}
}
