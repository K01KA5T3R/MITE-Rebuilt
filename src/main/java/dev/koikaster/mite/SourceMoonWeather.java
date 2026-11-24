package dev.koikaster.mite;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Replaces only native weather scheduling. Native strength interpolation,
 * dimension-scoped packets, precipitation and thunder consumers remain live. */
public final class SourceMoonWeather { private static final Map<ServerLevel,Long> VISUAL_CREATION=new WeakHashMap<>();
 private static final class State extends SavedData {
  long creation;int day=Integer.MIN_VALUE;List<SourceMoonWeatherRules.Event> events=List.of();
  State(){creation=System.currentTimeMillis();setDirty();}
  static State load(CompoundTag tag){var state=new State();state.creation=tag.getLong("CreationTime");return state;}
  @Override public CompoundTag save(CompoundTag tag){tag.putLong("CreationTime",creation);return tag;}
 }
 public static boolean scoped(Object raw){return ((Level)raw).dimension()==Level.OVERWORLD;}
 public static void beforeCycle(Object raw){
  if(!(raw instanceof ServerLevel level)||!scoped(level))return;
  var state=level.getDataStorage().computeIfAbsent(State::load,State::new,"mite_moon_weather");remember(level,state.creation);int day=SourceMoonCycle.dayOfWorld(level.getDayTime());
  if(state.day!=day){state.day=day;state.events=SourceMoonWeatherRules.today(state.creation,0,day);}
  var event=SourceMoonWeatherRules.at(state.events,level.getDayTime());level.setWeatherParameters(0,0,event!=null,event!=null&&event.storming(level.getDayTime()));
 }
 public static boolean vanillaCycle(boolean original,Object raw){return !scoped(raw)&&original;}
 public static boolean skipNight(boolean original,Object raw){var level=(Level)raw;return original&&!(scoped(level)&&SourceMoonCycle.blood(level.getDayTime(),false));}
 public static boolean bloodPrecipitation(Object raw){var level=(Level)raw;return scoped(level)&&SourceMoonCycle.blood(level.getDayTime(),false);}
 public static boolean rainAt(Object raw,Object pos){return OriginalWeather.rain((Level)raw,(BlockPos)pos);}
 private SourceMoonWeather(){}
 public static OptionalLong visualCreationTime(ServerLevel level){Long value=VISUAL_CREATION.get(level);return value==null?OptionalLong.empty():OptionalLong.of(value);}
 private static void remember(ServerLevel level,long creation){VISUAL_CREATION.put(level,creation);}
}
