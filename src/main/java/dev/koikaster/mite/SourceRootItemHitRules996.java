package dev.koikaster.mite;
import java.util.function.IntUnaryOperator;
/** yc.a(ItemStack,Living,Living). Inventory/effect transports retain their original ordering. */
public final class SourceRootItemHitRules996 {
 public enum Kind {OTHER,STICK,BONE}
 public interface Hit {boolean targetOnClient();void unexpectedClientCall();boolean attackerIsPlayer();boolean creative();boolean sameAsHeldStack();int selectedSlot();void breakingEffect(int selectedSlot);void consumeOneHeld();}
 public static boolean hit(Kind kind,Hit h,IntUnaryOperator itemRandom){
  if(h.targetOnClient())h.unexpectedClientCall();
  if(!h.attackerIsPlayer()||h.creative())return false;
  int bound;if(kind==Kind.STICK)bound=50;else if(kind==Kind.BONE)bound=100;else return false;
  if(itemRandom.applyAsInt(bound)==0){h.breakingEffect(h.sameAsHeldStack()?h.selectedSlot():-1);h.consumeOneHeld();return true;}
  return false;
 }
 private SourceRootItemHitRules996(){}
}
