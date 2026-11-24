package dev.koikaster.mite;

/** ItemGelatinousSphere.onItemRightClick; flight and contact have separate owners. */
public final class SourceGelatinousUse782 {
 private SourceGelatinousUse782(){}
 public interface Use {
  boolean server(); int subtype(); boolean creative(); void consume();
  float endurance(); void exhaust(float value); float itemRandom();
  void bow(float volume,float pitch); void spawn(int subtype); void bob();
 }
 public static boolean use(Use action){
  if(action.server()){
   int subtype=action.subtype();
   if(!action.creative()){action.consume();action.exhaust(.25f*action.endurance());}
   action.bow(.5f,.4f/(action.itemRandom()*.4f+.8f));
   action.spawn(subtype);
  }else action.bob();
  return true;
 }
}
