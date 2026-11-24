package dev.koikaster.mite;

/** sk.c's server flight and seven-part pose, with original float sqrt vector
 * normalization. World collision, target acquisition and old position storage
 * are the native owner's explicit operations. */
public final class SourceDragonKinematics {
 public record Vector(double x,double y,double z){
  public Vector normalized(){double length=(double)(float)Math.sqrt(x*x+y*y+z*z);return length<1.0E-4?new Vector(0,0,0):new Vector(x/length,y/length,z/length);}
  public double dot(Vector value){return x*value.x+y*value.y+z*value.z;}
 }
 public record Target(double x,double z,double bottomY){}
 public interface MotionHost {
  SourceDragonRules.State state();double x();double y();double z();float yaw();void yaw(float value);float turning();void turning(float value);
  Vector motion();void motion(Vector value);Target tracked();double nextGaussian();boolean horizontalCollision();boolean verticalCollision();boolean blocked();void chooseTarget();void accelerate(float amount);void move(Vector displacement);
 }
 public interface LayoutHost {
  double x();double y();double z();float yaw();float turning();double[] history(int offset,float partial);void size(int part,float width,float height);void part(int part,double x,double y,double z);boolean contactAllowed();void contact();
 }
 public static float flap(float previous,double x,double y,double z,boolean blocked){float amount=.2f/((float)Math.sqrt(x*x+z*z)*10f+1f);amount*=(float)Math.pow(2.0,y);return previous+(blocked?amount*.5f:amount);}
 public static void flight(MotionHost h){
  var s=h.state();double dx=s.x-h.x(),dy=s.y-h.y(),dz=s.z-h.z(),distance=dx*dx+dy*dy+dz*dz;
  var target=h.tracked();if(target!=null){s.x=target.x;s.z=target.z;double x=s.x-h.x(),z=s.z-h.z(),horizontal=Math.sqrt(x*x+z*z),offset=(double).4f+horizontal/80-1;if(offset>10)offset=10;s.y=target.bottomY+offset;}else{s.x+=h.nextGaussian()*2;s.z+=h.nextGaussian()*2;}
  if(s.force||distance<100||distance>22500||h.horizontalCollision()||h.verticalCollision())h.chooseTarget();
  float limit=.6f;dy/=(double)(float)Math.sqrt(dx*dx+dz*dz);if(dy<(double)-limit)dy=-limit;if(dy>(double)limit)dy=limit;var m=h.motion();h.motion(new Vector(m.x,m.y+dy*(double).1f,m.z));h.yaw(wrap(h.yaw()));double desired=180-Math.atan2(dx,dz)*180/Math.PI,turn=wrap(desired-(double)h.yaw());if(turn>50)turn=50;if(turn< -50)turn=-50;
  var direction=new Vector(s.x-h.x(),s.y-h.y(),s.z-h.z()).normalized();float angle=h.yaw()*(float)Math.PI/180f;var forward=new Vector(SourceDragonTrig.sin(angle),h.motion().y,-SourceDragonTrig.cos(angle)).normalized();float align=(float)(forward.dot(direction)+.5)/1.5f;if(align<0)align=0;h.turning(h.turning()*.8f);m=h.motion();float f=(float)Math.sqrt(m.x*m.x+m.z*m.z)*1f+1f;double speed=Math.sqrt(m.x*m.x+m.z*m.z)*1+1;if(speed>40)speed=40;h.turning((float)((double)h.turning()+turn*((double).7f/speed/(double)f)));h.yaw(h.yaw()+h.turning()*.1f);float factor=(float)(2/(speed+1)),acceleration=.06f*(align*factor+(1-factor));h.accelerate(acceleration);m=h.motion();h.move(h.blocked()?new Vector(m.x*(double).8f,m.y*(double).8f,m.z*(double).8f):m);var unit=h.motion().normalized();float friction=(float)(unit.dot(forward)+1)/2f;friction=.8f+.15f*friction;m=h.motion();h.motion(new Vector(m.x*(double)friction,m.y*(double).91f,m.z*(double)friction));
 }
 public static void layout(LayoutHost h){
  h.size(0,3,3);h.size(1,5,3);for(int i=2;i<=4;i++)h.size(i,2,2);h.size(5,4,2);h.size(6,4,3);
  float slope=(float)(h.history(5,1)[1]-h.history(10,1)[1])*10f/180f*(float)Math.PI,cos=SourceDragonTrig.cos(slope),sin=-SourceDragonTrig.sin(slope),yaw=h.yaw()*(float)Math.PI/180f,s=SourceDragonTrig.sin(yaw),c=SourceDragonTrig.cos(yaw);
  h.part(1,h.x()+(double)(s*.5f),h.y(),h.z()-(double)(c*.5f));h.part(5,h.x()+(double)(c*4.5f),h.y()+2,h.z()+(double)(s*4.5f));h.part(6,h.x()-(double)(c*4.5f),h.y()+2,h.z()-(double)(s*4.5f));
  if(h.contactAllowed())h.contact();
  var old=h.history(5,1);var current=h.history(0,1);float hs=SourceDragonTrig.sin(h.yaw()*(float)Math.PI/180f-h.turning()*.01f),hc=SourceDragonTrig.cos(h.yaw()*(float)Math.PI/180f-h.turning()*.01f);h.part(0,h.x()+(double)(hs*5.5f*cos),h.y()+(current[1]-old[1])+(double)(sin*5.5f),h.z()-(double)(hc*5.5f*cos));
  for(int i=0;i<3;i++){var history=h.history(12+i*2,1);float a=h.yaw()*(float)Math.PI/180f+(float)wrap(history[0]-old[0])*(float)Math.PI/180f*1f,ts=SourceDragonTrig.sin(a),tc=SourceDragonTrig.cos(a),distance=(float)(i+1)*2f;h.part(i+2,h.x()-(double)((s*1.5f+ts*distance)*cos),h.y()+(history[1]-old[1])-(double)((distance+1.5f)*sin)+1.5,h.z()+(double)((c*1.5f+tc*distance)*cos));}
 }
 public static float wrap(float value){value%=360f;if(value>=180f)value-=360f;if(value< -180f)value+=360f;return value;}
 public static double wrap(double value){value%=360;if(value>=180)value-=360;if(value< -180)value+=360;return value;}
 private SourceDragonKinematics(){}
}
