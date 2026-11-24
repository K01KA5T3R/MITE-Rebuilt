package dev.koikaster.mite;
/** sk.c: original exit structure, fixed Y64 and a 3.5-radius cylinder. */
public final class SourceDragonPortal {
 public interface Host {void allowPortalPlacement(boolean value);void block(int x,int y,int z,int block);void torch(int x,int y,int z,String face);}
 public static void create(Host h,int centerX,int centerZ){
  int level=64,radius=4;h.allowPortalPlacement(true);
  for(int y=level-1;y<=level+32;y++)for(int x=centerX-radius;x<=centerX+radius;x++)for(int z=centerZ-radius;z<=centerZ+radius;z++){
   double dx=x-centerX,dz=z-centerZ,d=dx*dx+dz*dz;if(!(d<=((double)radius-.5)*((double)radius-.5)))continue;
   if(y<level){if(d<=((double)(radius-1)-.5)*((double)(radius-1)-.5))h.block(x,y,z,7);}
   else if(y>level)h.block(x,y,z,0);
   else h.block(x,y,z,d>((double)(radius-1)-.5)*((double)(radius-1)-.5)?7:119);
  }
  h.block(centerX,level,centerZ,7);h.block(centerX,level+1,centerZ,7);h.block(centerX,level+2,centerZ,7);
  h.torch(centerX-1,level+2,centerZ,"WEST");h.torch(centerX+1,level+2,centerZ,"EAST");h.torch(centerX,level+2,centerZ-1,"NORTH");h.torch(centerX,level+2,centerZ+1,"SOUTH");
  h.block(centerX,level+3,centerZ,7);h.block(centerX,level+4,centerZ,122);h.allowPortalPlacement(false);
 }
 private SourceDragonPortal(){}
}
