package dev.koikaster.mite;

/** ud's selected conversion, complete-merge probe, insertion and overflow splitting.
 * Stack metadata/equality, slot packets, drops and crash reports are explicit API transports.
 */
public final class SourceInventoryConvert1035 {
 public interface Inventory<S> {
  int size();int selected();int limit();boolean client();boolean creative();
  S get(int slot);void set(int slot,S stack);int count(S stack);void count(S stack,int value);
  int maximum(S stack);boolean damaged(S stack);boolean stackable(S stack);S copy(S stack);
  boolean equal(S first,S second,boolean ignoreTag);void pop(S stack,int ticks);void changed(int slot);
  void restock(S previous);void drop(S stack);void clientConversionError();RuntimeException insertionFailure(S stack,Throwable error);
 }
 public static <S> boolean canCompletelyMerge(Inventory<S> h,S stack){
  int remaining=h.count(stack);
  for(int i=0;i<h.size();i++){S other=h.get(i);if(!h.equal(other,stack,true)||(remaining-=h.maximum(other)-h.count(other))>0)continue;return true;}
  return false;
 }
 private static <S> int empty(Inventory<S> h){for(int i=0;i<h.size();i++)if(h.get(i)==null)return i;return -1;}
 private static <S> int mergeSlot(Inventory<S> h,S stack){
  for(int i=0;i<h.size();i++){S other=h.get(i);if(other==null||!h.stackable(other)||h.count(other)>=h.maximum(other)||h.count(other)>=h.limit()||!h.equal(other,stack,false))continue;return i;}return -1;
 }
 private static <S> int insertPart(Inventory<S> h,S stack){
  if(h.count(stack)==0)return 0;int remaining=h.count(stack);
  if(h.maximum(stack)==1){int slot=empty(h);if(slot<0)return remaining;if(h.get(slot)==null)h.set(slot,h.copy(stack));return 0;}
  int slot=mergeSlot(h,stack);if(slot<0)slot=empty(h);if(slot<0)return remaining;
  if(h.get(slot)==null){h.set(slot,h.copy(stack));return 0;}
  S other=h.get(slot);int moved=remaining;if(remaining>h.maximum(other)-h.count(other))moved=h.maximum(other)-h.count(other);if(moved>h.limit()-h.count(other))moved=h.limit()-h.count(other);
  if(moved==0){if(!h.client())h.changed(slot);return remaining;}
  remaining-=moved;h.count(other,h.count(other)+moved);h.pop(other,5);if(!h.client())h.changed(slot);return remaining;
 }
 public static <S> boolean add(Inventory<S> h,S stack){
  if(stack==null||h.count(stack)==0)return false;
  try{
   int before;
   if(h.damaged(stack)){int slot=empty(h);if(slot>=0){h.set(slot,h.copy(stack));h.pop(h.get(slot),5);h.count(stack,0);return true;}if(h.creative()){h.count(stack,0);return true;}return false;}
   do{before=h.count(stack);h.count(stack,insertPart(h,stack));}while(h.count(stack)>0&&h.count(stack)<before);
   if(h.count(stack)==before&&h.creative()){h.count(stack,0);return true;}return h.count(stack)<before;
  }catch(Throwable error){throw h.insertionFailure(stack,error);}
 }
 public static <S> void addOrDrop(Inventory<S> h,S supplied){
  if(supplied==null||h.count(supplied)<1)return;S stack=h.copy(supplied);
  while(h.count(stack)>h.maximum(stack)){S split=h.copy(stack);h.count(split,h.maximum(stack));addOrDrop(h,split);h.count(stack,h.count(stack)-h.maximum(stack));}
  if(!add(h,stack))h.drop(stack);
 }
 public static <S> void convert(Inventory<S> h,int slot,S input,S created){
  if(h.client()){h.clientConversionError();return;}
  if(h.count(input)==1&&slot==h.selected())h.restock(input);
  h.count(input,h.count(input)-1);
  if(h.count(input)>0||created!=null&&canCompletelyMerge(h,created)){
   if(h.count(input)==0)h.set(h.selected(),null);
   addOrDrop(h,created);
  }else h.set(h.selected(),created);
 }
 private SourceInventoryConvert1035(){}
}
