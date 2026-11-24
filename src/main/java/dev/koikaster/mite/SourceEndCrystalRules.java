package dev.koikaster.mite;
/** sj immunity, fire maintenance and parent-result-driven one-hit destruction. */
public final class SourceEndCrystalRules {
 public interface Defense {boolean parentImmune();boolean melee();boolean item();boolean tool();boolean effectiveAgainstMithrilBlock();}
 public interface Result {void destroyed();}
 public interface Host<R extends Result> {
  R parentDamage();int health();void health(int value);void miteDiscardCrystal();void explode();
  void previousPosition();int animation();void animation(int value);void synchronizeHealth();double x();double y();double z();boolean fire(int x,int y,int z);void fireAt(int x,int y,int z);
 }
 public static boolean immune(Defense h){if(h.parentImmune())return true;return !h.melee()||!h.item()||!h.tool()||!h.effectiveAgainstMithrilBlock();}
 public static <R extends Result> R hurt(Host<R> h){R result=h.parentDamage();if(result==null)return null;h.health(0);if(h.health()<=0){h.miteDiscardCrystal();result.destroyed();h.explode();}return result;}
 public static void tick(Host<?> h){h.previousPosition();h.animation(h.animation()+1);h.synchronizeHealth();int x=(int)Math.floor(h.x()),y=(int)Math.floor(h.y()),z=(int)Math.floor(h.z());if(!h.fire(x,y,z))h.fireAt(x,y,z);}
 private SourceEndCrystalRules(){}
}
