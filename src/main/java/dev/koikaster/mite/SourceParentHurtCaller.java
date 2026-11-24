package dev.koikaster.mite;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Actual hurt caller. Snowball selection and Phase's pre-parent evade
 * stay outside this method. The returned native boolean is preserved exactly;
 * it is deliberately not used as EntityDamageResult.negative.
 */
public final class SourceParentHurtCaller {
 public static boolean hurt(Object rawActor,Object rawSource,float amount,Object rawLife){
  var bridge=(SourceParentNativeHurt)rawActor;var source=(DamageSource)rawSource;
  if(rawLife==null)return bridge.mite$nativeParentHurt(source,amount);
  var actor=(LivingEntity)rawActor;var life=(SourceParentArachnidLife)rawLife;
  try(var frame=SourceParentHurtFrame.begin(actor,outcome->life.afterDamage(source,outcome))){
   boolean accepted=bridge.mite$nativeParentHurt(source,amount);
   frame.finish();return accepted;
  }
 }
 private SourceParentHurtCaller(){}
}
