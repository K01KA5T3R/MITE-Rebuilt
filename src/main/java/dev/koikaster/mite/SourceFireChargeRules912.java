package dev.koikaster.mite;

/** xs.onItemRightClick. Permissions, world storage and sided interaction are adapters. */
public final class SourceFireChargeRules912 {
 public interface Use {
  boolean blockHit(); boolean neighborAir(); boolean editNeighbor();
  boolean client(); boolean creative();
  void swing(); void sound(); void fire(); void consume();
 }
 public static boolean use(Use host){
  if(!host.blockHit()||!host.neighborAir())return false;
  if(!host.editNeighbor())return false;
  if(host.client())host.swing();
  else{host.sound();host.fire();if(!host.creative())host.consume();}
  return true;
 }
 private SourceFireChargeRules912(){}
}
