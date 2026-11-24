package dev.koikaster.mite;

import java.util.Objects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;

/** qy family filter extending the frozen nearest-target implementation through its protected predicate. */
public final class OriginalWolfHostilePreyGoal extends OriginalNearestTargetGoal {
 public enum Family {ZOMBIE,GHOUL}
 private final Family family;
 public OriginalWolfHostilePreyGoal(Wolf wolf,Family family){super(wolf,LivingEntity.class,3200,true);this.family=Objects.requireNonNull(family);}
 /** Exact declared source-role mapping; modern Husk/Drowned/Piglin are deliberately absent. */
 public static boolean includes(LivingEntity target,Family family){
  var type=target.getType();
  return switch(family){
   case ZOMBIE->type==EntityType.ZOMBIE||type==EntityType.ZOMBIE_VILLAGER||type==EntityType.ZOMBIFIED_PIGLIN||type==Mobs.UNDEAD.get("revenant").get();
   case GHOUL->type==Mobs.UNDEAD.get("ghoul").get();
  };
 }
 @Override protected boolean suitable(LivingEntity target,boolean includeInvulnerable){return target!=null&&includes(target,family)&&super.suitable(target,includeInvulnerable);}
}
