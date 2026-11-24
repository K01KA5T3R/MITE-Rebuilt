package dev.koikaster.mite;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Original Damage.setKnockbackOnly flag, admitted through the actual native living hurt owner. */
public final class SourceKnockbackOnly838 extends DamageSource {
 private SourceKnockbackOnly838(DamageSource source){super(source.typeHolder(),source.getDirectEntity(),source.getEntity());}
 static void hit(Entity target,Entity projectile,Entity owner){
  if(target.level().isClientSide||!(target instanceof LivingEntity living))return;
  var damage=new SourceKnockbackOnly838(projectile.damageSources().thrown(projectile,owner));
  try(var frame=SourceParentHurtFrame.beginObserver(living,outcome->{})){living.hurt(damage,1f);frame.finish();}
 }
 /** Runs after source knockback and before native health/gear/iframe handling. */
 static void finish(LivingEntity actor,DamageSource source){
  if(SourceParentHurtFrame.wasKnockedBack(actor)){
   if(actor.invulnerableTime<=10){((SourceSnowballParentAccess)actor).mite$snowballHurtSound(source);SourceSnowballEffects.noTint(actor);}
   actor.walkAnimation.setSpeed(1.5f);((SourceKnockbackBody)actor).mite$attackedYaw(0);
  }
 }
}
