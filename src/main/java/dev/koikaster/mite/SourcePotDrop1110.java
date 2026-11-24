package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Selected ordinary pot drop: aqz chance, BlockBreakInfo half-bounds, ss launch and delay10. */
public final class SourcePotDrop1110 {
 private static final ThreadLocal<SourceDropLaunch1110.State> active=new ThreadLocal<>();
 private static boolean constructing(Object item){return item instanceof Drop&&active.get()!=null;}
 public static float nextFloat(Object random,Object item){return constructing(item)?0f:((RandomSource)random).nextFloat();}
 public static float bob(float value,Object item){return constructing(item)?active.get().bob():value;}
 public static float yaw(float value,Object item){return constructing(item)?active.get().yaw():value;}
 private static final class Drop extends ItemEntity {
  Drop(ServerLevel level,double x,double y,double z,ItemStack stack,SourceDropLaunch1110.State launch){super(level,x,y,z,stack,launch.vx(),launch.vy(),launch.vz());}
 }
 public static ItemEntity create(ServerLevel level,double x,double y,double z,ItemStack stack){
  var previous=active.get();var launch=SourceDropLaunch1110.sample(true,Math::random);active.set(launch);
  try{return new Drop(level,x,y,z,stack,launch);}finally{if(previous==null)active.remove();else active.set(previous);}
 }
 public static int emit(ServerLevel level,BlockPos pos,BlockState state,ItemStack sample){
  if(sample.isEmpty()||!(level.random.nextFloat()<1f))return 0;
  if(level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)){
   var shape=state.getShape(level,pos);var bounds=shape.isEmpty()?new AABB(0,0,0,1,1,1):shape.bounds();
   double cx=(bounds.minX+bounds.maxX)*.5,cy=(bounds.minY+bounds.maxY)*.5,cz=(bounds.minZ+bounds.maxZ)*.5;
   double minX=cx+(bounds.minX-cx)*.5,minY=cy+(bounds.minY-cy)*.5,minZ=cz+(bounds.minZ-cz)*.5,maxX=cx+(bounds.maxX-cx)*.5,maxY=cy+(bounds.maxY-cy)*.5,maxZ=cz+(bounds.maxZ-cz)*.5;
   var item=create(level,pos.getX()+level.random.nextFloat()*(maxX-minX)+minX,pos.getY()+level.random.nextFloat()*(maxY-minY)+minY,pos.getZ()+level.random.nextFloat()*(maxZ-minZ)+minZ,sample.copy());item.setDefaultPickUpDelay();level.addFreshEntity(item);
  }
  return 1;
 }
 private SourcePotDrop1110(){}
}
