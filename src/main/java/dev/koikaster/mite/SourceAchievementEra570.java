package dev.koikaster.mite;
import net.minecraft.nbt.CompoundTag;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.level.saveddata.SavedData;
/** A pre-feature world has no complete historical first-completer data. Do not invent it. */
public final class SourceAchievementEra570 extends SavedData {
 public boolean completeHistory;
 private SourceAchievementEra570(){}
 private static SourceAchievementEra570 fresh(ServerLevel level){var value=new SourceAchievementEra570();value.completeHistory=level.getGameTime()==0;value.setDirty();return value;}
 public static SourceAchievementEra570 get(ServerLevel level){var over=level.getServer().overworld();return over.getDataStorage().computeIfAbsent(SourceAchievementEra570::load,()->fresh(over),"mite_r195_achievement_era");}
 public static SourceAchievementEra570 load(CompoundTag tag){var value=new SourceAchievementEra570();value.completeHistory=tag.getBoolean("CompleteHistory");return value;}
 @Override public CompoundTag save(CompoundTag tag){tag.putBoolean("CompleteHistory",completeHistory);return tag;}
}
