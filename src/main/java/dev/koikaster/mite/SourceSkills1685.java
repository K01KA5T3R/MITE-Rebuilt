package dev.koikaster.mite;
import com.mojang.serialization.Dynamic;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.Level;
/** Original saved world switch and player bits. No commands or configuration entry. */
public final class SourceSkills1685 {
 public static boolean enabled(Level level){Object data=level.isClientSide?level.getLevelData():level.getServer().getWorldData();return data instanceof SourceSkillAccess1685.WorldData skills&&skills.mite$skillsEnabled1685();}
 public static int bits(Player player){return ((SourceSkillAccess1685.PlayerData)player).mite$skillBits1685();}
 public static int day(Player player){return ((SourceSkillAccess1685.PlayerData)player).mite$skillDay1685();}
 public static boolean has(Player player,int skill){return SourceSkillRules1685.all(bits(player),skill);}
 public static boolean permits(Player player,int[] sets){return !enabled(player.level())||SourceSkillRules1685.alternatives(bits(player),sets);}
 public static void loadWorld(Object raw,Object dynamic){((SourceSkillAccess1685.WorldData)raw).mite$skillsEnabled1685(((Dynamic<?>)dynamic).get("are_skills_enabled").asBoolean(false));WorldContentMode.load(raw,(Dynamic<?>)dynamic);}
 public static void saveWorld(Object raw,Object tag){if(((SourceSkillAccess1685.WorldData)raw).mite$skillsEnabled1685())((CompoundTag)tag).putBoolean("are_skills_enabled",true);WorldContentMode.save(raw,(CompoundTag)tag);}
 public static void loadPlayer(Object raw,Object input){var player=(SourceSkillAccess1685.PlayerData)raw;var tag=(CompoundTag)input;player.mite$skillBits1685(tag.getInt("skills"));player.mite$skillDay1685(tag.getInt("last_skill_learned_on_day"));}
 public static void savePlayer(Object raw,Object output){var player=(SourceSkillAccess1685.PlayerData)raw;var tag=(CompoundTag)output;if(player.mite$skillBits1685()!=0)tag.putInt("skills",player.mite$skillBits1685());if(player.mite$skillDay1685()!=0)tag.putInt("last_skill_learned_on_day",player.mite$skillDay1685());}
 public static void copy(Player original,Player replacement){var out=(SourceSkillAccess1685.PlayerData)replacement;out.mite$skillBits1685(bits(original));out.mite$skillDay1685(day(original));}
 public static void snapshot(Player player,CompoundTag tag){tag.putBoolean("are_skills_enabled",enabled(player.level()));tag.putInt("skills",bits(player));tag.putInt("last_skill_learned_on_day",day(player));WorldContentMode.snapshot(player.level(),tag);}
 public static void receive(Player player,CompoundTag tag){var out=(SourceSkillAccess1685.PlayerData)player;out.mite$skillBits1685(tag.getInt("skills"));out.mite$skillDay1685(tag.getInt("last_skill_learned_on_day"));if(player.level().getLevelData() instanceof SourceSkillAccess1685.WorldData world)world.mite$skillsEnabled1685(tag.getBoolean("are_skills_enabled"));WorldContentMode.receive(player.level(),tag);}
 public static float meleeMultiplier(Player player){int level=PlayerState.level(player);return level<0||!enabled(player.level())||has(player,SourceSkillRules1685.FIGHTING)?Rules.meleeMultiplier(level):1f;}
 private SourceSkills1685(){}
}
