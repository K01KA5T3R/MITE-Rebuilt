package dev.koikaster.mite;
import java.util.List;
/** nn.d particle branch: isEntityFX=true, stepHeight=0, not a living/player/item entity, walking=false, no rider. */
public final class SourceIconMotion1014 {
 public interface World {double minXZ();double maxXZ();double minY();double maxY();List<SourceParticleBox1014> collisions(SourceParticleBox1014 swept);void afterCollision(State state);}
 public static final class State {
  public double x,y,z,vx,vy,vz;public float width=.2f,height=.2f,offsetY=.1f,ySize,fallDistance;public boolean noClip,web,slide=true,onGround,horizontal,vertical,collided;public SourceParticleBox1014 box;
  public State(double x,double y,double z){position(x,y,z);}
  public void position(double x,double y,double z){this.x=x;this.y=y;this.z=z;float half=width/2f;box=new SourceParticleBox1014(x-(double)half,y-(double)offsetY+(double)ySize,z-(double)half,x+(double)half,y-(double)offsetY+(double)ySize+(double)height,z+(double)half);}
  public void cobweb(){web=true;fallDistance=0;}
 }
 private static boolean limits(State s,double[] limit,int axis){
  if(Double.isNaN(limit[0])&&Double.isNaN(limit[1]))return false;var box=s.box;float size=axis==1?s.height:s.width;
  if(!Double.isNaN(limit[0])){double max=axis==0?box.d:axis==1?box.e:box.f;limit[2]=limit[0]-max;if(axis==0){box.d=limit[0];box.a=limit[0]-(double)size;}else if(axis==1){box.e=limit[0];box.b=limit[0]-(double)size;}else{box.f=limit[0];box.c=limit[0]-(double)size;}}
  else{double min=axis==0?box.a:axis==1?box.b:box.c;limit[2]=limit[1]-min;if(axis==0){box.a=limit[1];box.d=limit[1]+(double)size;}else if(axis==1){box.b=limit[1];box.e=limit[1]+(double)size;}else{box.c=limit[1];box.f=limit[1]+(double)size;}}return true;
 }
 private static void location(State s){s.x=(s.box.a+s.box.d)/2.0;s.y=s.box.b+(double)s.offsetY-(double)s.ySize;s.z=(s.box.c+s.box.f)/2.0;}
 public static void move(State s,World world,double dx,double dy,double dz){
  if(s.x+dx<world.minXZ())dx=world.minXZ()-s.x;else if(s.x+dx>world.maxXZ())dx=world.maxXZ()-s.x;
  if(s.y+dy<world.minY())dy=world.minY()-s.y;else if(s.y+dy>world.maxY())dy=world.maxY()-s.y;
  if(s.z+dz<world.minXZ())dz=world.minXZ()-s.z;else if(s.z+dz>world.maxXZ())dz=world.maxXZ()-s.z;
  if(s.noClip){s.box.offset(dx,dy,dz);location(s);return;}
  s.ySize*=.4f;
  if(s.web){s.web=false;dx*=.25;dy*=(double).05f;dz*=.25;s.vx=0;s.vy=0;s.vz=0;}
  double originalX=dx,originalY=dy,originalZ=dz;var boxes=world.collisions(s.box.swept(dx,dy,dz));double[] limit={Double.NaN,Double.NaN,0};
  for(var box:boxes)dy=box.calculateYOffset(s.box,dy,limit);if(!limits(s,limit,1))s.box.offset(0,dy,0);
  if(!s.slide&&originalY!=dy){dz=0;dy=0;dx=0;}
  limit[0]=Double.NaN;limit[1]=Double.NaN;for(var box:boxes)dx=box.calculateXOffset(s.box,dx,limit);if(!limits(s,limit,0))s.box.offset(dx,0,0);
  if(!s.slide&&originalX!=dx){dz=0;dy=0;dx=0;}
  limit[0]=Double.NaN;limit[1]=Double.NaN;for(var box:boxes)dz=box.calculateZOffset(s.box,dz,limit);if(!limits(s,limit,2))s.box.offset(0,0,dz);
  if(!s.slide&&originalZ!=dz){dz=0;dy=0;dx=0;}
  location(s);s.horizontal=originalX!=dx||originalZ!=dz;s.vertical=originalY!=dy;s.onGround=originalY!=dy&&originalY<0;s.collided=s.horizontal||s.vertical;
  if(s.onGround){if(s.fallDistance>0)s.fallDistance=0;}else if(dy<0)s.fallDistance=(float)((double)s.fallDistance-dy);
  if(originalX!=dx)s.vx=0;if(originalY!=dy)s.vy=0;if(originalZ!=dz)s.vz=0;
  world.afterCollision(s);
 }
 private SourceIconMotion1014(){}
}
