package dev.koikaster.mite;
/** abw's ambient countdown and per-active-chunk candidate. No world mutation. */
public final class SourceCaveSoundRules {
 public static final class State{public int remaining,sequence;public State(int countdown,int lcg){remaining=countdown;sequence=lcg;}}
 public interface World{
  default int minY(){return 0;}default int maxY(){return 256;}
  boolean client();boolean air(int x,int y,int z);int light(int x,int y,int z);int sky(int x,int y,int z);double nearestAlivePlayerSquared(int x,int y,int z);
  int nextInt(int bound);float nextFloat();void play(int x,int y,int z,float volume,float pitch);
 }
 public static int countdown(int value){return value>0?value-1:value;}
 public static int delay(boolean first,java.util.function.IntUnaryOperator random){return random.applyAsInt(12000)+(first?0:6000);}
 public static boolean sample(State state,World world,int blockX,int blockZ){
  if(state.remaining!=0||world.client())return false;
  state.sequence=state.sequence*3+1013904223;int bits=state.sequence>>2,x=(bits&15)+blockX,z=(bits>>8&15)+blockZ,y=sampleY(state.sequence,world.minY(),world.maxY());
  if(!world.air(x,y,z)||world.light(x,y,z)>world.nextInt(8)||world.sky(x,y,z)>0)return false;
  double nearest=world.nearestAlivePlayerSquared(x,y,z);if(nearest<0||nearest>=64||nearest<=4)return false;
  world.play(x,y,z,.7f,.8f+world.nextFloat()*.2f);state.remaining=delay(false,world::nextInt);return true;
 }
 /** Preserve the source selector for original bounds; cover the retained modern terrain otherwise. */
 public static int sampleY(int sequence,int min,int max){
  if(min==0&&max==256)return sequence>>18&127;
  long height=(long)max-min;if(height<=0||height>Integer.MAX_VALUE)throw new IllegalArgumentException("World height");
  return min+(int)(((long)(sequence>>>16)*height)>>>16);
 }
 private SourceCaveSoundRules(){}
}
