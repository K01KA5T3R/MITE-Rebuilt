package dev.koikaster.mite;
import java.util.function.BooleanSupplier;import java.util.function.DoubleSupplier;
/** Literal sf/bln/vf sound decisions; no AI, recipes or persistent state. */
public final class SourceContextSoundRules {
 public static boolean howl(boolean day,boolean attacking,boolean fleeing,DoubleSupplier roll,BooleanSupplier outdoors){return !day&&!attacking&&!fleeing&&roll.getAsDouble()<.04f&&outdoors.getAsBoolean();}
 public static float howlVolume(double distance,float volume){return distance<16||distance>64||volume<=0?0:volume*(1f/((float)distance-16f+1f));}
 public static boolean boil(int before,int current){return before!=current&&before==0;}
 private SourceContextSoundRules(){}
}
