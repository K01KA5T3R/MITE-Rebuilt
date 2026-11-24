package dev.koikaster.mite;
import java.util.*;import net.minecraft.nbt.*;import net.minecraft.server.level.*;import net.minecraft.world.level.saveddata.SavedData;
/** WorldInfoShared's cross-dimension, first-completer history. It never grants another player's stats. */
public final class SourceAchievementWorld561 extends SavedData {
 private final Map<Integer,SourceAchievementRules561.First> records=new LinkedHashMap<>();
 public static SourceAchievementWorld561 get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(SourceAchievementWorld561::load,SourceAchievementWorld561::new,"mite_r195_achievements");}
 private static SourceAchievementRules561.First record(String name,int day){return name==null||name.length()>64?new SourceAchievementRules561.First("",0):new SourceAchievementRules561.First(name,Math.max(0,day));}
 public static SourceAchievementWorld561 load(CompoundTag tag){var result=new SourceAchievementWorld561();var list=tag.getList("Achievements",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var n=list.getCompound(i);if(!n.contains("Id",Tag.TAG_INT))continue;int id=n.getInt("Id");if(SourceAchievementPolicy567.active(id))result.records.putIfAbsent(id,record(n.getString("Username"),n.getInt("Day")));}return result;}
 public boolean first(int id,String username,int day){if(!SourceAchievementPolicy567.active(id))return false;var value=record(username,day);if(!SourceAchievementRules561.first(records,id,value.username(),value.day()))return false;setDirty();return true;}
 public Map<Integer,SourceAchievementRules561.First> records(){return Collections.unmodifiableMap(records);}
 @Override public CompoundTag save(CompoundTag tag){var list=new ListTag();records.forEach((id,value)->{var n=new CompoundTag();n.putInt("Id",id);n.putString("Username",value.username());n.putInt("Day",value.day());list.add(n);});tag.put("Achievements",list);return tag;}
 private SourceAchievementWorld561(){}
}
