package dev.koikaster.mite;

/** Direct tv.c before-parent state machine. The host owns native item/effect,
 * attribute and entity operations; this owner retains source RNG and timer order.
 * SourceWitch integration is pending in060: an unwired helper is not acceptance.
 */
public final class SourceWitchLife {
 public interface Host {
  boolean client();boolean drinking();float nextFloat();
  boolean burning();boolean fireResistance();boolean wounded();
  boolean hasTarget();boolean speedEffect();double targetDistanceSquared();
  int beginPotion(int subtype);void finishPotion();void magic();
  Object lastHarmingEntity();boolean player(Object entity);int summonWolves(Object rememberedTarget);
 }
 int drinkTimer,summonCountdown;Object summonTarget;boolean hasSummonedWolves;
 public void beforeParent(Host host){
  if(host.client())return;
  if(host.drinking()){
   if(drinkTimer--<=0)host.finishPotion();
  }else{
   int subtype=-1;
   if(host.nextFloat()<.15f&&host.burning()&&!host.fireResistance())subtype=16307;
   else if(host.nextFloat()<.01f&&host.wounded())subtype=16341;
   else if(host.nextFloat()<.25f&&host.hasTarget()&&!host.speedEffect()&&host.targetDistanceSquared()>121)subtype=16274;
   // This duplicate branch and its independent roll are present in tv.c.
   else if(host.nextFloat()<.25f&&host.hasTarget()&&!host.speedEffect()&&host.targetDistanceSquared()>121)subtype=16274;
   if(subtype>-1)drinkTimer=host.beginPotion(subtype);
  }
  if(host.nextFloat()<7.5E-4f)host.magic();
  if(summonCountdown>0){
   if(--summonCountdown==0)hasSummonedWolves=host.summonWolves(summonTarget)>0||hasSummonedWolves;
  }else if(!hasSummonedWolves&&host.player(host.lastHarmingEntity())){
   summonTarget=host.lastHarmingEntity();summonCountdown=60;
  }
 }
}
