package dev.koikaster.mite;

/** nn.pushOutOfBlocks, oa range2. World block bounds and the existing RNG table are transports. */
public final class SourceOrbEscape860 {
 public interface Host {
  boolean client();int ticks();double x();double y();double z();double centerY();
  boolean solid(int x,int y,int z);boolean cauldron(int x,int y,int z);boolean clear(double dx,double dy,double dz);boolean loaded(int x,int y,int z);
  double globalRandom();double table(int index);void relocate(double dx,double dy,double dz);
 }
 public static int cell(double value){return value<0?(int)value-1:(int)value;}
 public static int escape(Host h){
  if(h.client()||h.ticks()<2)return 0;double originalY=h.centerY();int x=cell(h.x()),y=cell(originalY),z=cell(h.z());
  if((!h.solid(x,y,z)||h.cauldron(x,y,z))&&h.clear(0,0,0))return 0;
  boolean possible=false;boolean[] candidates=new boolean[125];
  for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)for(int dz=-2;dz<=2;dz++)if(h.loaded(x+dx,y+dy,z+dz)&&!h.solid(x+dx,y+dy,z+dz)){possible=true;candidates[dx+2+(dy+2)*5+(dz+2)*25]=true;}
  if(!possible)return -1;int index=(int)(h.globalRandom()*2147483647d);float range=.1f;
  while((range+=.001f)<3f){
   double dx=h.table(++index&32767)*(double)range*2-(double)range,dy=h.table(++index&32767)*(double)range*2-(double)range,dz=h.table(++index&32767)*(double)range*2-(double)range;
   int tx=cell(h.x()+dx)-x,ty=cell(originalY+dy)-y,tz=cell(h.z()+dz)-z;
   if(tx < -2||tx>2||ty < -2||ty>2||tz < -2||tz>2||!candidates[tx+2+(ty+2)*5+(tz+2)*25]||!h.clear(dx,dy,dz))continue;
   h.relocate(dx,dy,dz);return 1;
  }return -1;
 }
 private SourceOrbEscape860(){}
}
