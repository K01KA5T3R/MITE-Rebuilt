package dev.koikaster.mite;
/** ud.convertOneItem's null-output path, including restock notification before decrement. */
public final class SourceHeldConsumeRules997 {
 public interface Host {boolean client();void unexpectedClientCall();int count();int slot();int selected();void restockNotification();int decrement();void clearSelected();void addNullOutput();}
 public static void consume(Host h){
  if(h.client()){h.unexpectedClientCall();return;}
  if(h.count()==1&&h.slot()==h.selected())h.restockNotification();
  if(h.decrement()>0)h.addNullOutput();else h.clearSelected();
 }
 private SourceHeldConsumeRules997(){}
}
