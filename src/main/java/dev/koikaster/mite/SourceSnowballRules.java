package dev.koikaster.mite;

/** The retained uq trajectory and up impact. World geometry, parent Entity
 * tick, registry IDs, packets and item interactions are explicit transports. */
public final class SourceSnowballRules {
 public static final class State {
  public double x,y,z,vx,vy,vz,lastX,lastY,lastZ;
  public float yaw,pitch,oldYaw,oldPitch;
  public int tileX=-1,tileY=-1,tileZ=-1,inTile,shake,groundTicks,airTicks;
  public boolean embedded;public Object owner;public String ownerName;
 }
 public record Thrower(Object actor,double x,double y,double z,float yaw,float pitch,float eye,boolean player,int level,boolean suspended){}
 public interface Host {
  State state();void pose();void position();void changedMotionRotation();void parentTick();
  double gaussian();float nextFloat();boolean server();int block(int x,int y,int z);
  Object raycast();boolean portal(Object hit);void enterPortal(Object hit);void impact(Object hit);
  boolean water();void bubble(double x,double y,double z,double vx,double vy,double vz);void discard();
  Object findPlayer(String name);String playerName(Object actor);
 }
 public interface Tags {
  void putShort(String name,short value);void putByte(String name,byte value);void putString(String name,String value);
  short getShort(String name);byte getByte(String name);String getString(String name);
 }
 public interface Impact {
  boolean server();boolean entity();boolean blazeOrFireElemental();boolean magmaOrNetherspawn();
  void quench();void damage(float amount);boolean burning();double globalRandom();void extinguish();
  boolean magmaEarth();void normalizeEarth();boolean neighborFire();void douseNeighbor();void blockCallback();void poof();void discard();
 }
 public interface ItemUse {
  boolean client();boolean creative();void bob();void consume();float itemRandom();void bow(float volume,float pitch);void spawn();
 }
 public static float wander(boolean player,int level,boolean suspended){float value=player?(level<0?5+(float)level*-.5f:(float)(.5+4.5/Math.pow(.8f+(float)(level+1)/5f,2))):1;if(suspended)value*=2;return value;}
 public static void thrown(Host h,Thrower thrower){
  var s=h.state();s.owner=thrower.actor();s.x=thrower.x();s.y=thrower.y()+(double)thrower.eye();s.z=thrower.z();s.yaw=thrower.yaw();s.pitch=thrower.pitch();s.oldYaw=s.yaw;s.oldPitch=s.pitch;h.pose();
  s.x-=(double)(SourceDragonTrig.cos(s.yaw/180f*(float)Math.PI)*.16f);s.y-=(double).1f;s.z-=(double)(SourceDragonTrig.sin(s.yaw/180f*(float)Math.PI)*.16f);h.position();
  float power=.4f;s.vx=-SourceDragonTrig.sin(s.yaw/180f*(float)Math.PI)*SourceDragonTrig.cos(s.pitch/180f*(float)Math.PI)*power;
  s.vz=SourceDragonTrig.cos(s.yaw/180f*(float)Math.PI)*SourceDragonTrig.cos(s.pitch/180f*(float)Math.PI)*power;s.vy=-SourceDragonTrig.sin((s.pitch+0)/180f*(float)Math.PI)*power;
  shoot(h,s.vx,s.vy,s.vz,1.5f,wander(thrower.player(),thrower.level(),thrower.suspended()));
 }
 public static void shoot(Host h,double x,double y,double z,float velocity,float wander){
  var s=h.state();float length=(float)Math.sqrt(x*x+y*y+z*z);x/=(double)length;y/=(double)length;z/=(double)length;
  x+=h.gaussian()*(double).0075f*(double)wander;y+=h.gaussian()*(double).0075f*(double)wander;z+=h.gaussian()*(double).0075f*(double)wander;
  s.vx=x*=(double)velocity;s.vy=y*=(double)velocity;s.vz=z*=(double)velocity;float horizontal=(float)Math.sqrt(x*x+z*z);
  s.oldYaw=s.yaw=(float)(Math.atan2(x,z)*180/Math.PI);s.oldPitch=s.pitch=(float)(Math.atan2(y,horizontal)*180/Math.PI);s.groundTicks=0;h.changedMotionRotation();
 }
 public static void receivedMotion(Host h,double x,double y,double z){var s=h.state();s.vx=x;s.vy=y;s.vz=z;if(s.oldPitch==0&&s.oldYaw==0){float horizontal=(float)Math.sqrt(x*x+z*z);s.oldYaw=s.yaw=(float)(Math.atan2(x,z)*180/Math.PI);s.oldPitch=s.pitch=(float)(Math.atan2(y,horizontal)*180/Math.PI);}h.changedMotionRotation();}
 public static void tick(Host h){
  var s=h.state();s.lastX=s.x;s.lastY=s.y;s.lastZ=s.z;h.parentTick();if(s.shake>0)--s.shake;
  if(s.embedded){if(h.block(s.tileX,s.tileY,s.tileZ)==s.inTile){if(++s.groundTicks==1200)h.discard();return;}s.embedded=false;s.vx*=(double)(h.nextFloat()*.2f);s.vy*=(double)(h.nextFloat()*.2f);s.vz*=(double)(h.nextFloat()*.2f);s.groundTicks=s.airTicks=0;}else ++s.airTicks;
  Object hit=h.raycast();if(hit!=null){if(h.portal(hit))h.enterPortal(hit);else h.impact(hit);}
  s.x+=s.vx;s.y+=s.vy;s.z+=s.vz;float horizontal=(float)Math.sqrt(s.vx*s.vx+s.vz*s.vz);s.yaw=(float)(Math.atan2(s.vx,s.vz)*180/Math.PI);s.pitch=(float)(Math.atan2(s.vy,horizontal)*180/Math.PI);
  while(s.pitch-s.oldPitch< -180)s.oldPitch-=360;while(s.pitch-s.oldPitch>=180)s.oldPitch+=360;while(s.yaw-s.oldYaw< -180)s.oldYaw-=360;while(s.yaw-s.oldYaw>=180)s.oldYaw+=360;
  s.pitch=s.oldPitch+(s.pitch-s.oldPitch)*.2f;s.yaw=s.oldYaw+(s.yaw-s.oldYaw)*.2f;float drag=.99f;
  if(h.water()){for(int i=0;i<4;i++)h.bubble(s.x-s.vx*(double).25f,s.y-s.vy*(double).25f,s.z-s.vz*(double).25f,s.vx,s.vy,s.vz);drag=.8f;}
  s.vx*=(double)drag;s.vy*=(double)drag;s.vz*=(double)drag;s.vy-=(double).03f;h.position();h.changedMotionRotation();
 }
 public static void impact(Impact h){
  if(h.server()){
   if(h.entity()){
    float amount;if(h.blazeOrFireElemental()){amount=3;h.quench();}else if(h.magmaOrNetherspawn()){amount=2;h.quench();}else amount=1;
    h.damage(amount);if(h.burning()){h.quench();if(h.globalRandom()<.5)h.extinguish();}else if(h.magmaEarth())h.normalizeEarth();
   }else{if(h.neighborFire())h.douseNeighbor();h.blockCallback();}
  }
  for(int i=0;i<8;i++)h.poof();if(h.server())h.discard();
 }
 public static Object owner(Host h){var s=h.state();if(s.owner==null&&s.ownerName!=null&&s.ownerName.length()>0)s.owner=h.findPlayer(s.ownerName);return s.owner;}
 public static void save(Host h,Tags tag){var s=h.state();tag.putShort("xTile",(short)s.tileX);tag.putShort("yTile",(short)s.tileY);tag.putShort("zTile",(short)s.tileZ);tag.putByte("inTile",(byte)s.inTile);tag.putByte("shake",(byte)s.shake);tag.putByte("inGround",(byte)(s.embedded?1:0));if((s.ownerName==null||s.ownerName.length()==0)&&s.owner!=null){String name=h.playerName(s.owner);if(name!=null)s.ownerName=name;}tag.putString("ownerName",s.ownerName==null?"":s.ownerName);}
 public static void load(State s,Tags tag){s.tileX=tag.getShort("xTile");s.tileY=tag.getShort("yTile");s.tileZ=tag.getShort("zTile");s.inTile=tag.getByte("inTile")&255;s.shake=tag.getByte("shake")&255;s.embedded=tag.getByte("inGround")==1;s.ownerName=tag.getString("ownerName");if(s.ownerName!=null&&s.ownerName.length()==0)s.ownerName=null;}
 public static boolean ownerGrace(boolean owner,int airTicks){return owner&&airTicks<5;}
 public static boolean renderDistance(double averageEdge,double distanceSquared){double distance=averageEdge*4;return distanceSquared<(distance*=64)*distance;}
 public static boolean itemUse(ItemUse h){if(h.client())h.bob();else{if(!h.creative())h.consume();h.bow(.5f,.4f/(h.itemRandom()*.4f+.8f));h.spawn();}return true;}
 private SourceSnowballRules(){}
}
