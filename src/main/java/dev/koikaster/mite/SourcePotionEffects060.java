package dev.koikaster.mite;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** ni's shared consumers reached by Witch potions. Uses actual affected actor
 * scope, so foreign entity types retain their native effect implementation. */
public final class SourcePotionEffects060 {
 public static boolean duration(Object rawEffect,int duration,int amplifier,Object rawActor){
  var effect=(MobEffect)rawEffect;var actor=(LivingEntity)rawActor;
  if(effect==MobEffects.POISON&&ClassicDefense.scoped(actor)){int interval=100>>amplifier;if(interval<1)interval=1;return duration%interval==0;}
  return effect.isDurationEffectTick(duration,amplifier);
 }
 public static boolean poisonTick(Object rawEffect,Object rawActor,int amplifier){
  if(rawEffect!=MobEffects.POISON||!(rawActor instanceof LivingEntity actor)||!ClassicDefense.scoped(actor))return false;
  // ni.a has no modern health>1 exception: poison can kill in R195.
  if(!actor.level().isClientSide)actor.hurt(ClassicDefense.poison(actor),1);return true;
 }
 public static void attributes(Object rawEffect,Object rawActor,int amplifier){
  if(rawEffect!=MobEffects.WEAKNESS&&rawEffect!=MobEffects.DAMAGE_BOOST||!(rawActor instanceof LivingEntity actor)||!ClassicDefense.scoped(actor))return;
  var effect=(MobEffect)rawEffect;var attribute=actor.getAttribute(Attributes.ATTACK_DAMAGE);if(attribute==null)return;
  var original=effect.getAttributeModifiers().get(Attributes.ATTACK_DAMAGE);if(original==null)return;
  attribute.removeModifier(original.getId());attribute.addTransientModifier(new AttributeModifier(original.getId(),"R195 "+effect.getDescriptionId()+" "+amplifier,(effect==MobEffects.WEAKNESS?-.4:.4)*(double)(amplifier+1),AttributeModifier.Operation.MULTIPLY_BASE));
 }
 public static void clearActualAttribute(Object rawEffect,Object rawActor){
  if(rawEffect!=MobEffects.WEAKNESS&&rawEffect!=MobEffects.DAMAGE_BOOST||!(rawActor instanceof LivingEntity actor)||!ClassicDefense.scoped(actor))return;
  var attribute=actor.getAttribute(Attributes.ATTACK_DAMAGE);if(attribute==null)return;
  var original=((MobEffect)rawEffect).getAttributeModifiers().get(Attributes.ATTACK_DAMAGE);if(original==null)return;
  // Native removeModifier(template) selects the template's ADDITION bucket,
  // which would leave our MULTIPLY_BASE modifier behind. Resolve by UUID first
  // so removal uses the actual installed operation, as R195's own template did.
  attribute.removeModifier(original.getId());
 }
 private SourcePotionEffects060(){}
}
