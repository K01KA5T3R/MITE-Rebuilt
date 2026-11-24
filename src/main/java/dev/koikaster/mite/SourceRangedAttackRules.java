package dev.koikaster.mite;

/** qn.e: source doubled squared range, two piercing traces and fixed cooldown. */
public final class SourceRangedAttackRules {
 public static final class State {public int cooldown=-1,visible;}
 public interface Host {
  State state();double distanceSquared();boolean piercingLine();float range();int cooldown();
  void stopNavigation();void pursue();void look();boolean eyePassable();void shoot(float power);boolean frenzied();
 }
 public static void tick(Host h){
  var s=h.state();double distance=h.distanceSquared();boolean clear=h.piercingLine();
  s.visible=clear?s.visible+1:0;float limit=h.range()*h.range();limit*=4f;
  if(distance<=(double)limit&&s.visible>=20)h.stopNavigation();else h.pursue();h.look();
  if(--s.cooldown==0){
   if(distance>(double)limit||!clear)return;
   float power=(float)Math.sqrt(distance)/h.range();if(power<.1f)power=.1f;if(power>1f)power=1f;
   if(h.eyePassable())h.shoot(power);
   s.cooldown=h.cooldown();if(h.frenzied())s.cooldown=(int)((float)s.cooldown*.67f);
  }else if(s.cooldown<0){s.cooldown=h.cooldown();if(h.frenzied())s.cooldown=(int)((float)s.cooldown*.67f);}
 }
 private SourceRangedAttackRules(){}
}
