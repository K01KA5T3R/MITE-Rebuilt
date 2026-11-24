package dev.koikaster.mite;

import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Calendar ownership stays with SourceDimensionLevelData. Observations are
 * made only after completed native setters/ticks, never from historical reads. */
public final class SourceFastForwardTimeline220 {
 static final class Journal extends SavedData {
  SourceCalendarTimeline220 timeline=new SourceCalendarTimeline220();
  static Journal load(CompoundTag tag){
   var journal=new Journal();if(tag.getInt("Version")!=1)return journal;
   var raw=tag.getLongArray("Segments");if(raw.length==0||raw.length%3!=0)return journal;
   var segments=new ArrayList<SourceCalendarTimeline220.Segment>();
   for(int i=0;i<raw.length;i+=3){if(raw[i+2]!=0&&raw[i+2]!=1)return journal;segments.add(new SourceCalendarTimeline220.Segment(raw[i],raw[i+1],raw[i+2]==1));}
   try{journal.timeline=SourceCalendarTimeline220.restore(segments,tag.getLong("Elapsed"),tag.getLong("Calendar"),tag.getBoolean("Running"));}
   catch(IllegalArgumentException|ArithmeticException ignored){journal.timeline=new SourceCalendarTimeline220();}return journal;
  }
  @Override public CompoundTag save(CompoundTag tag){
   if(timeline.empty())return tag;var segments=timeline.segments();var raw=new long[segments.size()*3];
   for(int i=0;i<segments.size();i++){var segment=segments.get(i);raw[i*3]=segment.elapsed();raw[i*3+1]=segment.calendar();raw[i*3+2]=segment.running()?1:0;}
   tag.putInt("Version",1);tag.putLongArray("Segments",raw);tag.putLong("Elapsed",timeline.lastElapsed());tag.putLong("Calendar",timeline.lastCalendar());tag.putBoolean("Running",timeline.lastRunning());return tag;
  }
 }
 static Journal journal(ServerLevel level){return level.getDataStorage().computeIfAbsent(Journal::load,Journal::new,"mite_chunk_calendar");}
 private static void observe(ServerLevel level){var data=level.getLevelData();if(!(data instanceof SourceDimensionLevelData))return;var journal=journal(level);if(journal.timeline.observe(data.getGameTime(),data.getDayTime(),level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)))journal.setDirty();}
 public static boolean afterClock(boolean original,Object raw){if(original)observe((ServerLevel)raw);return original;}
 public static void calendarChanged(Object raw){
  var data=(SourceDimensionLevelData)raw;
  var key=switch(data.dimension){case 0->Level.OVERWORLD;case -1->Level.NETHER;case 1->Level.END;case -2->WorldEvents.UNDERWORLD;default->null;};
  var level=key==null?null:data.server.getLevel(key);if(level!=null&&level.getLevelData()==data)observe(level);
 }
 static SourceCalendarTimeline220 current(ServerLevel level){observe(level);var value=journal(level).timeline;if(value.empty())value.observe(level.getLevelData().getGameTime(),level.getLevelData().getDayTime(),level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));return value;}
 private SourceFastForwardTimeline220(){}
}
