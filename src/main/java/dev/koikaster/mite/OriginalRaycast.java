package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** R195 Raycast selection and physical-reach policies using native voxel shapes and pose coordinates. */
public final class OriginalRaycast {
 /** of.j uses body yaw/pitch; LivingEntity.getViewVector instead reads separately updated head yaw. */
 public static Vec3 look(Player player,float partial){float pitch=partial==1?player.getXRot():net.minecraft.util.Mth.lerp(partial,player.xRotO,player.getXRot());float yaw=partial==1?player.getYRot():net.minecraft.util.Mth.lerp(partial,player.yRotO,player.getYRot());return Vec3.directionFromRotation(pitch,yaw);}
 private static BlockHitResult miss(Vec3 origin,Vec3 end){var direction=end.subtract(origin);return BlockHitResult.miss(end,Direction.getNearest(direction.x,direction.y,direction.z),BlockPos.containing(end));}
 private static boolean physicalIgnores(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){var block=state.getBlock();if(block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN))return true;if(block instanceof DoorBlock||block instanceof TrapDoorBlock||block instanceof FenceGateBlock||block instanceof FenceBlock||block instanceof IronBarsBlock)return false;if(block instanceof BushBlock||block instanceof VineBlock||block instanceof GrowingPlantBlock||block instanceof SugarCaneBlock)return true;if((block instanceof AbstractCauldronBlock||block instanceof HopperBlock)&&!level.getBlockState(pos.above()).isFaceSturdy(level,pos.above(),Direction.DOWN))return true;return !state.isSolid()||state.getCollisionShape(level,pos,context).isEmpty();}
 private static BlockHitResult single(Entity player,Vec3 origin,Vec3 end,boolean liquids,boolean physical){var context=CollisionContext.of(player);return player.level().clip(new ClipContext(origin,end,physical?ClipContext.Block.COLLIDER:ClipContext.Block.OUTLINE,liquids?ClipContext.Fluid.SOURCE_ONLY:ClipContext.Fluid.NONE,player){public VoxelShape getBlockShape(BlockState state,BlockGetter level,BlockPos pos){if(physical&&physicalIgnores(state,level,pos,context))return Shapes.empty();if(!physical&&(state.getBlock() instanceof HopperBlock||state.getBlock() instanceof AbstractCauldronBlock))return Shapes.block();return super.getBlockShape(state,level,pos);}});}
 /** Raycast.performVsBlocks perturbs X-/X+/Z-/Z+ by exactly1e-4 then compares using restored origin. */
 public static BlockHitResult blocks(Entity player,Vec3 origin,Vec3 end,boolean liquids,boolean physical){BlockHitResult best=null;double distance=0;for(var start:new Vec3[]{origin.add(-.0001,0,0),origin.add(.0001,0,0),origin.add(0,0,-.0001),origin.add(0,0,.0001)}){var hit=single(player,start,end,liquids,physical);if(hit.getType()==HitResult.Type.MISS)continue;double next=origin.distanceTo(hit.getLocation());if(best==null||next<distance){best=hit;distance=next;}}return best;}
 public static boolean physicalLine(Entity player,Vec3 origin,Vec3 end){return blocks(player,origin,end,false,true)==null;}
 public static float border(Entity target){return target instanceof net.minecraft.world.entity.decoration.HangingEntity?0:target instanceof net.minecraft.world.entity.projectile.ThrowableProjectile||target instanceof SourceLargeFireball?1f:.1f;}
 public static AABB targetBox(Player observer,Entity target){var box=target.getBoundingBox().inflate((double)border(target));if(observer.getVehicle()==target){if(target instanceof AbstractHorse)box=box.deflate(box.getXsize()*.25,box.getYsize()*.25,box.getZsize()*.25);else box=box.move(0,-.5,0);}return box;}
 private static EntityHitResult entities(Player observer, Vec3 origin, Vec3 end, boolean includeNonCollidable) {
  EntityHitResult nearest = null;
  double distance = 0;
  for (var target : observer.level().getEntities(observer, new AABB(origin, end).inflate(1), entity ->
      !entity.isSpectator() && (includeNonCollidable || entity.isPickable()
          || entity instanceof net.minecraft.world.entity.projectile.ThrowableProjectile
          || entity instanceof net.minecraft.world.entity.decoration.HangingEntity))) {
   java.util.Optional<Vec3> intercept;
   if (target instanceof GiantZombieLeg leg && leg.hasLiftedHitbox()) {
    intercept = leg.clipLifted(origin, end, border(target));
   } else {
    var box = targetBox(observer, target);
    if (box.contains(origin)) return new EntityHitResult(target, origin);
    intercept = box.clip(origin, end);
   }
   if (intercept.isEmpty()) continue;
   Vec3 point = intercept.get();
   if (point == origin) return new EntityHitResult(target, origin);
   double next = origin.distanceTo(point);
   if (nearest == null || next < distance) {
    nearest = new EntityHitResult(target, point);
    distance = next;
   }
  }
  return nearest;
 }
 public static HitResult select(Player player,float partial,boolean liquids,boolean includeNonCollidable,LegacyReach.Context entityContext){var origin=player.getEyePosition(partial);var end=origin.add(look(player,partial).scale(16));if(player.isSleeping())return miss(origin,end);var block=blocks(player,origin,end,liquids,false);var entity=entities(player,origin,end,includeNonCollidable);HitResult nearest=block;if(entity!=null){if(block==null)nearest=entity;else{double entityDistance=origin.distanceTo(entity.getLocation());float bias=-border(entity.getEntity())*3f;var physical=blocks(player,origin,end,false,true);if(physical==null||origin.distanceTo(physical.getLocation())>entityDistance+(double)(border(entity.getEntity())*3f))bias+=.75f;if(entityDistance-(double)bias<=origin.distanceTo(block.getLocation()))nearest=entity;}}if(nearest instanceof BlockHitResult hit&&!OriginalReach.canSelectBlock(player,hit.getBlockPos(),partial))nearest=null;else if(nearest instanceof EntityHitResult hit&&entityContext!=null&&!OriginalReach.canReachHit(player,hit.getEntity(),hit.getLocation(),entityContext,partial))nearest=null;return nearest==null?miss(origin,end):nearest;}
 /** Item's modern API only accepts a block result; an entity selected first becomes a miss. */
 public static BlockHitResult itemPov(Object rawLevel,Object rawPlayer,Object rawFluid){var player=(Player)rawPlayer;var hit=select(player,1,rawFluid!=ClipContext.Fluid.NONE,false,null);if(hit instanceof BlockHitResult block)return block;return miss(player.getEyePosition(),hit.getLocation());}
 /** ss item pickup policy: physical reach with leaves ignored, preserving all four narrow perturbations. */
 public static boolean pickupLine(Entity item,Vec3 from,Vec3 to){var context=CollisionContext.of(item);for(var start:new Vec3[]{from.add(-.0001,0,0),from.add(.0001,0,0),from.add(0,0,-.0001),from.add(0,0,.0001)}){var hit=item.level().clip(new ClipContext(start,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,item){public VoxelShape getBlockShape(BlockState state,BlockGetter level,BlockPos pos){if(state.getBlock() instanceof LeavesBlock||physicalIgnores(state,level,pos,context))return Shapes.empty();return super.getBlockShape(state,level,pos);}});if(hit.getType()==HitResult.Type.BLOCK)return false;}return true;}
 /** Source explosion may have no exploder; retain the physical-reach shape policy. */
 public static boolean explosionLine(Level world,Entity actor,Vec3 from,Vec3 to){
  if(actor!=null)return physicalLine(actor,from,to);var context=CollisionContext.empty();
  for(var start:new Vec3[]{from.add(-.0001,0,0),from.add(.0001,0,0),from.add(0,0,-.0001),from.add(0,0,.0001)}){
   var hit=world.clip(new ClipContext(start,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,(Entity)null){public VoxelShape getBlockShape(BlockState state,BlockGetter level,BlockPos pos){return physicalIgnores(state,level,pos,context)?Shapes.empty():super.getBlockShape(state,level,pos);}});if(hit.getType()==HitResult.Type.BLOCK)return false;
  }return true;
 }
 private OriginalRaycast(){}
}
