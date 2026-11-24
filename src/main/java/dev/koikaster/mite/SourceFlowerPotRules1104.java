package dev.koikaster.mite;

/** The two R195 pot activation bodies. Metadata/registry, drop and world APIs are transports. */
public final class SourceFlowerPotRules1104 {
 public interface Host<S> {
  S held();int baseMetadata(S stack);int multiMetadata(S stack);boolean server();int metadata();
  void drop(int metadata);float randomFloat();void pop(float volume,float pitch);
  void switchPot(boolean multi,int metadata,int flags);void changeMetadata(int metadata,int flags);
  boolean creative();void convertHeld();
 }
 public static <S> boolean base(Host<S> h){
  S stack=h.held();if(stack==null)return false;
  if(h.multiMetadata(stack)!=0){
   if(h.server()){
    int metadata=h.metadata();if(metadata!=0){h.drop(metadata);h.pop(.1f,((h.randomFloat()-h.randomFloat())*.7f+1f)*2f);}
    h.switchPot(true,h.multiMetadata(stack),2);if(!h.creative())h.convertHeld();
   }
   return true;
  }
  int incoming=h.baseMetadata(stack);if(incoming==0)return false;int metadata=h.metadata();if(metadata==incoming)return false;
  if(h.server()){
   if(metadata!=0){h.drop(metadata);h.pop(.1f,((h.randomFloat()-h.randomFloat())*.7f+1f)*2f);}
   h.changeMetadata(incoming,2);if(!h.creative())h.convertHeld();
  }
  return true;
 }
 public static <S> boolean multi(Host<S> h){
  S stack=h.held();if(stack==null)return false;
  if(h.baseMetadata(stack)!=0){
   if(h.server()){
    int metadata=h.metadata();if(metadata!=0){h.drop(metadata);h.pop(.1f,((h.randomFloat()-h.randomFloat())*.7f+1f)*2f);}
    h.switchPot(false,h.baseMetadata(stack),2);if(!h.creative())h.convertHeld();
   }
   return true;
  }
  int incoming=h.multiMetadata(stack);if(incoming==0)return false;int metadata=h.metadata();if(metadata==incoming)return false;
  if(h.server()){
   if(metadata!=0){h.drop(metadata);h.pop(.1f,((h.randomFloat()-h.randomFloat())*.7f+1f)*2f);}
   h.changeMetadata(incoming,2);if(!h.creative())h.convertHeld();
  }
  return true;
 }
 private SourceFlowerPotRules1104(){}
}
