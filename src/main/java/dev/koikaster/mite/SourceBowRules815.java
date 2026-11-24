package dev.koikaster.mite;

/** wp/ud/uh player use, ammunition and launch operations. Game transports live in SourceBowUse815. */
public final class SourceBowRules815 {
 public interface Inventory {
  int size(); int hotbarSize(); Object stack(int slot); boolean arrow(Object stack);
  int count(Object stack); Object item(Object stack); void decrement(Object stack); void set(int slot,Object stack);
 }
 public static int arrowSlot(Inventory inv){
  for(int i=0;i<inv.hotbarSize();i++){Object stack=inv.stack(i);if(stack!=null&&inv.arrow(stack)&&inv.count(stack)>0)return i;}
  return -1;
 }
 public static Object readied(Inventory inv){int slot=arrowSlot(inv);return slot<0?null:inv.item(inv.stack(slot));}
 public static boolean consume(Inventory inv){
  int slot=arrowSlot(inv);if(slot<0)return false;
  Object stack=inv.stack(slot),item=inv.item(stack);inv.decrement(stack);
  if(inv.count(stack)<=0){
   if(readied(inv)==null)for(int i=inv.hotbarSize();i<inv.size();i++){
    Object stock=inv.stack(i);if(stock!=null&&inv.item(stock)==item){inv.set(slot,stock);inv.set(i,null);return true;}
   }
   inv.set(slot,null);
  }
  return true;
 }
 public interface Use {
  boolean client(); boolean creative(); Object readied(); Object nocked(); Object flint();
  void nock(Object arrow); void syncNock(); void start();
 }
 public static boolean use(Use use){
  if(use.creative()||use.readied()!=null){
   use.nock(use.readied());if(use.nocked()==null&&use.creative())use.nock(use.flint());
   if(!use.client())use.syncNock();use.start();return true;
  }
  return false;
 }
 public interface Release extends Use {
  int maxPull(); Object create(Object arrow,float velocity); void critical(Object arrow);
  int power(); int punch(); int flame(); void power(Object arrow,int level); void punch(Object arrow,int level);
  void flame(Object arrow); void wear(); float nextItemFloat(); void sound(float pitch);
  void creativePickup(Object arrow); void consume(); void spawn(Object arrow);
 }
 public static float fraction(int ticks,int maxPull){return Math.min(ticks/(float)maxPull,1f);}
 public static float power(int ticks,int maxPull){float f=fraction(ticks,maxPull);return Math.min((f*f+f*2f)/3f,1f);}
 public static void release(Release use,int ticks){
  if(use.client())return;
  Object item=use.readied();if(item==null){if(use.creative())item=use.nocked();else return;}
  float fraction=power(ticks,use.maxPull());if(fraction<.1f)return;
  Object arrow=use.create(item,fraction*2f);use.nock(null);
  if(fraction==1f)use.critical(arrow);
  int level=use.power();if(level>0)use.power(arrow,level);
  level=use.punch();if(level>0)use.punch(arrow,level);
  if(use.flame()>0)use.flame(arrow);
  use.wear();use.sound(1f/(use.nextItemFloat()*.4f+1.2f)+fraction*.5f);
  if(use.creative())use.creativePickup(arrow);else use.consume();
  if(!use.client())use.spawn(arrow);
 }
 public interface Random {double gaussian();boolean bool();}
 public record Launch(double x,double y,double z,double dx,double dy,double dz,float yaw,float pitch){}
 public static float wander(int level,boolean skills,boolean archery,int trueFlight,boolean suspended){
  int effective=level<0||!skills||archery?level:0;effective+=trueFlight;
  float result=effective<0?5f+effective*-.5f:(float)(.5+4.5/Math.pow(.8f+(float)(effective+1)/5f,2));
  return suspended?result*2f:result;
 }
 public static Launch launch(double x,double y,double z,float eye,float yaw,float pitch,float velocity,float bonus,float wander,Random random){
  float angle=yaw/180f*(float)Math.PI,vertical=pitch/180f*(float)Math.PI;
  x-=(double)(SourceDragonTrig.cos(angle)*.16f)*(double).3f;y+=(double)eye;y-=(double).1f;
  z-=(double)(SourceDragonTrig.sin(angle)*.16f)*(double).3f;
  double dx=-SourceDragonTrig.sin(angle)*SourceDragonTrig.cos(vertical);
  double dz=SourceDragonTrig.cos(angle)*SourceDragonTrig.cos(vertical),dy=-SourceDragonTrig.sin(vertical);
  velocity*=1.5f;velocity*=bonus;
  float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=(double)length;dy/=(double)length;dz/=(double)length;
  dx+=random.gaussian()*(double)(random.bool()?-1:1)*(double).0075f*(double)wander;
  dy+=random.gaussian()*(double)(random.bool()?-1:1)*(double).0075f*(double)wander;
  dz+=random.gaussian()*(double)(random.bool()?-1:1)*(double).0075f*(double)wander;
  dx*=(double)velocity;dy*=(double)velocity;dz*=(double)velocity;
  float horizontal=(float)Math.sqrt(dx*dx+dz*dz);
  return new Launch(x,y,z,dx,dy,dz,(float)(Math.atan2(dx,dz)*180/Math.PI),(float)(Math.atan2(dy,horizontal)*180/Math.PI));
 }
 private SourceBowRules815(){}
}
