package dev.koikaster.mite;

import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;

/** Staged for the source-species refactor; not wired into0.24 and not runtime-verified.
 * tp.a's unchanged vanilla default is20HP; tp.b is FOLLOW_RANGE. No quality/day scaling.
 */
public final class OriginalHumanoidProfiles {
 public record Profile(double health,double follow,double speed,double attack,MobType creatureType,float naturalDefense){
  public AttributeSupplier attributes(){return builder().build();}
  AttributeSupplier.Builder builder(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.FOLLOW_RANGE,follow).add(Attributes.MOVEMENT_SPEED,speed).add(Attributes.ATTACK_DAMAGE,attack);}
 }
 public static Profile profile(String sourceName){return switch(sourceName){
  case "ghoul"->new Profile(20,40,(double).28f,5,MobType.UNDEFINED,0);
  case "wight"->new Profile(20,40,.25,5,MobType.UNDEAD,0);
  case "revenant"->new Profile(30,40,(double).26f,7,MobType.UNDEAD,0);
  case "earth_elemental"->new Profile(30,20,(double).2f,12,MobType.UNDEFINED,4);
  case "fire_elemental"->new Profile(20,40,.25,5,MobType.UNDEFINED,0);
  case "shadow"->new Profile(20,40,(double).23f,5,MobType.UNDEAD,0);
  case "clay_golem"->new Profile(30,20,(double).2f,6,MobType.UNDEFINED,0);
  default->throw new IllegalArgumentException("Source humanoid profile not audited: "+sourceName);
 };}
 private OriginalHumanoidProfiles(){}
}
