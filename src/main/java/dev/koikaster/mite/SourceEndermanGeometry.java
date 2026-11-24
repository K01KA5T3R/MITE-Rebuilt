package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.ForgeEventFactory;
/** tg.j/c/bT. V103 retains modern terrain bounds; source landing/collision order stays. */
public final class SourceEndermanGeometry {
 static boolean solid(SourceEnderman actor,BlockPos pos){var state=actor.level().getBlockState(pos);return !state.isAir()&&!state.liquid()&&!state.getCollisionShape(actor.level(),pos,CollisionContext.of(actor)).isEmpty();}
 static boolean passable(SourceEnderman actor,BlockPos pos){return actor.level().isOutsideBuildHeight(pos)||actor.level().hasChunkAt(pos)&&actor.level().getFluidState(pos).isEmpty()&&!solid(actor,pos);}
 static double collisionTop(SourceEnderman actor,BlockPos pos){var at=pos;var shape=actor.level().getBlockState(at).getCollisionShape(actor.level(),at,CollisionContext.of(actor));if(shape.isEmpty()){at=at.below();shape=actor.level().getBlockState(at).getCollisionShape(actor.level(),at,CollisionContext.of(actor));}return shape.isEmpty()?at.getY():at.getY()+shape.max(Direction.Axis.Y);}
 static boolean valuable(SourceEnderman actor){if(actor.sourceWet()||actor.isOnFire())return false;var item=actor.valuables().nearest();if(item==null)return false;var p=item.blockPosition();return teleport(actor,p.getX()+.5,collisionTop(actor,p),p.getZ()+.5);}
 static boolean random(SourceEnderman actor){if(actor.sourceDecoy())return false;if(valuable(actor))return true;var r=actor.getRandom();double x=actor.getX()+(r.nextDouble()-.5)*64,y=actor.getY()+r.nextInt(64)-32,z=actor.getZ()+(r.nextDouble()-.5)*64;return teleport(actor,x,y,z);}
 static boolean toward(SourceEnderman actor,Entity target){var r=actor.getRandom();int x=target.getBlockX()+r.nextInt(7)-3,y=target.getBlockY()+3,z=target.getBlockZ()+r.nextInt(7)-3;for(int dy=0;dy>=-6&&passable(actor,new BlockPos(x,y-1,z));--dy)--y;return teleport(actor,(float)x+.5f,(float)y+.1f,(float)z+.5f);}
 static boolean teleport(SourceEnderman actor,double x,double y,double z){
  if(actor.level().isClientSide||actor.isRemoved())return false;
  // Forge cancellation is honored before changing native position or sending effects.
  var event=ForgeEventFactory.onEnderTeleport(actor,x,y,z);if(event.isCanceled())return false;x=event.getTargetX();y=event.getTargetY();z=event.getTargetZ();
  var old=actor.position();var pos=BlockPos.containing(x,y,z);boolean supported=false;
  if(actor.level().hasChunkAt(pos)&&!actor.level().isOutsideBuildHeight(pos))while(pos.getY()>actor.level().getMinBuildHeight()){if(solid(actor,pos.below())){supported=true;break;}y-=1;pos=pos.below();}
  if(!supported)return false;actor.setPos(x,y,z);
  if(!actor.level().noCollision(actor,actor.getBoundingBox())||actor.level().containsAnyLiquid(actor.getBoundingBox())){actor.setPos(old.x,old.y,old.z);return false;}
  var end=actor.blockPosition();int count=(int)(8*actor.position().distanceTo(old));Network.phaseTrail(actor,end,(short)count,BlockPos.containing(old));Network.phaseTrail(actor,end.above(),(short)count,BlockPos.containing(old.add(0,1,0)));
  if(!actor.isSilent())actor.level().playSound(null,old.x,old.y,old.z,SoundEvents.ENDERMAN_TELEPORT,actor.getSoundSource(),1,1);
  return true;
 }
 private SourceEndermanGeometry(){}
}
