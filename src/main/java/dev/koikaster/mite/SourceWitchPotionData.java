package dev.koikaster.mite;

/** tv/ni subtype aliases evaluated by the original inherited zp bytecode.
 * Splash durations already include zp's round(duration*.75+.5), before uu's
 * separate distance falloff. Do not apply that .75 a second time.
 */
public final class SourceWitchPotionData {
 public record Effect(int id,int duration,int amplifier,boolean splash){}
 public static Effect effect(int subtype){return switch(subtype){
  case 16307,8227->new Effect(12,3600,0,false);
  case 16341,8261->new Effect(6,1,0,false);
  case 16274->new Effect(1,3600,0,false);
  case 32732,16460->new Effect(7,1,0,true);
  case 32698,16426->new Effect(2,1351,0,true);
  case 32660,16388->new Effect(19,676,0,true);
  case 32696,16424->new Effect(18,1351,0,true);
  default->throw new IllegalArgumentException("Not a source Witch subtype: "+subtype);
 };}
 private SourceWitchPotionData(){}
}
