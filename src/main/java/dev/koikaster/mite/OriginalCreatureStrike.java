package dev.koikaster.mite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Source species branches connected after actual melee damage in0.25.
 * Caller must pass actual health loss and the successful result AFTER armor/absorption.
 * Remaining species, sounds, AI and damage-immunity branches require the full species port.
 */
public final class OriginalCreatureStrike {
 public static void result(Mob actor,Entity victim,boolean affected,float healthLost){
  if(actor.level().isClientSide||!affected||victim.isRemoved()||victim instanceof LivingEntity living&&!living.isAlive())return;
  if(actor.getType()==EntityType.CAVE_SPIDER){if(healthLost>0&&victim instanceof LivingEntity target)target.addEffect(new MobEffectInstance(MobEffects.POISON,480,0),actor);return;}
  var id=ForgeRegistries.ENTITY_TYPES.getKey(actor.getType());if(id==null||!id.getNamespace().equals(MiteReforged.ID))return;
  if(id.getPath().equals("shadow")){if(victim instanceof ServerPlayer player)OriginalShadowVision.add(player,2f*(1-ClassicDefense.silverCoverage(player)*.5f));if(healthLost>0&&victim instanceof LivingEntity target)target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600,0),actor);return;}
  if(actor instanceof Mobs.Undead earth&&earth.earthBody()){if(earth.originalEarth().magma()&&actor.getRandom().nextFloat()<.4f)victim.setSecondsOnFire(1+actor.getRandom().nextInt(8));return;}
  if(healthLost<=0)return;
  switch(id.getPath()){
   case "ghoul"->{if(victim instanceof LivingEntity target)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,5),actor);}
   case "wight"->{if(victim instanceof ServerPlayer player&&actor.getRandom().nextFloat()<.4f){int drain=Math.max((PlayerState.level(player)+1)*10,20);drain=Math.round(drain*(1-ClassicDefense.silverCoverage(player)*.5f));PlayerState.addXp(player,-drain);}}
   case "fire_elemental"->victim.setSecondsOnFire(6);
   case "wood_spider"->{if(victim instanceof LivingEntity target)target.addEffect(new MobEffectInstance(MobEffects.POISON,240,0),actor);}
   case "black_widow_spider"->{if(victim instanceof LivingEntity target)target.addEffect(new MobEffectInstance(MobEffects.POISON,960,0),actor);}
   case "demon_spider"->{if(victim instanceof LivingEntity target){target.addEffect(new MobEffectInstance(MobEffects.POISON,480,0),actor);target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,5),actor);}}
  }
 }
 private OriginalCreatureStrike(){}
}
