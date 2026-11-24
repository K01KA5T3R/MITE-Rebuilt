package dev.koikaster.mite;

/** uq with the actual ut virtual speed/gravity/heading overrides; shared kernels stay sealed. */
public final class SourceBottleFlight854 {
 public static void thrown(SourceSnowballRules.Host h,SourceSnowballRules.Thrower thrower){
  var s=h.state();s.owner=thrower.actor();s.x=thrower.x();s.y=thrower.y()+(double)thrower.eye();s.z=thrower.z();s.yaw=thrower.yaw();s.pitch=thrower.pitch();s.oldYaw=s.yaw;s.oldPitch=s.pitch;h.pose();
  s.x-=(double)(SourceDragonTrig.cos(s.yaw/180f*(float)Math.PI)*.16f);s.y-=(double).1f;s.z-=(double)(SourceDragonTrig.sin(s.yaw/180f*(float)Math.PI)*.16f);h.position();
  float power=.4f;s.vx=-SourceDragonTrig.sin(s.yaw/180f*(float)Math.PI)*SourceDragonTrig.cos(s.pitch/180f*(float)Math.PI)*power;
  s.vz=SourceDragonTrig.cos(s.yaw/180f*(float)Math.PI)*SourceDragonTrig.cos(s.pitch/180f*(float)Math.PI)*power;s.vy=-SourceDragonTrig.sin((s.pitch+-20f)/180f*(float)Math.PI)*power;
  SourceSnowballRules.shoot(h,s.vx,s.vy,s.vz,.7f,SourceSnowballRules.wander(thrower.player(),thrower.level(),thrower.suspended()));
 }
 public static void tick(SourceSnowballRules.Host h){
  var s=h.state();s.lastX=s.x;s.lastY=s.y;s.lastZ=s.z;h.parentTick();if(s.shake>0)--s.shake;
  if(s.embedded){if(h.block(s.tileX,s.tileY,s.tileZ)==s.inTile){if(++s.groundTicks==1200)h.discard();return;}s.embedded=false;s.vx*=(double)(h.nextFloat()*.2f);s.vy*=(double)(h.nextFloat()*.2f);s.vz*=(double)(h.nextFloat()*.2f);s.groundTicks=s.airTicks=0;}else ++s.airTicks;
  Object hit=h.raycast();if(hit!=null){if(h.portal(hit))h.enterPortal(hit);else h.impact(hit);}
  s.x+=s.vx;s.y+=s.vy;s.z+=s.vz;float horizontal=(float)Math.sqrt(s.vx*s.vx+s.vz*s.vz);s.yaw=(float)(Math.atan2(s.vx,s.vz)*180/Math.PI);s.pitch=(float)(Math.atan2(s.vy,horizontal)*180/Math.PI);
  while(s.pitch-s.oldPitch< -180)s.oldPitch-=360;while(s.pitch-s.oldPitch>=180)s.oldPitch+=360;while(s.yaw-s.oldYaw< -180)s.oldYaw-=360;while(s.yaw-s.oldYaw>=180)s.oldYaw+=360;
  s.pitch=s.oldPitch+(s.pitch-s.oldPitch)*.2f;s.yaw=s.oldYaw+(s.yaw-s.oldYaw)*.2f;float drag=.99f;
  if(h.water()){for(int i=0;i<4;i++)h.bubble(s.x-s.vx*(double).25f,s.y-s.vy*(double).25f,s.z-s.vz*(double).25f,s.vx,s.vy,s.vz);drag=.8f;}
  s.vx*=(double)drag;s.vy*=(double)drag;s.vz*=(double)drag;s.vy-=(double).07f;h.position();h.changedMotionRotation();
 }
 private SourceBottleFlight854(){}
}
