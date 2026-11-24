package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** uq uses the BLUNT policy, including old vine RNG and five-tick owner grace.
 * Native block shapes and Forge impact cancellation remain explicit adapters. */
final class SourceBrickRaycast790 {
 static BlockHitResult blocks(SourceBrick actor,Vec3 origin,Vec3 end){
  BlockHitResult best=null;double distance=0;
  for(var start:new Vec3[]{origin.add(-.0001,0,0),origin.add(.0001,0,0),origin.add(0,0,-.0001),origin.add(0,0,.0001)}){
   var hit=actor.level().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,actor){
    @Override public VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){var block=state.getBlock();
     if(block instanceof FenceGateBlock&&state.getValue(FenceGateBlock.OPEN)||block instanceof TallGrassBlock||block instanceof FlowerBlock||block instanceof CropBlock||block instanceof WebBlock)return Shapes.empty();
     if(block instanceof VineBlock){if(actor.level() instanceof ServerLevel level)OriginalRandomTables.initialize(level.getSeed());if(!(OriginalRandomTables.chance_in_2[actor.nextRaycastSeed()]&&OriginalRandomTables.chance_in_4[actor.nextRaycastSeed()]))return Shapes.empty();}
     return super.getBlockShape(state,world,pos);
    }
   });if(hit.getType()==HitResult.Type.MISS)continue;double next=origin.distanceTo(hit.getLocation());if(best==null||next<distance){best=hit;distance=next;}
  }return best;
 }
 static EntityHitResult entities(SourceBrick actor,Vec3 origin,Vec3 end,int airTicks){
  EntityHitResult best=null;double distance=0;
  for(var target:actor.level().getEntities(actor,new AABB(origin,end).inflate(1),entity->entity.isPickable()&&(entity!=actor.getOwner()||airTicks>=5))){
   float border=target instanceof HangingEntity?0:target instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile?1:.3f;
   var bounds=target.getBoundingBox().inflate((double)border);if(target==actor.getVehicle())bounds=bounds.move(0,-.5,0);
   if(bounds.contains(origin))return new EntityHitResult(target,origin);var at=bounds.clip(origin,end);if(at.isEmpty())continue;double next=origin.distanceTo(at.get());if(best==null||next<distance){best=new EntityHitResult(target,at.get());distance=next;}
  }return best;
 }
 private SourceBrickRaycast790(){}
}
