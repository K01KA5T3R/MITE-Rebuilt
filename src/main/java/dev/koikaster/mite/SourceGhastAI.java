package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Literal tj.bl state order. Native collision shapes and source player-motion bridge adapt the platform. */
public final class SourceGhastAI implements OriginalParentHarmState.Flags {
 private final SourceGhast actor;
 public final OriginalParentHarmState harm;
 int moveTimer,searchTimer,previousCharge,charge;
 double waypointX,waypointY,waypointZ;
 private boolean decided,fleeing;
 SourceGhastAI(SourceGhast actor){this.actor=actor;harm=new OriginalParentHarmState(actor,this);}
 @Override public boolean hasDecidedToFlee(){return decided;}
 @Override public void hasDecidedToFlee(boolean value){decided=value;}
 @Override public boolean fleeing(){return fleeing;}
 @Override public void fleeing(boolean value){fleeing=value;}
 void step(){
  var world=actor.level();var random=actor.getRandom();if(world.getDifficulty()==Difficulty.PEACEFUL)actor.discard();OriginalDespawn.tryDespawn(actor);previousCharge=charge;
  double dx=waypointX-actor.getX(),dy=waypointY-actor.getY(),dz=waypointZ-actor.getZ(),squared=dx*dx+dy*dy+dz*dz;
  if(squared<1||squared>3600){waypointX=actor.getX()+(double)((random.nextFloat()*2-1)*16);waypointY=actor.getY()+(double)((random.nextFloat()*2-1)*16);waypointZ=actor.getZ()+(double)((random.nextFloat()*2-1)*16);}
  if(moveTimer--<=0){moveTimer+=random.nextInt(5)+2;double length=(float)Math.sqrt(squared);if(routeClear(length)){
   // Preserve the original pre-selection delta. Guard only the original 0/0 case.
   if(length>0&&Double.isFinite(length))actor.setDeltaMovement(actor.getDeltaMovement().add(dx/length*.1,dy/length*.1,dz/length*.1));
  }else{waypointX=actor.getX();waypointY=actor.getY();waypointZ=actor.getZ();}}
  var target=actor.getTarget();if(target!=null&&target.isRemoved()){actor.setTarget(null);target=null;}
  if(target==null||searchTimer--<=0){target=findPlayer();actor.setTarget(target);if(target!=null)searchTimer=20;}
  if(target!=null&&target.distanceToSqr(actor)<4096){
   var center=target.position().add(0,target.getBbHeight()*.5f,0);var origin=actor.position().add(0,actor.getBbHeight()*.5f,0);var delta=center.subtract(origin);float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90;actor.setYRot(yaw);actor.setYBodyRot(yaw);
   if(actor.hasLineOfSight(target)){
    if(charge==10)world.levelEvent(null,1015,BlockPos.containing(actor.getX(),actor.getY(),actor.getZ()),0);
    if(++charge==20){float lead=(float)Math.pow(origin.distanceToSqr(center),.44);lead*=.5f+random.nextFloat();var predicted=SourceArachnidPrediction.at(target,lead);center=new Vec3(predicted.x,center.y,predicted.z);
     world.levelEvent(null,1016,BlockPos.containing(actor.getX(),actor.getY(),actor.getZ()),0);world.addFreshEntity(new SourceLargeFireball(world,actor,center,4,actor.getExplosionPower()));charge=-40;
    }
   }else if(charge>0)--charge;
  }else{float yaw=-(float)Math.atan2(actor.getDeltaMovement().x,actor.getDeltaMovement().z)*180/(float)Math.PI;actor.setYRot(yaw);actor.setYBodyRot(yaw);if(charge>0)--charge;}
  actor.setCharging(charge>10);
 }
 boolean routeClear(double length){var step=new Vec3((waypointX-actor.getX())/length,(waypointY-actor.getY())/length,(waypointZ-actor.getZ())/length);var box=actor.getBoundingBox();for(int i=1;i<length;i++){box=box.move(step);if(!actor.level().noCollision(actor,box))return false;}return true;}
 Player findPlayer(){Player nearest=null;double shortest=-1;for(var player:actor.level().players()){if(player.isSpectator()||player.getAbilities().invulnerable||!player.isAlive())continue;double limit=100;if(player.isCrouching())limit*=(double).8f;if(player.isInvisible())limit*=(double)(.7f*Math.max(.1f,player.getArmorCoverPercentage()));double distance=player.distanceToSqr(actor.getX(),actor.getY()+(double)(actor.getBbHeight()/2f),actor.getZ());if(distance>=limit*limit||shortest!=-1&&distance>=shortest||!actor.hasLineOfSight(player))continue;nearest=player;shortest=distance;}return nearest;}
}
