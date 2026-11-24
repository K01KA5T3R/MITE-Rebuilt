package dev.koikaster.mite;
/** ud.getNextHotbarOrInventorySlotContainingMostSimilarItem and trySwitchItemOrRestock. */
public final class SourceRestockRules997 {
 public interface Scores {boolean previousKnown();int selected();int similarity(int slot);}
 public static int choose(Scores h){
  if(!h.previousKnown())return -1;int closest=0,best=-1;
  for(int i=h.selected();i<9;i++){int score=h.similarity(i);if(score>=1&&score>closest){best=i;closest=score;}}
  for(int i=0;i<h.selected();i++){int score=h.similarity(i);if(score>=1&&score>closest){best=i;closest=score;}}
  for(int i=9;i<36;i++){int score=h.similarity(i);if(score>=1&&score>closest){best=i;closest=score;}}
  return best;
 }
 public static boolean inventoryWindow(long resetMs,long nowMs){long left=resetMs-nowMs;return left>0&&left<=3000L-250L;}
 public interface Switch {boolean client();void wrongSide();boolean previousKnown();void missingPrevious();boolean immediate();void pending(boolean value);int choose();int selected();void selectHotbar(int slot);void equipChanged();void sendSelection();boolean inventoryWindow();boolean selectedOccupied();void moveInventoryToSelected(int slot);void sendMovedSlots(int slot);void clearLastUsed();void noteLastUsed();}
 public static boolean attempt(Switch h){
  if(!h.client()){h.wrongSide();return false;}
  if(!h.previousKnown()){h.missingPrevious();return false;}
  if(h.immediate()){
   h.pending(false);int slot=h.choose();if(slot==-1||slot==h.selected())return false;
   if(slot<9){h.selectHotbar(slot);h.equipChanged();h.sendSelection();return true;}
   if(!h.inventoryWindow()||h.selectedOccupied())return false;
   h.moveInventoryToSelected(slot);h.sendMovedSlots(slot);h.clearLastUsed();return true;
  }
  h.noteLastUsed();h.pending(true);return false;
 }
 private SourceRestockRules997(){}
}
