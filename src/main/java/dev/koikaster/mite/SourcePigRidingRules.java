package dev.koikaster.mite;

/** Literal pl controller decisions/math and ws right-click gate. The physical
 * travel, source path queries, item wear and rider transport are explicit leaves. */
public final class SourcePigRidingRules {
 public static final float LIMIT=.3f;
 public static final class State {public float speed;public boolean boosting;public int elapsed,duration;}
 public interface Host {
  State state();boolean alive();boolean rider();boolean stick();boolean creative();float riderYaw();float yaw();void yaw(float value);
  double x();double y();double z();boolean grounded();float friction(int x,int y,int z);float aiSpeed();float width();float height();float riderHeight();
  boolean air(int x,int y,int z);boolean stairOrBottomSlab(int x,int y,int z);int path(int x,int y,int z,int width,int height,int depth);void jump();float nextFloat();int nextInt(int bound);void nibble(int amount);void move(float forward);
 }
 public interface UseHost {boolean ridingPig();int remaining();boolean server();boolean boostAllowed();void boost();void damage(int amount);}
 public static boolean use(UseHost h){if(h.ridingPig()&&h.remaining()>=7){if(h.server()&&h.boostAllowed()){h.boost();h.damage(7);}return true;}return false;}
 public static boolean canUse(Host h){return h.alive()&&h.rider()&&(h.state().boosting||h.stick());}
 public static void start(State s){s.speed=0;}
 public static void stop(State s){s.boosting=false;s.speed=0;}
 public static boolean canBoost(State s){return !s.boosting&&s.speed>LIMIT*.3f;}
 public static void boost(Host h){var s=h.state();s.boosting=true;s.elapsed=0;s.duration=h.nextInt(841)+140;}
 public static void tick(Host h){
  var s=h.state();float turn=wrap(h.riderYaw()-h.yaw())*.5f;if(turn>5)turn=5;if(turn< -5)turn=-5;h.yaw(wrap(h.yaw()+turn));
  if(s.speed<LIMIT)s.speed+=(LIMIT-s.speed)*.01f;if(s.speed>LIMIT)s.speed=LIMIT;
  int x=floor(h.x()),y=floor(h.y()),z=floor(h.z());float forward=s.speed;
  if(s.boosting){if(s.elapsed++>s.duration)s.boosting=false;forward+=forward*1.15f*SourceDragonTrig.sin((float)s.elapsed/(float)s.duration*(float)Math.PI);}
  float friction=.91f;if(h.grounded())friction=h.friction(floorFloat((float)x),floorFloat((float)y)-1,floorFloat((float)z))*.91f;
  float scale=.16277136f/(friction*friction*friction),sin=SourceDragonTrig.sin(h.yaw()*(float)Math.PI/180f),cos=SourceDragonTrig.cos(h.yaw()*(float)Math.PI/180f),base=h.aiSpeed()*scale,divisor=Math.max(forward,1f);divisor=base/divisor;float step=forward*divisor,dx=-(step*sin),dz=step*cos;
  if(Math.abs(dx)>Math.abs(dz)){if(dx<0)dx-=h.width()/2f;if(dx>0)dx+=h.width()/2f;dz=0;}else{dx=0;if(dz<0)dz-=h.width()/2f;if(dz>0)dz+=h.width()/2f;}
  int nx=floor(h.x()+(double)dx),nz=floor(h.z()+(double)dz),width=floorFloat(h.width()+1f),height=floorFloat(h.height()+h.riderHeight()+1f),depth=floorFloat(h.width()+1f);
  if(x!=nx||z!=nz){boolean special=h.stairOrBottomSlab(x,y,z)||h.air(x,y,z)&&h.stairOrBottomSlab(x,y-1,z);if(!special&&h.path(nx,y,nz,width,height,depth)==0&&h.path(x,y+1,z,width,height,depth)==1&&h.path(nx,y+1,nz,width,height,depth)==1)h.jump();}
  if(!h.creative()&&s.speed>=LIMIT*.5f&&h.nextFloat()<.006f&&!s.boosting&&h.stick())h.nibble(1);
  h.move(forward);
 }
 private static int floor(double v){int i=(int)v;return v<(double)i?i-1:i;}private static int floorFloat(float v){int i=(int)v;return v<(float)i?i-1:i;}
 private static float wrap(float v){v%=360;if(v>=180)v-=360;if(v< -180)v+=360;return v;}
 private SourcePigRidingRules(){}
}
