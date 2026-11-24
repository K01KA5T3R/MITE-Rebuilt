package dev.koikaster.mite;
/** yi/ItemRock's ordered callbacks; parent finish and experience storage remain separate transports. */
public final class SourceUseOrder1066 {
 public interface Milk {boolean server();void clearEffects();void addFood();}
 public static void milk(Milk h){if(h.server()){h.clearEffects();h.addFood();}}
 public interface Mineral {int value();boolean server();void breakingEffect();void convert();void experience(int value);void bob();}
 public static boolean mineral(Mineral h){int value=h.value();if(value<1)return false;if(h.server()){h.breakingEffect();h.convert();h.experience(value);}else h.bob();return true;}
 private SourceUseOrder1066(){}
}
