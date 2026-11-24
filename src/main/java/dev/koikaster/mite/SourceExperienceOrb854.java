package dev.koikaster.mite;

/** R195 oa. World collision, parent entity damage and player XP are explicit transports. */
public final class SourceExperienceOrb854 {
 public static final class State {
  public double x,y,z,vx,vy,vz,lastX,lastY,lastZ;
  public int animation,age,pickupDelay,health=5,value,lastSearch;
  public Object target;public String ownerName;public boolean bottle;
 }
 public interface Host {
  State state();void parentTick();boolean server();boolean fullLava();void pushOut();int entityId();
  Iterable<?> players();String name(Object player);double distanceSquared(Object player);Object closest(double radius);
  double playerX(Object player);double playerY(Object player);double playerZ(Object player);float playerEye(Object player);
  void move(double vx,double vy,double vz);boolean onGround();int belowBlock();float slipperiness(int block);void discard();
 }
 public interface Constructor {
  State state();void size(float width,float height);void yOffset(float value);void position(double x,double y,double z);
  boolean server();double globalRandom();
 }
 public interface Pickup {
  State state();boolean server();boolean reachable();int playerDelay();String playerName();void playerDelay(int value);
  void take(int count);void experience(int value,boolean createdByBottle);void discard();
 }
 public interface Reach {boolean clear(float fraction);}
 public interface Tags {
  void putShort(String name,short value);void putString(String name,String value);void putBoolean(String name,boolean value);
  short getShort(String name);String getString(String name);boolean getBoolean(String name);boolean contains(String name);
 }
 public interface Result {boolean destroyed();void start(int health);void finish(int health);void destroyed(boolean value);}
 public interface Hurt {State state();Result parent();float amount();void attacked();void discard();}
 public static void init(Constructor h,double x,double y,double z,int value){
  h.size(.25f,.25f);h.yOffset(.25f/2f);h.position(x,y,z);var s=h.state();s.value=value;
  if(h.server()){s.vx=(float)(h.globalRandom()*(double).2f-(double).1f)*2f;s.vy=(float)(h.globalRandom()*.2)*2f;s.vz=(float)(h.globalRandom()*(double).2f-(double).1f)*2f;}
 }
 public static void owner(State s,String name){s.ownerName="".equals(name)?null:name;}
 public static void tick(Host h){
  var s=h.state();h.parentTick();if(s.pickupDelay>0)--s.pickupDelay;
  s.lastX=s.x;s.lastY=s.y;s.lastZ=s.z;s.vy-=(double).03f;
  if(h.fullLava()){s.vy=0;s.vx*=(double).95f;s.vz*=(double).95f;}
  h.pushOut();double range=8;
  if(s.lastSearch<s.animation-20+h.entityId()%100){
   if(s.ownerName!=null){for(Object player:h.players())if(s.ownerName.equals(h.name(player))){s.target=player;break;}}
   else if(s.target==null||h.distanceSquared(s.target)>range*range)s.target=h.closest(range);
   s.lastSearch=s.animation;
  }
  if(s.target!=null){
   double x=(h.playerX(s.target)-s.x)/range,y=(h.playerY(s.target)+(double)h.playerEye(s.target)-s.y)/range,z=(h.playerZ(s.target)-s.z)/range;
   double distance=Math.sqrt(x*x+y*y+z*z),pull=1-distance;
   if(pull>0){pull*=pull;s.vx+=x/distance*pull*.1;s.vy+=y/distance*pull*.1;s.vz+=z/distance*pull*.1;}
  }
  h.move(s.vx,s.vy,s.vz);float friction=.98f;
  if(h.onGround()){friction=.58800006f;int block=h.belowBlock();if(block>0)friction=h.slipperiness(block)*.98f;}
  s.vx*=(double)friction;s.vy*=(double).98f;s.vz*=(double)friction;if(h.onGround())s.vy*=(double)-.9f;
  ++s.animation;++s.age;if(s.age>=6000)h.discard();
 }
 public static boolean reachable(Reach h){return h.clear(.25f)||h.clear(.5f)||h.clear(.75f);}
 public static void pickup(Pickup h){
  if(h.server()){
   if(!h.reachable())return;var s=h.state();
   if(s.pickupDelay==0&&h.playerDelay()==0){if(s.ownerName!=null&&!s.ownerName.equals(h.playerName()))return;
    h.playerDelay(2);h.take(1);h.experience(s.value,s.bottle);h.discard();
   }
  }
 }
 public static Result hurt(Hurt h){
  var result=h.parent();if(result==null||result.destroyed())return result;h.attacked();var s=h.state();result.start(s.health);
  s.health=(int)((float)s.health-h.amount());result.finish(s.health);if(s.health<=0){h.discard();result.destroyed(true);}return result;
 }
 public static void save(State s,Tags tag){
  tag.putShort("Health",(short)((byte)s.health));tag.putShort("Age",(short)s.age);tag.putShort("Value",(short)s.value);
  if(s.ownerName!=null)tag.putString("player_this_belongs_to",s.ownerName);if(s.bottle)tag.putBoolean("created_by_bottle_of_enchanting",true);
 }
 public static void load(State s,Tags tag){
  s.health=tag.getShort("Health")&255;s.age=tag.getShort("Age");s.value=tag.getShort("Value");
  if(tag.contains("player_this_belongs_to"))s.ownerName=tag.getString("player_this_belongs_to");s.bottle=tag.getBoolean("created_by_bottle_of_enchanting");
 }
 public static int split(int value){return value>=2477?2477:value>=1237?1237:value>=617?617:value>=307?307:value>=149?149:value>=73?73:value>=37?37:value>=17?17:value>=7?7:value>=3?3:1;}
 public static int icon(int value){return value>=2477?10:value>=1237?9:value>=617?8:value>=307?7:value>=149?6:value>=73?5:value>=37?4:value>=17?3:value>=7?2:value>=3?1:0;}
 public static int brightness(int parent){int low=(parent&255)+120;if(low>240)low=240;return low|(parent>>16&255)<<16;}
 private SourceExperienceOrb854(){}
}
