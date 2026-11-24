package dev.koikaster.mite.visual;

/** Original 1.6.4 bfj.a(Tessellator,FFFFIIF): both icon faces and all four
 * extrusion directions. UV half-texel offsets belong to the original mesh.
 */
public final class ClassicFlatItemMesh {
 @FunctionalInterface public interface Vertex {
  void emit(float x,float y,float z,float u,float v,float nx,float ny,float nz);
 }
 public static void emit(Vertex out,float maxU,float minV,float minU,float maxV,int width,int height){
  float back=-.0625f;
  out.emit(0,0,0,maxU,maxV,0,0,1);out.emit(1,0,0,minU,maxV,0,0,1);out.emit(1,1,0,minU,minV,0,0,1);out.emit(0,1,0,maxU,minV,0,0,1);
  out.emit(0,1,back,maxU,minV,0,0,-1);out.emit(1,1,back,minU,minV,0,0,-1);out.emit(1,0,back,minU,maxV,0,0,-1);out.emit(0,0,back,maxU,maxV,0,0,-1);
  float halfU=.5f*(maxU-minU)/width,halfV=.5f*(maxV-minV)/height;
  for(int i=0;i<width;i++){
   float fraction=(float)i/width,u=maxU+(minU-maxU)*fraction-halfU,x=fraction;
   out.emit(x,0,back,u,maxV,-1,0,0);out.emit(x,0,0,u,maxV,-1,0,0);out.emit(x,1,0,u,minV,-1,0,0);out.emit(x,1,back,u,minV,-1,0,0);
  }
  for(int i=0;i<width;i++){
   float fraction=(float)i/width,u=maxU+(minU-maxU)*fraction-halfU,x=fraction+1f/width;
   out.emit(x,1,back,u,minV,1,0,0);out.emit(x,1,0,u,minV,1,0,0);out.emit(x,0,0,u,maxV,1,0,0);out.emit(x,0,back,u,maxV,1,0,0);
  }
  for(int i=0;i<height;i++){
   float fraction=(float)i/height,v=maxV+(minV-maxV)*fraction-halfV,y=fraction+1f/height;
   out.emit(0,y,0,maxU,v,0,1,0);out.emit(1,y,0,minU,v,0,1,0);out.emit(1,y,back,minU,v,0,1,0);out.emit(0,y,back,maxU,v,0,1,0);
  }
  for(int i=0;i<height;i++){
   float fraction=(float)i/height,v=maxV+(minV-maxV)*fraction-halfV,y=fraction;
   out.emit(1,y,0,minU,v,0,-1,0);out.emit(0,y,0,maxU,v,0,-1,0);out.emit(0,y,back,maxU,v,0,-1,0);out.emit(1,y,back,minU,v,0,-1,0);
  }
 }
 public static void emitMirrored(Vertex out,float maxU,float minV,float minU,float maxV,int width,int height){
  Vertex reverse=new Vertex(){final float[] quad=new float[32];int index;
   public void emit(float x,float y,float z,float u,float v,float nx,float ny,float nz){
    int at=index++*8;quad[at]=-x;quad[at+1]=y;quad[at+2]=z;quad[at+3]=u;quad[at+4]=v;quad[at+5]=-nx;quad[at+6]=ny;quad[at+7]=nz;
    if(index==4){for(int i=3;i>=0;i--){at=i*8;out.emit(quad[at],quad[at+1],quad[at+2],quad[at+3],quad[at+4],quad[at+5],quad[at+6],quad[at+7]);}index=0;}
   }
  };
  emit(reverse,maxU,minV,minU,maxV,width,height);
 }
 private ClassicFlatItemMesh(){}
}
