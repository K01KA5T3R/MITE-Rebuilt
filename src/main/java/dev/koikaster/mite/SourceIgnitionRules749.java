package dev.koikaster.mite;

/** BlockSpark and xw control flow. Native permissions/storage/FX are separate adapters. */
public final class SourceIgnitionRules749 {
 public interface Spark {
  boolean portal();void schedule(int ticks);boolean adjacentFuel();boolean netherrackBelow();void fire();void remove();
 }
 public static void added(Spark host){if(host.portal())return;host.schedule(2);}
 public static boolean tick(Spark host){if(host.adjacentFuel()||host.netherrackBelow())host.fire();else host.remove();return true;}
 public interface BlockUse {
  boolean blockHit();boolean tnt();boolean neighborAir();boolean editNeighbor();boolean server();
  void primeTnt();void spark();void swing();void sound();void wear();
 }
 public static boolean blockUse(BlockUse host){
  if(!host.blockHit())return false;
  if(host.tnt()){if(host.server())host.primeTnt();}
  else if(host.neighborAir()&&host.editNeighbor()){if(host.server())host.spark();}
  else return false;
  if(!host.server())host.swing();else{host.sound();host.wear();}return true;
 }
 public enum Target {CHICKEN,SHEEP,WOLF,DIRE_WOLF,HELLHOUND,OTHER}
 public interface EntityUse {
  boolean server();Target target();boolean sheared();boolean tame();void targetPlayer();void ignite(int seconds);void sound();void wear();
 }
 public static boolean entityUse(EntityUse host){
  if(host.server()){
   boolean ignite=false;
   if(host.target()==Target.CHICKEN)ignite=true;
   else if(host.target()==Target.SHEEP){if(!host.sheared())ignite=true;}
   else if(host.target()==Target.WOLF||host.target()==Target.DIRE_WOLF||host.target()==Target.HELLHOUND){
    if(host.target()==Target.HELLHOUND)host.targetPlayer();
    else if(host.target()==Target.DIRE_WOLF){if(host.tame())ignite=true;host.targetPlayer();}
    else ignite=true;
   }
   if(ignite)host.ignite(6);host.sound();host.wear();
  }
  return true;
 }
 private SourceIgnitionRules749(){}
}
