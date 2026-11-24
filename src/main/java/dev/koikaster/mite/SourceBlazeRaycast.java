package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** R195 piercing policy: liquids/reeds/vines/metal bars/open gates ignored;
 * source11 leaves use the two short-circuited lookup tables. The td line check
 * has a null originator, so leaves cannot impede that particular query. Native
 * outline shapes adapt modern blocks; source four perturbations remain. */
public final class SourceBlazeRaycast {
 public interface Seed {int nextRaycastSeed();}
 public static BlockHitResult blocks(Entity geometry,Vec3 origin,Vec3 end,Seed seed){
  BlockHitResult best=null;double distance=0;
  for(var start:new Vec3[]{origin.add(-.0001,0,0),origin.add(.0001,0,0),origin.add(0,0,-.0001),origin.add(0,0,.0001)}){
   var hit=geometry.level().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,geometry){
    @Override public VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){
     var block=state.getBlock();
     if(block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN)||block instanceof BushBlock||block instanceof SugarCaneBlock||block instanceof VineBlock||block instanceof GrowingPlantBlock||state.is(Blocks.IRON_BARS))return Shapes.empty();
     if(block instanceof LeavesBlock&&(seed==null||!(OriginalRandomTables.chance_in_10[seed.nextRaycastSeed()]&&OriginalRandomTables.chance_in_4[seed.nextRaycastSeed()])))return Shapes.empty();
     return super.getBlockShape(state,world,pos);
    }
   });
   if(hit.getType()==HitResult.Type.MISS)continue;double next=origin.distanceTo(hit.getLocation());if(best==null||next<distance){best=hit;distance=next;}
  }return best;
 }
 public static EntityHitResult entities(SourceSmallFireball actor,Vec3 origin,Vec3 end,int airTicks){
  EntityHitResult closest=null;double distance=0;
  for(var target:actor.level().getEntities(actor,new AABB(origin,end).inflate(1),entity->entity.isPickable()&&(entity!=actor.getOwner()||airTicks>=25))){
   float border=target instanceof HangingEntity?0:target instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile?1:.3f;
   var box=target.getBoundingBox().inflate((double)border);if(target==actor.getVehicle())box=box.move(0,-.5,0);
   if(box.contains(origin))return new EntityHitResult(target,origin);var intercept=box.clip(origin,end);if(intercept.isEmpty())continue;double next=origin.distanceTo(intercept.get());if(closest==null||next<distance){closest=new EntityHitResult(target,intercept.get());distance=next;}
  }return closest;
 }
 private SourceBlazeRaycast(){}
}
