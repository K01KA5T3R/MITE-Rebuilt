package dev.koikaster.mite;
/** alg.a(entity,x,y,z,size,false,false,true), while pl is executing. Cactus
 * avoidance is explicitly bypassed by that source task; modern block shapes
 * are classified by the native host rather than substituting noCollision. */
public final class SourcePigPathRules {
 public record Cell(boolean air,boolean trapdoor,boolean water,boolean rail,boolean pathable,boolean lava,boolean fenceLike){}
 public interface Host {Cell cell(int x,int y,int z);boolean standingOnRail();boolean inLava();}
 public static int query(Host h,int x,int y,int z,int width,int height,int depth){boolean special=false;for(int a=x;a<x+width;a++)for(int b=y;b<y+height;b++)for(int c=z;c<z+depth;c++){var block=h.cell(a,b,c);if(block.air)continue;if(block.trapdoor||block.water)special=true;if(block.rail){if(h.standingOnRail())continue;return -3;}if(!block.lava&&block.pathable)continue;if(block.fenceLike)return -3;if(block.trapdoor)return -4;if(!block.lava||h.inLava())return 0;return -2;}return special?2:1;}
 private SourcePigPathRules(){}
}
