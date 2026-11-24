package dev.koikaster.mite;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;

/** Native transport for tm's outgoing strike. Deliberately bypasses modern
 * Zombie/Mob hit side effects; the victim still owns its real hurt pipeline. */
public final class SourceMobStrikeNative {
 public static final class StrikeDamage extends DamageSource {
  public final boolean fireAspect;
  StrikeDamage(Mob actor,boolean fireAspect){super(actor.damageSources().mobAttack(actor).typeHolder(),actor);this.fireAspect=fireAspect;}
 }
 public static boolean attack(Monster actor,Entity target){
  if(actor.level().isClientSide)return false;
  ItemStack held=actor.getMainHandItem();
  var result=SourceMobStrike.attack(new SourceMobStrike.Host(){
   public boolean decoy(){return actor instanceof SourceEnderman e&&e.sourceDecoy();}
   // uf.isImmuneByGrace is literally false in R195.
   public boolean immuneByGrace(){return false;}
   public float attack(){return (float)actor.getAttributeValue(Attributes.ATTACK_DAMAGE);}
   public boolean frenzied(){return SourceMobFrenzy.active(actor);}
   public float baseAttack(){return (float)actor.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();}
   public boolean livingTarget(){return target instanceof LivingEntity;}
   public float enchantmentDamage(){return EnchantmentHelper.getDamageBonus(held,((LivingEntity)target).getMobType());}
   public int knockback(){return EnchantmentHelper.getKnockbackBonus(actor);}
   public int fireAspect(){return EnchantmentHelper.getFireAspect(actor);}
   public SourceMobStrike.Result hurt(float damage,boolean fire){
    var source=new StrikeDamage(actor,fire);
    if(target instanceof LivingEntity living){
     float before=living.getHealth();
     try(var frame=SourceParentHurtFrame.beginObserver(living,outcome->{})){
      target.hurt(source,damage);var outcome=frame.finish();
      return new SourceMobStrike.Result(outcome.negativelyAffected(),Math.max(0,before-living.getHealth()));
     }
    }
    return new SourceMobStrike.Result(target.hurt(source,damage),0);
   }
   public void push(int knockback){float yaw=actor.getYRot()*(float)Math.PI/180f;target.push(-Mth.sin(yaw)*knockback*.5f,.1,Mth.cos(yaw)*knockback*.5f);actor.setDeltaMovement(actor.getDeltaMovement().multiply(.6,1,.6));}
   public void ignite(int seconds){target.setSecondsOnFire(seconds);}
   public boolean burning(){return actor.isOnFire();}public boolean hasHeldItem(){return !actor.getMainHandItem().isEmpty();}
   public float nextFloat(){return actor.getRandom().nextFloat();}public int difficulty(){return actor.level().getDifficulty().getId();}
   public void thorns(){SourceMobStrikeNative.thorns(actor,(LivingEntity)target);}
   public int stun(){return EnchantmentEffects.level("stun",actor.getMainHandItem());}public double globalRandom(){return Math.random();}
   public void stun(int ticks,int amplifier){((LivingEntity)target).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,amplifier),actor);}
   public boolean biologicalTarget(){return ((LivingEntity)target).getMobType()!=MobType.UNDEAD&&!(target instanceof SourceEnderman);}
   public int vampiric(){return EnchantmentEffects.level("vampiric",actor.getMainHandItem());}public void heal(int amount){if(amount!=0)actor.heal(amount);}
   public boolean playerTarget(){return target instanceof Player;}public void refreshDespawn(int ticks){OriginalDespawn.refresh(actor,ticks);}
  });
  return result!=null&&result.negativelyAffected();
 }
 private static void thorns(Mob attacker,LivingEntity wearer){
  int maximum=0;ItemStack first=ItemStack.EMPTY;
  for(var stack:wearer.getArmorSlots()){
   int level=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.THORNS,stack);
   if(level>0){maximum=Math.max(maximum,level);if(first.isEmpty())first=stack;}
  }
  int level=maximum;ItemStack armor=first;
  SourceMobStrike.thorns(new SourceMobStrike.ThornsHost(){
   public int level(){return level;}public boolean hasArmor(){return !armor.isEmpty();}
   public float nextFloat(){return attacker.getRandom().nextFloat();}public int nextInt(int bound){return attacker.getRandom().nextInt(bound);}
   public void retaliate(int damage){attacker.hurt(wearer.damageSources().thorns(wearer),damage);}
   public void wear(int amount){ClassicDefense.wearItem(wearer,wearer.damageSources().generic(),armor,amount);}
  });
 }
 private SourceMobStrikeNative(){}
}
