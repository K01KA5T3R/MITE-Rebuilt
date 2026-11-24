package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

/** aci.trySpawningHostileMobs' missing close-player branch. R195 permits
 * hostile spawns8..24 blocks away in completely dark Overworld/Underworld
 * caves. Other positions and other categories retain the normal24-block gate.
 * The native spawn-point and ticking-chunk guards below remain in force. */
public final class SourceCaveSpawnRange {
 public static boolean allow(Object rawLevel,Object rawChunk,Object rawPos,double nearestDistanceSquared,Object rawCategory){
  var level=(ServerLevel)rawLevel;var chunk=(ChunkAccess)rawChunk;var pos=(BlockPos)rawPos;
  if(rawCategory==MobCategory.MONSTER){
   double x=pos.getX()+.5,y=pos.getY(),z=pos.getZ()+.5;
   // R195 ls.d(float) floors the resolved feet. Native candidates already
   // carry their integer foot block, so the light samples start exactly here.
   var foot=pos;
   boolean close=(level.dimension()==Level.OVERWORLD||level.dimension()==WorldEvents.UNDERWORLD)
    &&playerNear(level,x,y,z,24,true)
    &&level.getMaxLocalRawBrightness(foot)==0&&level.getMaxLocalRawBrightness(foot.above())==0;
   if(playerNear(level,x,y,z,close?8:24,false))return false;
  }else if(nearestDistanceSquared<=576)return false;
  var center=new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
  if(level.getSharedSpawnPos().closerToCenterThan(center,24))return false;
  return new ChunkPos(pos).equals(chunk.getPos())||level.isNaturalSpawningAllowed(pos);
 }
 private static boolean playerNear(ServerLevel level,double x,double y,double z,double range,boolean alive){
  for(var player:level.players())if(!player.isSpectator()&&(!alive||player.isAlive())&&player.distanceToSqr(x,y,z)<range*range)return true;
  return false;
 }
 private SourceCaveSpawnRange(){}
}
