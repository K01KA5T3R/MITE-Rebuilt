package dev.koikaster.mite;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;

/** Actual nn default and the merged R195 method overrides, including the
 * ooze/pudding overrides of EntityCubic's true result. */
public final class SourceSnowballTraits {
 public static boolean proper(DamageSource source){return source.getDirectEntity() instanceof SourceSnowball;}
 public static boolean fireTarget(Entity actor){return actor instanceof Blaze||actor instanceof Mobs.Undead entity&&Mobs.name(entity).equals("fire_elemental");}
 public static boolean playerDamage(Entity actor){
  if(fireTarget(actor)||actor instanceof SourceNetherspawn||actor instanceof Chicken||actor instanceof ItemFrame||actor instanceof Painting)return true;
  if(actor instanceof SourceGelatinousCube cube)return cube.profile()!=OriginalGelatinousProfile.OOZE&&cube.profile()!=OriginalGelatinousProfile.PUDDING;
  if(actor instanceof Slime)return true;
  if(actor instanceof Mob mob){var species=SourceArachnidOwners.species(mob);if(species!=null&&SourceArachnidProfiles.woodFamily(species))return true;}
  return false;
 }
 /** Extra health gate after the already-qualified source knockback helper.
  * Every other damage source returns the original decision unchanged. */
 public static boolean parentGate(boolean alreadyStop,Object raw,Object rawDamage){
  var source=(DamageSource)rawDamage;if(alreadyStop||!proper(source)||!(source.getEntity() instanceof Player)||playerDamage((Entity)raw))return alreadyStop;
  var actor=(LivingEntity)raw;if(SourceParentHurtFrame.wasKnockedBack(actor)){
   if(actor.invulnerableTime<=10){((SourceSnowballParentAccess)actor).mite$snowballHurtSound(source);SourceSnowballEffects.noTint(actor);}
   actor.walkAnimation.setSpeed(1.5f);((SourceKnockbackBody)actor).mite$attackedYaw(0);
  }
  return true;
 }
 private SourceSnowballTraits(){}
}
