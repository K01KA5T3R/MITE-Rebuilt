package dev.koikaster.mite;
import java.util.function.IntUnaryOperator;
/** Staged tl leaf only. Native EntityCubic parent and contact hooks unbound. */
public final class OriginalMagmaLife {
 public interface Host {boolean client();boolean wet();boolean immersed();int nextInt(int bound);float nextFloat();void steam(int count);void fizz(float volume,float pitch);void waterHurt(float amount);}
 int fizzTicks;
 public void beforeParent(Host host){
  if(host.client()&&host.wet())host.steam(host.immersed()?10:1);
  if(!host.client()&&host.wet()&&--fizzTicks<=0){host.fizz(.7f,1.6f+(host.nextFloat()-host.nextFloat())*.4f);fizzTicks=host.nextInt(7)+2;if(host.nextInt(host.immersed()?4:16)==0)host.waterHurt(1f);}
 }
 public static int jumpDelay(boolean target,IntUnaryOperator random){return target?20:random.applyAsInt(81)+40;}
 public static float jumpVelocity(int size){return .42f+(float)size*.1f;}
 public static float defense(int size,boolean mundaneBypass){return mundaneBypass?0f:(float)size*2f;}
 public static int experience(int size){return size*3;}
 public static int drops(int size,int looting,IntUnaryOperator random){return size>1?Math.max(0,random.applyAsInt(4+looting)-2):0;}
}
