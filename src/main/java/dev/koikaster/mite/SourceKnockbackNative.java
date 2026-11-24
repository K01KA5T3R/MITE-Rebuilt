package dev.koikaster.mite;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** Retained of parent knockback, before the health iframe branch. Native
 * damage/Forge admission and species-specific pre-parent rejection stay owned
 * by their callers. The later native hurt knockback branch is disabled. */
public final class SourceKnockbackNative {
 public static void tick(Object raw){var body=(SourceKnockbackBody)raw;int ticks=body.mite$knockbackTicks();if(ticks>0)body.mite$knockbackTicks(ticks-1);}
 static boolean protectsHand(LivingEntity actor){
  var held=actor.getMainHandItem();
  return ClassicToolWear.profile(held)!=null||held.is(Items.STICK)||held.is(Items.BONE);
 }
 public static boolean weakStrike(LivingEntity actor){
  return actor instanceof Player player&&!protectsHand(actor)
   &&(actor.getHealth()<2||!SurvivalFood.hasFoodEnergy(player)||actor.getAttributeValue(Attributes.ATTACK_DAMAGE)<1);
 }
 /** True means source helper ends before health/armor/iframes. */
 public static boolean beforeHurt(Object raw,Object rawDamage,float amount){
  var actor=(LivingEntity)raw;var source=(DamageSource)rawDamage;
  if(amount<=0)return true;
  var responsible=source.getEntity();boolean weak=responsible instanceof LivingEntity living&&source.getDirectEntity()==responsible&&!source.is(DamageTypeTags.IS_PROJECTILE)&&weakStrike(living);
  if(responsible!=null&&!BossCombatTuning.knockbackImmune(actor)&&SourceKnockbackRules.knockBack(new Host(actor),true,responsible.getX(),responsible.getZ(),weak?.6f:1)){
   SourceParentHurtFrame.knockedBack(actor);actor.hurtMarked=true;
  }
  if(source instanceof SourceKnockbackOnly838){SourceKnockbackOnly838.finish(actor,source);return true;}
  return weak;
 }
 private static final class Host implements SourceKnockbackRules.Host {
  final LivingEntity actor;final SourceKnockbackBody body;Host(LivingEntity actor){this.actor=actor;body=(SourceKnockbackBody)actor;}
  public boolean canBeKnockedBack(){return true;}
  public int resistanceTicks(){return body.mite$knockbackTicks();}public void resistanceTicks(int value){body.mite$knockbackTicks(value);}
  public double x(){return actor.getX();}public double z(){return actor.getZ();}public float yaw(){return actor.getYRot();}public void attackedYaw(float value){body.mite$attackedYaw(value);}
  public double globalRandom(){return Math.random();}public double nextDouble(){return actor.getRandom().nextDouble();}public double resistance(){return actor.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);}
  public boolean player(){return actor instanceof Player;}public boolean earthElemental(){return actor instanceof Mobs.Undead entity&&entity.earthBody();}
  public void impulse(){actor.hasImpulse=true;}public double motionX(){return actor.getDeltaMovement().x;}public double motionY(){return actor.getDeltaMovement().y;}public double motionZ(){return actor.getDeltaMovement().z;}
  public void motion(double x,double y,double z){actor.setDeltaMovement(x,y,z);}public void direct(float amount,double dx,double dz){SourceKnockbackRules.direct(this,amount,dx,dz);}
 }
 private SourceKnockbackNative(){}
}
