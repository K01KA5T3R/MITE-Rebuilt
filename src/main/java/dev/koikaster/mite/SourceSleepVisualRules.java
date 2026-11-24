package dev.koikaster.mite;
/** avj's black255 fade and bfe's rain-volume factor. The native220 blue fade
 * reverses above100 and therefore cannot consume R195's sleeping counter110. */
public final class SourceSleepVisualRules {
 public static int color(int counter){return counter<=0?0:Math.min(255*counter/100,255)<<24;}
 public static float rainFactor(int counter){return Math.max(1-(float)counter/50,0);}
 private SourceSleepVisualRules(){}
}
