package dev.koikaster.mite;

/** Activated sr.l_ control for tileless, non-anvil BlockUnderminable entities.
 * Coordinates use R195's entity centre; a native adapter must translate its
 * foot-origin entity coordinates. Glass is the akc.s ground exception. */
public final class SourceFallingSoilRules243 {
 public static final class State {
  public int block,metadata,time,itemDamage;
  public String customName;
  public double x,y,z,previousX,previousY,previousZ,vx,vy,vz;
  public float yOffset=.49f;
  public boolean removed,ground,inWater,firstUpdate=true,dropItem=true,cancelDrop;
 }
 public interface Host {
  boolean client();int floor();int ceiling();
  int block(int x,int y,int z);boolean liquid(int x,int y,int z);boolean water(int x,int y,int z);boolean glass(int x,int y,int z);boolean movingPiston(int x,int y,int z);
  boolean dislodged(int falling,int metadata,int x,int y,int z);boolean sandFalls(int metadata,int x,int y,int z);
  void move(State entity);void discard(State entity);void splash(State entity);
  int dropOccupied(State entity,int x,int y,int z,boolean collided);void pop(State entity,float pitch);float randomFloat();
  void remove(int x,int y,int z);void destroyGlass(State entity,int x,int y,int z);
  boolean place(State entity,int x,int y,int z);void landed(State entity,int x,int y,int z);void dropFalling(State entity);
 }
 private static int floor(double value){return (int)Math.floor(value);}
 private static void occupied(Host world,State entity,int x,int y,int z){
  if(world.block(x,y,z)==0||world.liquid(x,y,z))return;
  if(world.dislodged(entity.block,entity.metadata,x,y,z)){
   boolean collided=world.sandFalls(entity.metadata,x,y-1,z);
   if(world.dropOccupied(entity,x,y,z,collided)>0&&collided)world.pop(entity,((world.randomFloat()-world.randomFloat())*.7f+1f)*2f);
   world.remove(x,y,z);
  }
 }
 public static void tick(Host world,State entity){
  if(entity.block==0)world.discard(entity);
  else{
   entity.previousX=entity.x;entity.previousY=entity.y;entity.previousZ=entity.z;
   ++entity.time;entity.vy-=(double).04f;world.move(entity);
   entity.vx*=(double).98f;entity.vy*=(double).98f;entity.vz*=(double).98f;
   int x=floor(entity.x),y=floor(entity.y),z=floor(entity.z);
   if(world.water(x,y,z)){
    if(!entity.inWater&&!entity.firstUpdate&&entity.vy<(double)-.2f&&!world.client())world.splash(entity);
    entity.inWater=true;
   }
   if(!world.client()){
    if(entity.time==1){if(world.block(x,y,z)!=entity.block){world.discard(entity);return;}world.remove(x,y,z);}
    occupied(world,entity,x,y,z);
    if(entity.ground&&world.glass(x,y-1,z))world.destroyGlass(entity,x,y-1,z);
    else if(entity.ground){
     entity.vx*=(double).7f;entity.vz*=(double).7f;entity.vy*= -.5;
     y=floor(entity.y-(double)entity.yOffset);occupied(world,entity,x,y,z);
     if(!world.movingPiston(x,y,z)){
      world.discard(entity);
      if(!entity.cancelDrop&&!world.sandFalls(entity.metadata,x,y-1,z)&&(world.block(x,y,z)==0||world.dislodged(entity.block,entity.metadata,x,y,z))&&world.place(entity,x,y,z))world.landed(entity,x,y,z);
      else if(entity.dropItem&&!entity.cancelDrop)world.dropFalling(entity);
     }
    }else if(entity.time>100&&(y<world.floor()+1||y>world.ceiling())||entity.time>600){
     if(entity.dropItem)world.dropFalling(entity);world.discard(entity);
    }
   }
  }
  entity.firstUpdate=false;
 }
 private SourceFallingSoilRules243(){}
}
