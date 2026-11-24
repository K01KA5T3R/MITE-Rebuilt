package dev.koikaster.mite;
import net.minecraft.network.chat.*;import net.minecraft.world.level.storage.LevelSummary;
/** Original acd/amg/awi flag propagation; no switch or player-facing configuration. */
public final class SourceSkillSettings1846 {
 public static void inherit(Object target,Object source){
  inheritSkills(target,source);
  WorldContentMode.inherit(target,source);
 }
 public static void inheritDraft(Object target,Object source){
  inheritSkills(target,source);
  // Recreate produces a new world. Carry the player's choices, without the old world's terrain lock.
  WorldContentMode.choose(target,WorldContentMode.settings(source));
 }
 private static void inheritSkills(Object target,Object source){
  ((SourceSkillAccess1685.WorldData)target).mite$skillsEnabled1685(((SourceSkillAccess1685.WorldData)source).mite$skillsEnabled1685());
 }
 public static void summary(Object raw,Object text){var settings=((LevelSummary)raw).getSettings();if((Object)settings instanceof SourceSkillAccess1685.WorldData skills&&skills.mite$skillsEnabled1685())((MutableComponent)text).append(", ").append(Component.translatable("selectWorld.skills"));}
 private SourceSkillSettings1846(){}
}
