package dev.koikaster.mite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
/** wp/uh launch parameters and original arrow identity; hit/recovery specialties remain a separate combat port. */
public final class ClassicArchery {
 public static int maxPull(ItemStack bow){return 20-Math.round(EnchantmentEffects.fraction("quickness",bow)*10);}
 public static float power(int ticks,Object rawBow){var bow=(ItemStack)rawBow;if(!ClassicToolCatalogue.sourceItem(bow))return BowItem.getPowerForTime(ticks);float fraction=Math.min(ticks/(float)maxPull(bow),1);return Math.min((fraction*fraction+fraction*2)/3,1);}
 public static boolean suspended(Entity actor){return !actor.isPassenger()&&!actor.onGround()&&(actor.isInWater()||actor.isInLava());}
 public static void shoot(Object rawArrow,Object rawShooter,float x,float y,float roll,float velocity,float inaccuracy,Object rawBow){
  var arrow=(AbstractArrow)rawArrow;var shooter=(Entity)rawShooter;var bow=(ItemStack)rawBow;
  SourceSpecificImmunities.captureLauncher(arrow,bow);
  if(ClassicToolCatalogue.sourceItem(bow)&&bow.getItem() instanceof BowItem){velocity*=ClassicToolCatalogue.bowVelocity(ClassicToolCatalogue.material(bow));if(shooter instanceof Player p){if(p.getProjectile(bow).is(Items.ARROW))arrow.setBaseDamage(ClassicToolCatalogue.arrowDamage("flint"));int trueFlight=Math.round(EnchantmentEffects.fraction("true_flight",bow)*40);inaccuracy=SourceBowRules815.wander(PlayerState.level(p),false,true,trueFlight,suspended(p));}}
  arrow.shootFromRotation(shooter,x,y,roll,velocity,inaccuracy);
 }
 public static Object pickup(Object original,Object rawArrow){var arrow=(AbstractArrow)rawArrow;String name=arrow.getPersistentData().getString("R195ArrowItem");var id=ResourceLocation.tryParse(name);var item=id==null?null:net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);return item instanceof ArrowItem?new ItemStack(item):original;}
 public static int arrowIndex(ItemStack stack){String material=stack.getItem()==Items.ARROW?"flint":ClassicToolCatalogue.material(stack);for(int i=0;i<ClassicToolCatalogue.ARROW_MATERIALS.length;i++)if(ClassicToolCatalogue.ARROW_MATERIALS[i].equals(material))return i;return 0;}
 public static boolean hit(Object rawTarget,Object rawSource,float damage,Object rawArrow){var target=(Entity)rawTarget;var arrow=(AbstractArrow)rawArrow;boolean harmed=target.hurt((net.minecraft.world.damagesource.DamageSource)rawSource,damage);if(harmed&&!arrow.level().isClientSide&&target instanceof net.minecraft.world.entity.LivingEntity living&&arrow.getOwner() instanceof net.minecraft.world.entity.LivingEntity shooter){var launcher=shooter.getMainHandItem();float poison=EnchantmentEffects.fraction("poison",launcher);if(poison>0&&arrow.level().random.nextFloat()<poison)living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,160+Math.round(poison*240),0),shooter);}return harmed;}
 public static int fortuneWait(int wait,Object rawHook){var hook=(FishingHook)rawHook;var owner=hook.getPlayerOwner();if(owner==null)return wait;var rod=owner.getMainHandItem();if(!(rod.getItem() instanceof FishingRodItem))rod=owner.getOffhandItem();if(wait>0&&SourceSkills1685.enabled(owner.level())&&!SourceSkills1685.has(owner,SourceSkillRules1685.FISHING))wait*=2;for(int i=EnchantmentEffects.level("fishing_fortune",rod);i>0;i--)wait=wait*9/10;return wait;}
 private ClassicArchery(){}
}
