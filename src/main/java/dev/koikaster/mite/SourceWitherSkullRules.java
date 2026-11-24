package dev.koikaster.mite;

/** uv.onImpact: the debuff is independent of damage acceptance. */
public final class SourceWitherSkullRules {
 public interface Host {
  boolean client();boolean entityHit();boolean owner();boolean livingHit();int difficulty();
  boolean damage(float amount,boolean fromOwner);void healOwner(float amount);
  void wither(int ticks,int amplifier);void explode();void discard();
 }
 public static float drag(boolean charged){return charged?.73f:.95f;}
 public static void impact(Host h){
  if(h.client())return;
  if(h.entityHit()){
   if(h.owner()){if(h.damage(8f,true))h.healOwner(5f);}else h.damage(5f,false);
   if(h.livingHit()){
    int seconds=0;if(h.difficulty()>1){if(h.difficulty()==2)seconds=10;else if(h.difficulty()==3)seconds=40;}
    if(seconds>0)h.wither(20*seconds,1);
   }
  }
  h.explode();h.discard();
 }
 private SourceWitherSkullRules(){}
}
