package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
/** Generation-region adapter. Population waits for native chunk promotion before entering the live world. */
public class SourceWorldgenWorld extends SourceSpawnWorld {
 protected final ServerLevelAccessor region;
 private final SourceSpawningKernel.Rng populationRandom;
 private final ProtoChunk decorationCenter;

 SourceWorldgenWorld(ServerLevelAccessor region){this(region,null);}

 SourceWorldgenWorld(ServerLevelAccessor region,ChunkAccess center){
  super(region.getLevel(),null);
  this.region=region;
  populationRandom=SourceWorldgenSpawning.adapt(region.getRandom());
  decorationCenter=center instanceof ProtoChunk proto && !(proto instanceof ImposterProtoChunk)
    && !proto.getStatus().isOrAfter(ChunkStatus.FEATURES) ? proto : null;
 }
 @Override public SourceSpawningKernel.Rng random(){return populationRandom;}
 @Override public int minimumY(){return region.getMinBuildHeight();}
 @Override public SourceSpawningKernel.Block block(int x,int y,int z){var pos=new BlockPos(x,y,z);return new RegionBlock(region.getBlockState(pos),pos);}
 @Override public boolean air(int x,int y,int z){return region.isEmptyBlock(new BlockPos(x,y,z));}
 private int top(int x,int z,boolean liquids){for(int y=region.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;y>=region.getMinBuildHeight();--y){var state=region.getBlockState(new BlockPos(x,y,z));if(state.getBlock() instanceof LeavesBlock)continue;if(SourcePhaseNativeWorld.solid(state)||liquids&&SourcePhaseNativeWorld.liquid(state))return y;}return region.getMinBuildHeight()-1;}
 @Override public int initialY(int x,int z){return top(x,z,false)+1;}@Override public int highestWaterY(int x,int z){return top(x,z,true);}
 private ChunkAccess chunk(int x,int z){var chunk=region.getChunk(x>>4,z>>4,ChunkStatus.EMPTY,false);if(chunk==null)throw new IllegalStateException("Source initial population outside generation region");return chunk;}
 @Override public int chunkAnimals(int x,int z){return SourceAnimalQuotas.get(chunk(x,z));}@Override public void chunkAnimals(int x,int z,int value){SourceAnimalQuotas.set(chunk(x,z),value);}
 @Override public Object finish(SourceSpawningKernel.Actor actor,Object data){var body=(Body)actor;var result=SourceSpawnInitialization.finish(body.mob,region,MobSpawnType.CHUNK_GENERATION,(SpawnGroupData)data);body.mob=result.actor();return result.data();}
 @Override public void spawn(SourceSpawningKernel.Actor actor){
  var mob=((Body)actor).mob;
  if(mob.isSpawnCancelled())return;
  OriginalOrigin.capture(mob);
  if(decorationCenter!=null){
   var target=chunk(mob.getBlockX(),mob.getBlockZ());
   if(target instanceof ImposterProtoChunk || target instanceof LevelChunk){
    // Native promotion loads from this carrier and uses the entity's actual position for ownership.
    decorationCenter.addEntity(mob);
    return;
   }
  }
  region.addFreshEntityWithPassengers(mob);
 }
 private final class RegionBlock implements SourceSpawningKernel.Block {
  final BlockState state;final BlockPos pos;RegionBlock(BlockState state,BlockPos pos){this.state=state;this.pos=pos;}
  public SourceSpawningKernel.Material material(){return state.isAir()?SourceSpawningKernel.Material.AIR:state.getBlock() instanceof LiquidBlock?state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)?SourceSpawningKernel.Material.WATER:state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)?SourceSpawningKernel.Material.LAVA:SourceSpawningKernel.Material.OTHER:SourceSpawningKernel.Material.OTHER;}
  public boolean fullStandardCube(){return SourcePhaseNativeWorld.solid(state)&&!(state.getBlock() instanceof SnowLayerBlock)&&state.isCollisionShapeFullBlock(region,pos);}public boolean solidAt(){return SourcePhaseNativeWorld.solid(state);}public double top(){var shape=state.getShape(region,pos);return shape.isEmpty()?0:shape.max(Direction.Axis.Y);}public boolean topFlatSolid(){return state.isFaceSturdy(region,pos,Direction.UP);}public boolean slabOrStairs(){return state.getBlock() instanceof SlabBlock||state.getBlock() instanceof StairBlock;}
  public boolean bedrockOrMantle(){return state.is(Blocks.BEDROCK)||state.getBlock() instanceof SourceMantleBlock;}public boolean myceliumOrLeaves(){return state.is(Blocks.MYCELIUM)||state.getBlock() instanceof LeavesBlock;}public boolean snowLayer(){return state.getBlock() instanceof SnowLayerBlock;}public boolean cactus(){return state.is(Blocks.CACTUS);}public boolean grass(){return state.is(Blocks.GRASS_BLOCK);}public boolean ice(){return state.is(Blocks.ICE);}
 }
}
