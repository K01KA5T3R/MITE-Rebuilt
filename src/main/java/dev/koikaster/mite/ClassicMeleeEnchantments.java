package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** uf/tm/aaw/abh, retaining57D's removal of weapon-material counters. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicMeleeEnchantments {
 private static final ThreadLocal<DamageSource> RAW_SOURCE=new ThreadLocal<>();
 public static boolean alreadyScaled(DamageSource source){return RAW_SOURCE.get()==source;}
 private static boolean hurtRaw(Entity target,DamageSource source,float amount){var previous=RAW_SOURCE.get();RAW_SOURCE.set(source);try{return target.hurt(source,amount);}finally{if(previous==null)RAW_SOURCE.remove();else RAW_SOURCE.set(previous);}}
 public static boolean critical(Player actor){return actor.fallDistance>0&&!actor.onGround()&&!actor.onClimbable()&&!actor.isInWater()&&!actor.hasEffect(MobEffects.BLINDNESS)&&!actor.isPassenger();}
 @SubscribeEvent public static void criticalEvent(net.minecraftforge.event.entity.player.CriticalHitEvent event){boolean critical=critical(event.getEntity());event.setDamageModifier(critical?1.5f:1);event.setResult(critical?net.minecraftforge.eventbus.api.Event.Result.ALLOW:net.minecraftforge.eventbus.api.Event.Result.DENY);}
 public static float rawDamage(Player actor,Entity target){float damage=(float)actor.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);if(damage<=0)return 0;if(critical(actor))damage*=1.5f;damage*=SourceSkills1685.meleeMultiplier(actor);var tool=ClassicToolWear.profile(actor.getMainHandItem());if(target instanceof AbstractSkeleton&&tool!=null&&(tool.kind()==LegacyToolWear.Kind.CUDGEL||tool.kind()==LegacyToolWear.Kind.CLUB||tool.kind()==LegacyToolWear.Kind.WARHAMMER))damage*=2;damage+=EnchantmentHelper.getDamageBonus(actor.getMainHandItem(),target instanceof LivingEntity living?living.getMobType():MobType.UNDEFINED);if(ClassicArchery.suspended(actor)&&damage>1)damage=1+(damage-1)*.5f;return damage;}
 public static boolean playerHit(Object rawTarget,Object rawSource,float ignoredNativeDamage,Object rawActor){var target=(Entity)rawTarget;var actor=(Player)rawActor;var source=(DamageSource)rawSource;float damage=rawDamage(actor,target);if(damage<=0)return false;if(!actor.level().isClientSide&&target instanceof Mob mob){var held=mob.getMainHandItem();if(!held.isEmpty()&&actor.getRandom().nextFloat()<EnchantmentEffects.fraction("disarming",actor.getMainHandItem())){mob.spawnAtLocation(held.copy(),mob.getBbHeight()/2);mob.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);mob.getPersistentData().putInt("MiteDisarmedTicks",40);}}
  boolean harmed=hurtRaw(target,source,damage);
  if(target instanceof SourceRevenant revenant)revenant.onPlayerMeleeAttack(actor);
  else if(target instanceof Mob mob)handBackDamage(actor,mob);
  SourceAchievementActions572.overkill(actor,damage,harmed);if(harmed&&target instanceof LivingEntity living){successful(actor,living,damage);WarhammerArea.afterHit(actor,living);}if(!actor.level().isClientSide&&!actor.isCreative())actor.causeFoodExhaustion(.3f*EnchantmentEffects.endurance(actor));return harmed;
 }
 private static void handBackDamage(Player actor,Mob mob){
  if(actor.level().isClientSide||actor.isCreative()||SourceKnockbackNative.protectsHand(actor))return;
  float chance=0,amount=1;
  if(mob instanceof SourceGelatinousCube cube){chance=1;amount=Math.max(1,cube.sourceSize());}
  else if(mob instanceof net.minecraft.world.entity.monster.Slime slime){chance=1;amount=Math.max(1,slime.getSize());}
  else if(mob instanceof net.minecraft.world.entity.monster.Blaze||mob instanceof Mobs.Undead undead&&Mobs.name(undead).equals("fire_elemental")){chance=1;}
  else if(mob.getTarget()==actor&&!SourceKnockbackNative.weakStrike(actor)){chance=.125f;}
  if(chance>0&&mob.getRandom().nextFloat()<chance){
   var type=actor.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(ClassicDefense.HAND_DAMAGE);
   actor.hurt(new DamageSource(type,mob),amount);
  }
 }
 /** Already-scaled source arithmetic, without recursively proccing the area or extra enchantment heals. */
 static boolean secondaryHit(Player actor,LivingEntity target,float fraction){return hurtRaw(target,actor.damageSources().playerAttack(actor),rawDamage(actor,target)*fraction);}
 public static boolean mobHit(Object rawTarget,Object rawSource,float damage,Object rawActor){var target=(Entity)rawTarget;var actor=(LivingEntity)rawActor;damage=SourceMobFrenzy.meleeDamage(actor,damage);if(!ClassicDefense.scoped(actor))return target.hurt((DamageSource)rawSource,damage);float before=target instanceof LivingEntity living?living.getHealth():0;boolean harmed=target.hurt((DamageSource)rawSource,damage);if(harmed&&target instanceof LivingEntity living){float lost=Math.max(0,before-living.getHealth());successful(actor,living,lost);if(actor instanceof Mob mob)OriginalCreatureStrike.result(mob,target,true,lost);}return harmed;}
 private static void successful(LivingEntity actor,LivingEntity target,float vampiricDamage){if(actor.level().isClientSide)return;int stun=EnchantmentEffects.level("stun",actor.getMainHandItem());if(stun>Math.random()*10)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,stun*50,stun*5),actor);int vampiric=EnchantmentEffects.level("vampiric",actor.getMainHandItem());if(vampiricDamage>0&&target.getMobType()!=MobType.UNDEAD&&!(target instanceof SourceEnderman)&&Math.random()*10<=vampiric)actor.heal(Math.max(1,(int)(vampiricDamage*.5f*Math.random())));}
 public static boolean canSweep(Object stack,Object action){return false;}
 public static void nativeFatigue(Object actor,float ignored){}
 public static boolean canPickUp(Object rawActor){return ((LivingEntity)rawActor).getPersistentData().getInt("MiteDisarmedTicks")<=0;}
 @SubscribeEvent public static void disarmedTick(LivingEvent.LivingTickEvent event){var actor=event.getEntity();if(actor.level().isClientSide)return;var data=actor.getPersistentData();int ticks=data.getInt("MiteDisarmedTicks");if(ticks>1)data.putInt("MiteDisarmedTicks",ticks-1);else if(ticks==1)data.remove("MiteDisarmedTicks");}
 public static boolean originalThorns(Object rawActor){return rawActor instanceof LivingEntity living&&ClassicDefense.scoped(living);}
 public static void thorns(Object rawWearer,Object rawAttacker){if(!(rawWearer instanceof LivingEntity wearer)||wearer.level().isClientSide||!ClassicDefense.scoped(wearer))return;int level=0;ItemStack first=ItemStack.EMPTY;for(var stack:wearer.getArmorSlots()){if(!WorldContentEvents.effectAllowed(wearer,stack))continue;int current=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.THORNS,stack);if(current>0){if(first.isEmpty())first=stack;level=Math.max(level,current);}}boolean proc=level>0&&wearer.getRandom().nextFloat()<.15f*level;if(proc&&rawAttacker instanceof Entity attacker){var source=wearer instanceof Player player?wearer.damageSources().playerAttack(player):wearer.damageSources().mobAttack(wearer);hurtRaw(attacker,source,level>10?level-10:1+wearer.getRandom().nextInt(4));}if(!first.isEmpty())ClassicDefense.wearItem(wearer,wearer.damageSources().generic(),first,proc?3:1);}
 public static double blastKnockback(Object rawActor,double amount){var actor=(LivingEntity)rawActor;if(BossCombatTuning.knockbackImmune(actor))return 0;int level=EnchantmentHelper.getEnchantmentLevel(Enchantments.BLAST_PROTECTION,actor);return level>0?amount-net.minecraft.util.Mth.floor(amount*(level*.15f)):amount;}
 private ClassicMeleeEnchantments(){}
}
