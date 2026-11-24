package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Direct afp room geometry and ChunkProviderUnderworld.populate's sixteen
 * attempts, before the biome decorator. Modern feature RNG is owned by each
 * decoration invocation instead of the old provider's shared mutable Random. */
public final class SourceUnderworldDungeons extends Feature<NoneFeatureConfiguration> {
 public SourceUnderworldDungeons(){super(NoneFeatureConfiguration.CODEC);}
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
  var level=context.level();if(level.getLevel().dimension()!=WorldEvents.UNDERWORLD)return false;
  var origin=context.origin();var random=context.random();boolean generated=false;
  var attempts=Boolean.getBoolean("mite.sourcePortValidation")?new ArrayList<List<Integer>>():null;
  var rooms=attempts==null?null:new ArrayList<List<Integer>>();
  boolean enhanced=UnderworldExplorationRules.enabled(level.getLevel());
  int attemptsCount=UnderworldExplorationRules.ORIGINAL_DUNGEON_ATTEMPTS;
  if(enhanced)attemptsCount+=UnderworldExplorationRules.EXTRA_DUNGEON_ATTEMPTS;
  for(int i=0;i<attemptsCount;i++){
   int x=origin.getX()+random.nextInt(16)+8,y=random.nextInt(32)+140,z=origin.getZ()+random.nextInt(16)+8;
   if(attempts!=null)attempts.add(List.of(x,y,z));
   if(room(level,random,new BlockPos(x,y,z),enhanced)){generated=true;if(rooms!=null)rooms.add(List.of(x,y,z));}
  }
  if(attempts!=null)SourceDungeonTrace.record(level.getLevel(),origin,attempts,rooms);
  return generated;
 }
 private static boolean solid(WorldGenLevel world,BlockPos pos){return SourcePortalMaterials.solid(world.getBlockState(pos));}
 static EntityType<? extends SourceLongdead> spawner(RandomSource random){return random.nextInt(6)==0?SourceLongdeadContent.GUARDIAN.get():SourceLongdeadContent.LONGDEAD.get();}
 static boolean room(WorldGenLevel world,RandomSource random,BlockPos center){
  return room(world,random,center,false);
 }
 private static boolean room(WorldGenLevel world,RandomSource random,BlockPos center,boolean enhanced){
  int x=center.getX(),y=center.getY(),z=center.getZ(),radiusX=random.nextInt(2)+2,radiusZ=random.nextInt(2)+2,openings=0;
  if(enhanced&&world instanceof WorldGenRegion region&&touchesCompletedChunk(region,center,radiusX,radiusZ))return false;
  for(int xx=x-radiusX-1;xx<=x+radiusX+1;xx++)for(int yy=y-1;yy<=y+4;yy++)for(int zz=z-radiusZ-1;zz<=z+radiusZ+1;zz++){
   var pos=new BlockPos(xx,yy,zz);
   if(enhanced&&protectedBlock(world.getBlockState(pos)))return false;
   if(yy==y-1&&!solid(world,pos))return false;
   if(yy==y+4&&(!solid(world,pos)||world.getBlockState(pos).getBlock() instanceof FallingBlock))return false;
   if((xx==x-radiusX-1||xx==x+radiusX+1||zz==z-radiusZ-1||zz==z+radiusZ+1)&&yy==y&&world.isEmptyBlock(pos)&&world.isEmptyBlock(pos.above()))openings++;
  }
  if(openings<1||openings>5)return false;
  for(int xx=x-radiusX-1;xx<=x+radiusX+1;xx++)for(int yy=y+3;yy>=y-1;yy--)for(int zz=z-radiusZ-1;zz<=z+radiusZ+1;zz++){
   var pos=new BlockPos(xx,yy,zz);
   if(xx!=x-radiusX-1&&yy!=y-1&&zz!=z-radiusZ-1&&xx!=x+radiusX+1&&yy!=y+4&&zz!=z+radiusZ+1){world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);continue;}
   if(yy>=world.getMinBuildHeight()&&!solid(world,pos.below())){world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);continue;}
   if(!solid(world,pos))continue;
   world.setBlock(pos,(yy==y-1&&random.nextInt(4)!=0?Blocks.MOSSY_COBBLESTONE:Blocks.COBBLESTONE).defaultBlockState(),2);
  }
  for(int chest=0;chest<2;chest++)for(int attempt=0;attempt<3;attempt++){
   var pos=new BlockPos(x+random.nextInt(radiusX*2+1)-radiusX,y,z+random.nextInt(radiusZ*2+1)-radiusZ);
   if(!world.isEmptyBlock(pos))continue;int walls=0;Direction facing=null;
   for(var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH})if(solid(world,pos.relative(direction))){walls++;facing=direction.getOpposite();}
   if(walls!=1)continue;
   world.setBlock(pos,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,facing),2);
   if(world.getBlockEntity(pos) instanceof Container inventory){
    SourceDungeonLoot.fill(inventory,random);
    if(enhanced)UnderworldExplorationRewards.dungeon(world.getLevel(),pos,inventory);
   }
   break;
  }
  world.setBlock(center,Blocks.SPAWNER.defaultBlockState(),2);
  if(world.getBlockEntity(center) instanceof SpawnerBlockEntity entity)entity.setEntityId(spawner(random),random);
  else throw new IllegalStateException("Original Underworld room lost its native spawner at "+center);
  return true;
 }
 static boolean touchesCompletedChunk(WorldGenRegion region,BlockPos center,int radiusX,int radiusZ){
  // Region writes can report success even when an already completed neighbor declines the block write.
  int minX=(center.getX()-radiusX-1)>>4,maxX=(center.getX()+radiusX+1)>>4;
  int minZ=(center.getZ()-radiusZ-1)>>4,maxZ=(center.getZ()+radiusZ+1)>>4;
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
   if(region.getChunk(x,z).getStatus()==ChunkStatus.FULL)return true;
  }
  return false;
 }
 private static boolean protectedBlock(BlockState state){
  return state.hasBlockEntity()||state.is(BlockTags.RAILS)||state.is(BlockTags.WOODEN_FENCES)
   ||state.is(BlockTags.PLANKS)||state.is(BlockTags.LOGS);
 }
}
