package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
/** EntityWeb's exact placement order. A web hit places a real cobweb, not a potion approximation. */
public final class SourceWebImpact {
 public static boolean entity(LivingEntity target,boolean burning){
  if(burning)target.setSecondsOnFire(5);
  Level level=target.level();int x=Mth.floor(target.getX()),y=Mth.floor(target.getY()),z=Mth.floor(target.getZ());
  var predicted=SourceArachnidPrediction.at(target,4);int px=Mth.floor(predicted.x),pz=Mth.floor(predicted.z);
  if((px!=x||pz!=z)&&place(level,new BlockPos(px,y,pz),burning))return true;
  if(place(level,new BlockPos(x,y,z),burning))return true;
  var box=target.getBoundingBox();
  for(y=Mth.floor(box.minY);y<=Mth.ceil(box.maxY)-1;y++)for(x=Mth.floor(box.minX);x<=Mth.ceil(box.maxX)-1;x++)for(z=Mth.floor(box.minZ);z<=Mth.ceil(box.maxZ)-1;z++)if(place(level,new BlockPos(x,y,z),burning))return true;
  return false;
 }
 public static boolean replaceable(Level level,BlockPos pos){var state=level.getBlockState(pos);return state.isAir()||state.is(Blocks.SNOW)&&state.getValue(SnowLayerBlock.LAYERS)==1;}
 public static boolean place(Level level,BlockPos pos,boolean burning){
  if(!replaceable(level,pos)||!level.setBlockAndUpdate(pos,Blocks.COBWEB.defaultBlockState()))return false;
  if(burning){
   var possible=new Direction[6];int count=0;
   for(var direction:new Direction[]{Direction.UP,Direction.DOWN,Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST})if(level.getBlockState(pos.relative(direction)).isAir())possible[count++]=direction;
   if(count>0){
    level.setBlockAndUpdate(pos.relative(possible[level.random.nextInt(count)]),Blocks.FIRE.defaultBlockState());
    for(var actor:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos)))actor.setSecondsOnFire(5);
   }
  }
  return true;
 }
 private SourceWebImpact(){}
}
