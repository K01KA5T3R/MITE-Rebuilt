package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

/** Deterministic js candidate restrictions, before constructing a mob. The
 * ordinary species' own body/light checks still run at their native entry.
 * R195 nt proves tt=Spider, tg=Enderman; the outdoor roll belongs to Spider. */
final class SourceCreatureSelection {
 static boolean custom(String name,ServerLevel level,BlockPos pos,RandomSource random){
  if(!SourceSpecialSpawns.selection(name,level,pos)||!SourceAmbientSpawnSelection.accepts(name,level,pos))return false;
  return switch(name){
   case "ender_prowler","ender_sentinel"->EndLifeContent.outer(level,pos);
   case "wood_spider"->SourceArachnidAI.woodHabitat(level,pos);
   case "black_widow_spider"->random.nextFloat()>=.5f;
   case "ochre_jelly"->stoneRoof(level,pos);
   case "crimson_blob"->depth(level,pos,40)&&stoneRoof(level,pos);
   case "gray_ooze"->depth(level,pos,32)&&stoneFloor(level,pos)&&stoneRoof(level,pos);
   case "black_pudding"->depth(level,pos,24)&&stoneFloor(level,pos)&&stoneRoof(level,pos);
   default->true;
  };
 }
 static boolean spider(ServerLevel level,BlockPos pos,RandomSource random){
  return !level.dimensionType().hasSkyLight()||random.nextInt(4)!=0||!SourceSpawnOutdoors.isOutdoors(level,pos);
 }
 static boolean ghast(ServerLevel level,BlockPos pos,RandomSource random){
  boolean accepted=true;
  // js scans every nearby loaded Ghast even after a rejection, retaining RNG order.
  for(var entity:level.getAllEntities())if(entity.getType()==EntityType.GHAST
   &&entity.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<2304&&random.nextFloat()<.8f)accepted=false;
  return accepted;
 }
 private static boolean depth(ServerLevel level,BlockPos pos,int maximum){return level.dimension()!=net.minecraft.world.level.Level.OVERWORLD||pos.getY()<=maximum;}
 private static boolean stoneFloor(ServerLevel level,BlockPos pos){var block=level.getBlockState(pos.below());return block.is(Blocks.STONE)||block.is(Blocks.DEEPSLATE);}
 private static boolean stoneRoof(ServerLevel level,BlockPos pos){return !SourceSlimeSelectionRoof.accepts(level,pos);}
 private SourceCreatureSelection(){}
}
