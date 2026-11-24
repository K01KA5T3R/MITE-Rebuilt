package dev.koikaster.mite;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** V007: a finite initial garrison and treasure, owned by native FEATURES
 * generation. Chunk status is the persisted generation ledger; no load/refill
 * event exists. The original three elders and all combat/drop rules are retained. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class OceanMonumentContent395 {
 public static final String ORIGIN="MiteMonument395";
 private static boolean restocking(EntityType<?> type,MobSpawnType reason){return type==EntityType.GUARDIAN&&(reason==MobSpawnType.NATURAL||reason==MobSpawnType.CHUNK_GENERATION);}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void position(MobSpawnEvent.PositionCheck e){if(restocking(e.getEntity().getType(),e.getSpawnType()))e.setResult(Event.Result.DENY);}
 @SubscribeEvent(priority=EventPriority.LOWEST)public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn e){if(restocking(e.getEntity().getType(),e.getSpawnType()))e.setSpawnCancelled(true);}
 public static BlockPos position(StructurePiece piece,int x,int y,int z){
  var d=piece.getOrientation();var box=piece.getBoundingBox();if(d==null)return new BlockPos(x,y,z);
  int wx=switch(d){case NORTH,SOUTH->box.minX()+x;case WEST->box.maxX()-z;case EAST->box.minX()+z;default->x;};
  int wz=switch(d){case NORTH->box.maxZ()-z;case SOUTH->box.minZ()+z;case WEST,EAST->box.minZ()+x;default->z;};
  return new BlockPos(wx,box.minY()+y,wz);
 }
 public static UUID guardianId(WorldGenLevel level,StructurePiece piece,int index){return UUID.nameUUIDFromBytes((level.getSeed()+":"+piece.getBoundingBox().minX()+":"+piece.getBoundingBox().minZ()+":monument395:"+index).getBytes(StandardCharsets.UTF_8));}
 public static void finish(StructurePiece piece,WorldGenLevel level,BoundingBox clip){
  if(level.getLevel().dimension()!=Level.OVERWORLD)return;
  chest(piece,level,clip,12,3,5);chest(piece,level,clip,45,3,5);
  if(level.getDifficulty()==Difficulty.PEACEFUL)return;
  int index=0;for(int x:new int[]{5,12,19,38,45,52})for(int z:new int[]{4,8,12,16}){
   int id=index++;var column=position(piece,x,2,z);if(!clip.isInside(column))continue;
   var mob=EntityType.GUARDIAN.create(level.getLevel());if(mob==null)continue;
   for(int y=2;y<=7;y++){
    var pos=position(piece,x,y,z);if(!clip.isInside(pos)||!level.getFluidState(pos).is(FluidTags.WATER)||!level.getFluidState(pos.above()).is(FluidTags.WATER))continue;
    mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);if(!level.noCollision(mob,mob.getBoundingBox()))continue;
    mob.setUUID(guardianId(level,piece,id));mob.setPersistenceRequired();mob.getPersistentData().putLong(ORIGIN,position(piece,0,0,0).asLong());
    mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.STRUCTURE,null,null);level.addFreshEntityWithPassengers(mob);break;
   }
  }
 }
 private static void chest(StructurePiece piece,WorldGenLevel level,BoundingBox clip,int x,int y,int z){
  var pos=position(piece,x,y,z);if(!clip.isInside(pos)||level.getBlockState(pos).is(Blocks.CHEST))return;
  level.setBlock(pos.below(),Blocks.PRISMARINE_BRICKS.defaultBlockState(),2);
  level.setBlock(pos.above(),Blocks.WATER.defaultBlockState(),2);
  var facing=piece.getOrientation()==null?Direction.NORTH:piece.getOrientation();
  if(level.setBlock(pos,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,facing).setValue(ChestBlock.WATERLOGGED,true),2))RandomizableContainerBlockEntity.setLootTable(level,net.minecraft.util.RandomSource.create(level.getSeed()^pos.asLong()),pos,Content.id("chests/ocean_monument"));
 }
 private OceanMonumentContent395(){}
}
