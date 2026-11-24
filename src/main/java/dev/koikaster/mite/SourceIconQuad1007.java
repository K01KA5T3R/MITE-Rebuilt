package dev.koikaster.mite;
/** bdo.a: preserves the source float operations, vertex order and item-specific U orientation. */
public final class SourceIconQuad1007 {
 public record State(double oldX,double oldY,double oldZ,double x,double y,double z,int tileX,int tileY,float jitterU,float jitterV,float quadSize,float red,float green,float blue){}
 public record Camera(double x,double y,double z,float rotationX,float rotationXZ,float rotationZ,float rotationYZ,float rotationXY){}
 public interface Icon {float u(double pixel);float v(double pixel);}
 public interface Sink {void color(float red,float green,float blue);void vertex(double x,double y,double z,double u,double v);}
 public static void render(State p,Camera camera,float partial,Icon icon,Sink sink){
  float u0=((float)p.tileX+p.jitterU/4f)/16f,u1=u0+.015609375f,v0=((float)p.tileY+p.jitterV/4f)/16f,v1=v0+.015609375f;
  if(icon!=null){u0=icon.u((double)(p.jitterU/4f*16f));u1=icon.u((double)((p.jitterU+1f)/4f*16f));v0=icon.v((double)(p.jitterV/4f*16f));v1=icon.v((double)((p.jitterV+1f)/4f*16f));}
  float x=(float)(p.oldX+(p.x-p.oldX)*(double)partial-camera.x),y=(float)(p.oldY+(p.y-p.oldY)*(double)partial-camera.y),z=(float)(p.oldZ+(p.z-p.oldZ)*(double)partial-camera.z);
  float size=p.quadSize,a=camera.rotationX,b=camera.rotationXZ,c=camera.rotationZ,d=camera.rotationYZ,e=camera.rotationXY;
  float brightness=1f;sink.color(brightness*p.red,brightness*p.green,brightness*p.blue);
  sink.vertex((double)(x-a*size-d*size),(double)(y-b*size),(double)(z-c*size-e*size),(double)u0,(double)v1);
  sink.vertex((double)(x-a*size+d*size),(double)(y+b*size),(double)(z-c*size+e*size),(double)u0,(double)v0);
  sink.vertex((double)(x+a*size+d*size),(double)(y+b*size),(double)(z+c*size+e*size),(double)u1,(double)v0);
  sink.vertex((double)(x+a*size-d*size),(double)(y-b*size),(double)(z+c*size-e*size),(double)u1,(double)v1);
 }
 private SourceIconQuad1007(){}
}
