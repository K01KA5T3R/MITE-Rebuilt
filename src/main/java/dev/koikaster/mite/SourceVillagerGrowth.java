package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** nn.pushOutOfBlocks retained NPC branch (range1), including negative-integer
 * conversion and the raw bytecode's pre-increment float range. Modern block
 * collision/full-solid queries are explicit adapters, not entity collisions. */
final class SourceVillagerGrowth {
 static int cell(double value){return value<0?(int)value-1:(int)value;}
 static boolean cauldron(net.minecraft.world.level.block.state.BlockState state){return state.getBlock() instanceof net.minecraft.world.level.block.AbstractCauldronBlock;}
 static boolean solid(SourceVillager mob,BlockPos pos){var s=mob.level().getBlockState(pos);return s.isSolidRender(mob.level(),pos)&&s.isCollisionShapeFullBlock(mob.level(),pos);}
 static boolean clear(SourceVillager mob,AABB box){return !mob.level().getBlockCollisions(mob,box).iterator().hasNext();}
 static int escape(SourceVillager mob){
  if(!(mob.level() instanceof ServerLevel level)||mob.tickCount<2)return 0;
  var box=mob.getBoundingBox();double centerY=(box.minY+box.maxY)/2;int x=cell(mob.getX()),y=cell(centerY),z=cell(mob.getZ());var center=new BlockPos(x,y,z);
  if((!solid(mob,center)||cauldron(level.getBlockState(center)))&&clear(mob,box))return 0;
  boolean[] candidates=new boolean[27];boolean possible=false;
  for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){var p=center.offset(dx,dy,dz);if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)||solid(mob,p))continue;possible=true;candidates[dx+1+(dy+1)*3+(dz+1)*9]=true;}
  if(!possible)return -1;OriginalEnvironmentPaths.initialize(mob);int index=(int)(Math.random()*2147483647d);float range=.1f;
  while((range+=.001f)<2f){double dx=OriginalRandomTables.double_1[++index&32767]*(double)range*2-(double)range,dy=OriginalRandomTables.double_1[++index&32767]*(double)range*2-(double)range,dz=OriginalRandomTables.double_1[++index&32767]*(double)range*2-(double)range;int tx=cell(mob.getX()+dx)-x,ty=cell(centerY+dy)-y,tz=cell(mob.getZ()+dz)-z;
   if(tx < -1||tx>1||ty < -1||ty>1||tz < -1||tz>1||!candidates[tx+1+(ty+1)*3+(tz+1)*9]||!clear(mob,box.move(dx,dy,dz)))continue;
   mob.setPos(mob.getX()+dx,mob.getY()+dy,mob.getZ()+dz);mob.setDeltaMovement(Vec3.ZERO);mob.xo=mob.xOld=mob.getX();mob.yo=mob.yOld=mob.getY();mob.zo=mob.zOld=mob.getZ();level.getChunkSource().broadcast(mob,new ClientboundTeleportEntityPacket(mob));return 1;
  }return -1;
 }
 private SourceVillagerGrowth(){}
}
