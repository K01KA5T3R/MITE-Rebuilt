package dev.koikaster.mite;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.*;
import java.util.function.Predicate;
/** Raycast.setForThrownWeb is selection-hit-liquids, NOT physical reach or visibility. */
public final class SourceWebRaycast {
 public static BlockHitResult blocks(Entity actor,Vec3 from,Vec3 to){return OriginalRaycast.blocks(actor,from,to,true,false);}
 public static EntityHitResult entities(Entity actor,Vec3 from,Vec3 to,Predicate<Entity> canHit){
  EntityHitResult closest=null;double distance=0;
  for(var target:actor.level().getEntities(actor,new AABB(from,to).inflate(1),entity->entity.isPickable()&&canHit.test(entity))){
   float border=target instanceof HangingEntity?0:actor instanceof SourceWebProjectile?.3f:.1f;
   var box=target.getBoundingBox().inflate((double)border);
   if(target==actor.getVehicle())box=target instanceof AbstractHorse?box.deflate(box.getXsize()*.25,box.getYsize()*.25,box.getZsize()*.25):box.move(0,-.5,0);
   if(box.contains(from))return new EntityHitResult(target,from);
   var hit=box.clip(from,to);if(hit.isEmpty())continue;
   double next=from.distanceTo(hit.get());if(closest==null||next<distance){closest=new EntityHitResult(target,hit.get());distance=next;}
  }
  return closest;
 }
 private SourceWebRaycast(){}
}
