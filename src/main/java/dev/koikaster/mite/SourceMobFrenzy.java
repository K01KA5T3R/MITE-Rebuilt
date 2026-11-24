package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
/** tm.isFrenzied and its additive melee term.
 * Cubic/bat/wolf og branches do not inherit the source tm branch. */
public final class SourceMobFrenzy {
 public static boolean blood(Mob mob){return SourceSpecialSpawns.bloodUp(mob.level());}
 public static boolean active(Monster mob){if(mob instanceof SourceEnderman)return false;if(mob instanceof SourceSkeleton skeleton)return skeleton.sourceFrenzied();if(mob instanceof SourceLongdead longdead)return longdead.sourceFrenzied();if(mob instanceof SourceWitherSkeleton wither)return wither.sourceFrenzied();return blood(mob);}
 public static float meleeDamage(LivingEntity actor,float damage){
  if(actor.level().isClientSide||!(actor instanceof Monster mob)||!OriginalDespawn.handled(mob)||!active(mob))return damage;
  var attribute=mob.getAttribute(Attributes.ATTACK_DAMAGE);return attribute==null?damage:damage+(float)attribute.getBaseValue()*.5f;
 }
 private SourceMobFrenzy(){}
}
