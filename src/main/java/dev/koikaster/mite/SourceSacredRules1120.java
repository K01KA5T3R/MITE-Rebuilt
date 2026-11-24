package dev.koikaster.mite;
import java.util.function.DoubleSupplier;
/** EntitySacredFX table, zero-velocity beg construction, growth and tick arithmetic. */
public final class SourceSacredRules1120 {
 public record Table(double[] x,double[] y,double[] z){}
 public static Table table(float[] values,DoubleSupplier math){
  var x=new double[255];var y=new double[255];var z=new double[255];int index=0;
  for(int i=0;i<255;i++){
   double vx=values[++index&32767]*.8f-.4f,vy=values[++index&32767]*.8f-.4f,vz=values[++index&32767]*.8f-.4f;
   float speed=(float)(math.getAsDouble()+math.getAsDouble()+1.0)*.15f,length=(float)Math.sqrt(vx*vx+vy*vy+vz*vz);
   vx=vx/(double)length*(double)speed*(double).4f;vy=vy/(double)length*(double)speed*(double).4f+(double).1f;vz=vz/(double)length*(double)speed*(double).4f;
   vx*=(double).1f;vy*=(double).1f;vz*=(double).1f;
   while(vx*vx+vy*vy+vz*vz>(double)1.0E-5f){vx*=(double).8f;vy*=(double).8f;vz*=(double).8f;}
   x[i]=vx;y[i]=vy;z[i]=vz;
  }return new Table(x,y,z);
 }
 public static final class State {public double vx,vy,vz;public float scale,alpha=.15f;public int age,lifetime,nextMotion,sharedIndex;}
 public static State create(Table table,float[] values,int[] life,int randomIndex,int motionIndex,int sharedIndex,float scale){
  float jitterU=values[++randomIndex&32767]*3f,jitterV=values[++randomIndex&32767]*3f;
  float size=values[++randomIndex&32767]+1f;int unusedLifetime=(int)(4f/(values[++randomIndex&32767]*.9f+.1f));
  var state=new State();state.vx=table.x[motionIndex];state.vy=table.y[motionIndex];state.vz=table.z[motionIndex];state.nextMotion=motionIndex+1>=255?0:motionIndex+1;size*=.25f;size*=scale;state.scale=size;state.lifetime=60+life[++sharedIndex&32767];state.sharedIndex=sharedIndex;return state;
 }
 public static float quad(State state,float partial){float factor=((float)state.age+partial)/(float)state.lifetime*32f;if(factor<0)factor=0;if(factor>1)factor=1;return .1f*(state.scale*factor);}
 public interface Motion {void previous();double y();void moveSource(double x,double y,double z);boolean ground();void removeSource();}
 public static void tick(State state,Motion h){h.previous();if(state.age++>=state.lifetime)h.removeSource();state.alpha=(float)(state.lifetime-state.age)/(float)state.lifetime*.1f;double oldY=h.y();h.moveSource(state.vx,state.vy,state.vz);if(h.y()==oldY){state.vx*=1.1;state.vz*=1.1;}state.vx*=(double).96f;state.vy*=(double).96f;state.vz*=(double).96f;if(h.ground()){state.vx*=(double).7f;state.vz*=(double).7f;}}
 private SourceSacredRules1120(){}
}
