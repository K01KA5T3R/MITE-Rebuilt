package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** Actual native adapter for the copied R195 cave/strata kernel. The seed comes
 * from native createState, never from a fixed JSON seed or player setting. */
public final class SourceUnderworldChunkGenerator extends ChunkGenerator {
 public static final Codec<SourceUnderworldChunkGenerator> CODEC=RecordCodecBuilder.create(instance->instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator->generator.biomeSource)).apply(instance,SourceUnderworldChunkGenerator::new));
 private OriginalUnderworldTerrain terrain;
 private final Map<Long,byte[]> chunks=new LinkedHashMap<>(32,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,byte[]> entry){return size()>32;}};
 public SourceUnderworldChunkGenerator(BiomeSource biomes){super(biomes);}
 @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
 @Override public synchronized ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures,RandomState random,long seed){terrain=new OriginalUnderworldTerrain(seed);chunks.clear();return super.createState(structures,random,seed);}
 private synchronized byte[] blocks(int chunkX,int chunkZ){
  if(terrain==null)throw new IllegalStateException("Native Underworld seed has not been bound by createState");
  long key=net.minecraft.world.level.ChunkPos.asLong(chunkX,chunkZ);return chunks.computeIfAbsent(key,ignored->terrain.generate(chunkX,chunkZ));
 }
 private static BlockState state(byte token){return switch(token){case OriginalUnderworldTerrain.AIR->Blocks.AIR.defaultBlockState();case OriginalUnderworldTerrain.STONE->Blocks.STONE.defaultBlockState();case OriginalUnderworldTerrain.WATER->Blocks.WATER.defaultBlockState();case OriginalUnderworldTerrain.BEDROCK->Blocks.BEDROCK.defaultBlockState();case OriginalUnderworldTerrain.COBWEB->Blocks.COBWEB.defaultBlockState();case OriginalUnderworldTerrain.MANTLE->SourceUnderworldContent.MANTLE.get().defaultBlockState();default->throw new IllegalStateException("Unmapped source Underworld block token "+token);};}
 @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
  // ChunkStatus owns this ProtoChunk exclusively until the returned future is
  // complete. Like the native flat generator, this fill is synchronous.
  var position=chunk.getPos();byte[] values=blocks(position.x,position.z);var at=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<256;y++){byte token=values[(x*16+z)*256+y];if(token!=OriginalUnderworldTerrain.AIR)chunk.setBlockState(at.set(position.getMinBlockX()+x,y,position.getMinBlockZ()+z),state(token),false);}
  Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG,Heightmap.Types.WORLD_SURFACE_WG));return CompletableFuture.completedFuture(chunk);
 }
 /** Original Underworld declares a cave carver but does not call it from d().
  * Its cave density is already the copied kernel. Additional native Nether
  * carvers would alter the source structure a second time. */
 @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving carving){}
 /** Source replaceBlocksForBiome returns immediately for y_offset120. */
 @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk){}
 /** Source populate calls the biome decorator, not surface animal worldgen.
  * Native inherited applyBiomeDecoration handles the registered biome features;
  * the source dungeon feature runs before ores; mycelium/population remains tracked in the source audit. */
 @Override public void spawnOriginalMobs(WorldGenRegion region){}
 @Override public int getGenDepth(){return 256;}
 @Override public int getSeaLevel(){return 144;}
 @Override public int getMinY(){return 0;}
 @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor height,RandomState random){byte[] values=blocks(Math.floorDiv(x,16),Math.floorDiv(z,16));int start=Math.floorMod(x,16)*4096+Math.floorMod(z,16)*256;var states=new BlockState[256];for(int y=0;y<256;y++)states[y]=state(values[start+y]);return new NoiseColumn(0,states);}
 @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor height,RandomState random){var column=getBaseColumn(x,z,height,random);for(int y=Math.min(255,height.getMaxBuildHeight()-1);y>=Math.max(0,height.getMinBuildHeight());y--)if(type.isOpaque().test(column.getBlock(y)))return y+1;return Math.max(0,height.getMinBuildHeight());}
 @Override public void addDebugScreenInfo(List<String> lines,RandomState random,BlockPos pos){}
}
