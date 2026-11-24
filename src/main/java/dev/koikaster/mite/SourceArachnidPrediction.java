package dev.koikaster.mite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
/** nn.getPredictedPosX/Z. ServerPlayer must receive the source packet-motion bridge before wiring. */
public final class SourceArachnidPrediction {
 public static Vec3 at(LivingEntity target,float lead){
  var motion=target.getDeltaMovement();double dx=motion.x,dz=motion.z;
  if(target instanceof ServerPlayer){
   if(!(target instanceof SourcePlayerMotion source))throw new IllegalStateException("Source player packet-motion bridge is not installed");
   double receivedX=source.mite$lastReceivedMotionX(),receivedZ=source.mite$lastReceivedMotionZ();
   if(Math.abs(receivedX)<=1)dx=receivedX;if(Math.abs(receivedZ)<=1)dz=receivedZ;
  }
  return new Vec3(target.getX()+dx*(double)lead,target.getY(),target.getZ()+dz*(double)lead);
 }
 private SourceArachnidPrediction(){}
}
