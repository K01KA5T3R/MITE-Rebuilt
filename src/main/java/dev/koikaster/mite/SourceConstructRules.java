package dev.koikaster.mite;
/** Retained of potion admissibility and non-player air recovery. sd alone
 * overrides underwater decrement; sb alone disables periodic parent healing. */
public final class SourceConstructRules {
 public interface FireHost {
  boolean removed();boolean server();void contactDamage(int amount);boolean extinguishBox();void quench();boolean wet();
  int fireTicks();void fireTicks(int value);void ignite(int seconds);int protectedFireTicks(int value);
 }
 public static boolean potion(boolean biological,boolean regeneration,boolean poison,boolean wither){return !regeneration&&!poison&&!wither||biological;}
 public static int recoveredAir(){return 300;}
 public static boolean ironHeal(boolean server,int offsetTicks){return server&&offsetTicks%1000==0;}
 /** nn.spentTickInFire for this living sb: canDouse/canCatch are true,ss=false. */
 public static void spentFire(FireHost h){if(h.removed())return;h.contactDamage(1);if(h.server()&&h.extinguishBox()&&!h.removed())h.quench();if(!h.wet()){int next=h.fireTicks()+1;h.fireTicks(next);if(next==0)h.ignite(8);}}
 public static void ignite(FireHost h,int seconds){if(seconds<1)return;int ticks=h.protectedFireTicks(seconds*20+10);if(h.fireTicks()<ticks)h.fireTicks(ticks);}
 private SourceConstructRules(){}
}
