package dev.koikaster.mite;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** Native followThePath rounds (width+1)/2 and width/2 separately. With
 * source Cave width .98, a block-center actor is .4900000095 from the former,
 * just outside .4899999797. Its native next entity position is that same
 * block center, so MoveControl never moves and the node never advances.
 * Admit only the one-float-ULP boundary rejected by that native calculation. */
public final class SourceCaveWaypoint {
 public static void afterNativeTick(Mob mob){
  if(mob.getType()!=EntityType.CAVE_SPIDER||mob.level().isClientSide||!mob.isAlive()||!(mob.onGround()||mob.isInWater()||mob.isInLava()||mob.isPassenger()))return;
  var path=mob.getNavigation().getPath();if(path==null||path.isDone())return;
  float width=mob.getBbWidth(),tolerance=width>.75f?width/2f:.75f-width/2f;
  var node=path.getNextNodePos();double offset=(double)(width+1f)/2d;
  double dx=Math.abs(mob.getX()-(node.getX()+offset)),dz=Math.abs(mob.getZ()-(node.getZ()+offset));
  double epsilon=Math.ulp(width+1f);
  if((dx>tolerance||dz>tolerance)&&dx<=tolerance+epsilon&&dz<=tolerance+epsilon&&Math.abs(mob.getY()-node.getY())<1)path.advance();
 }
 private SourceCaveWaypoint(){}
}
