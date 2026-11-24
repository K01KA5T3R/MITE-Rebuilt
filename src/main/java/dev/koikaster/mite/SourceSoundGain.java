package dev.koikaster.mite;
/** R195 bln.a(String,float,float): positive input, clamp to one, then quarter gain. */
public final class SourceSoundGain {
 public static float ui(float volume){return volume>0f?(volume>1f?1f:volume)*.25f:0f;}
 private SourceSoundGain(){}
}
