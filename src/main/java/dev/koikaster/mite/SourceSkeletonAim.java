package dev.koikaster.mite;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/** ka's last received movement and nn/uh's skeleton leading/trajectory. No saved
 * fields or player choices. A new packet call site only supplies missing1.20 API. */
public final class SourceSkeletonAim {
 private record Motion(double x,double z){}
 private static final Map<ServerPlayer,Motion> RECEIVED=new WeakHashMap<>();
 public static void received(Object rawPlayer,Object rawPacket){
  var player=(ServerPlayer)rawPlayer;if(!player.server.isSameThread())return;
  var packet=(ServerboundMovePlayerPacket)rawPacket;
  RECEIVED.put(player,new Motion(packet.hasPosition()?packet.getX(player.getX())-player.getX():0,packet.hasPosition()?packet.getZ(player.getZ())-player.getZ():0));
 }
 static Vec3 prediction(LivingEntity target,float lead){
  var velocity=target.getDeltaMovement();var motion=target instanceof ServerPlayer player?RECEIVED.getOrDefault(player,new Motion(0,0)):null;
  double x=motion!=null&&Math.abs(motion.x())<=1?motion.x():velocity.x,z=motion!=null&&Math.abs(motion.z())<=1?motion.z():velocity.z;
  return target.position().add(x*lead,0,z*lead);
 }
 static void launch(AbstractArrow arrow,net.minecraft.world.entity.monster.AbstractSkeleton shooter,LivingEntity target){
  // Original uh owns a fresh entity RNG, separate from the shooter's loot RNG.
  var random=RandomSource.create();double initialX=target.getX()-shooter.getX(),initialZ=target.getZ()-shooter.getZ();
  float lead=(float)Math.pow(initialX*initialX+initialZ*initialZ,.44);lead*=.5f+random.nextFloat();
  var predicted=prediction(target,lead);double dx=predicted.x-shooter.getX(),dz=predicted.z-shooter.getZ();
  double y=shooter.getY()+shooter.getEyeHeight()-(double).1f,dy=target.getBoundingBox().minY+target.getBbHeight()/3f-y;
  double distanceSquared=dx*dx+dz*dz,horizontal=(float)Math.sqrt(distanceSquared);
  if(horizontal<1e-7)return;
  arrow.setPos(shooter.getX()+dx/horizontal,y,shooter.getZ()+dz/horizontal);
  double vertical=dy+(float)horizontal*.2f;float length=(float)Math.sqrt(dx*dx+vertical*vertical+dz*dz);float scatter=(14-shooter.level().getDifficulty().getId()*4)*1.5f;
  dx/=length;vertical/=length;dz/=length;
  dx+=random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075f*scatter;
  vertical+=random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075f*scatter;
  dz+=random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075f*scatter;
  double correction=distanceSquared*(double).0005f*(distanceSquared*(double).0005f)-(double).05f;
  if(distanceSquared>576)correction+=(double).06f;
  float rise=(float)target.getY()-(float)shooter.getY();if(rise>5)correction+=(double)((rise-5)*.025f)*((double)1.2f-distanceSquared*(double).0005f);
  arrow.setDeltaMovement(dx*(double)1.6f,vertical*(double)1.6f+correction,dz*(double)1.6f);
 }
 private SourceSkeletonAim(){}
}
