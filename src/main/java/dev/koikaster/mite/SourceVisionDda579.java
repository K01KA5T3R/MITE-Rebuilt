package dev.koikaster.mite;
import net.minecraft.util.Mth;import net.minecraft.world.phys.*;
/** abw.tryRaycastVsBlocks: single-ray cell traversal and tie order, without modern endpoint nudges. */
public final class SourceVisionDda579 {
 @FunctionalInterface public interface Cells{BlockHitResult hit(int x,int y,int z,Vec3 from,Vec3 to);}
 public static BlockHitResult trace(Vec3 origin,Vec3 limit,Cells cells){
  double x=origin.x,y=origin.y,z=origin.z;if(Double.isNaN(x)||Double.isNaN(y)||Double.isNaN(z)||Double.isNaN(limit.x)||Double.isNaN(limit.y)||Double.isNaN(limit.z))return null;
  int endX=Mth.floor(limit.x),endY=Mth.floor(limit.y),endZ=Mth.floor(limit.z),cx=Mth.floor(x),cy=Mth.floor(y),cz=Mth.floor(z);var hit=cells.hit(cx,cy,cz,new Vec3(x,y,z),limit);if(hit!=null)return hit;
  int remaining=200;while(remaining-->=0){
   if(Double.isNaN(x)||Double.isNaN(y)||Double.isNaN(z)||cx==endX&&cy==endY&&cz==endZ)return null;
   boolean stepX=true,stepY=true,stepZ=true;double planeX=999d,planeY=999d,planeZ=999d;
   if(endX>cx)planeX=cx+1d;else if(endX<cx)planeX=cx;else stepX=false;
   if(endY>cy)planeY=cy+1d;else if(endY<cy)planeY=cy;else stepY=false;
   if(endZ>cz)planeZ=cz+1d;else if(endZ<cz)planeZ=cz;else stepZ=false;
   double tx=999d,ty=999d,tz=999d,dx=limit.x-x,dy=limit.y-y,dz=limit.z-z;if(stepX)tx=(planeX-x)/dx;if(stepY)ty=(planeY-y)/dy;if(stepZ)tz=(planeZ-z)/dz;int face;
   if(tx<ty&&tx<tz){face=endX>cx?4:5;x=planeX;y+=dy*tx;z+=dz*tx;}else if(ty<tz){face=endY>cy?0:1;x+=dx*ty;y=planeY;z+=dz*ty;}else{face=endZ>cz?2:3;x+=dx*tz;y+=dy*tz;z=planeZ;}
   cx=Mth.floor(x);if(face==5)--cx;cy=Mth.floor(y);if(face==1)--cy;cz=Mth.floor(z);if(face==3)--cz;
   hit=cells.hit(cx,cy,cz,new Vec3(x,y,z),limit);if(hit!=null)return hit;
  }return null;
 }
 private SourceVisionDda579(){}
}
