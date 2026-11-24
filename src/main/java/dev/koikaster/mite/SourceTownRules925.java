package dev.koikaster.mite;
import java.util.function.DoubleSupplier;
/** The selected beg/beq construction and beq tick arithmetic; entity root RNG is an adapter. */
public final class SourceTownRules925 {
 public interface Random {int nextInt();float nextFloat();}
 public record Initial(double vx,double vy,double vz,float size,int lifetime,int sharedIndex,float baseColor){}
 public static Initial create(float[] table,int sharedIndex,Random random,DoubleSupplier math,double vx,double vy,double vz){
  int index=random.nextInt();
  float jitterU=table[++index&32767]*3f,jitterV=table[++index&32767]*3f;
  float size=table[++index&32767]+1f;
  int baseLifetime=(int)(4f/(table[++index&32767]*.9f+.1f));
  vx=vx+(double)(table[++index&32767]*.8f)-(double).4f;
  vy=vy+(double)(table[++index&32767]*.8f)-(double).4f;
  vz=vz+(double)(table[++index&32767]*.8f)-(double).4f;
  float scale=(table[++index&32767]*(float)math.getAsDouble()+1f)*.15f;
  float length=(float)Math.sqrt(vx*vx+vy*vy+vz*vz);
  vx=vx/(double)length*(double)scale*(double).4f;
  vy=vy/(double)length*(double)scale*(double).4f+(double).1f;
  vz=vz/(double)length*(double)scale*(double).4f;
  float unusedBaseColor=table[++sharedIndex&32767]*.1f+.2f;
  size*=random.nextFloat()*.6f+.5f;
  vx*=(double).02f;vy*=(double).02f;vz*=(double).02f;
  int lifetime=(int)(20.0/((double)table[++sharedIndex&32767]*.8+.2));
  return new Initial(vx,vy,vz,.1f*size,lifetime,sharedIndex,unusedBaseColor);
 }
 private SourceTownRules925(){}
}
