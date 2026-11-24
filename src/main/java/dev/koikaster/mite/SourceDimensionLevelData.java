package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.*;

/** DerivedLevelData's time aliases are replaced, while every native setter
 * retains its original owner. In particular the Overworld never acquires the
 * no-op setters that native secondary dimensions intentionally use. */
final class SourceDimensionLevelData extends DerivedLevelData {
 final MinecraftServer server;
 final ServerLevelData original;
 final int dimension;
 private volatile SourceDimensionClock.State clocks;
 SourceDimensionLevelData(MinecraftServer server,ServerLevelData original,int dimension){super(server.getWorldData(),original);this.server=server;this.original=original;this.dimension=dimension;this.clocks=SourceDimensionClock.state(server);}
 SourceDimensionClock.State clocks(){if(clocks==null)clocks=SourceDimensionClock.state(server);return clocks;}
 @Override public long getGameTime(){var data=clocks();return data==null?original.getGameTime():data.game[SourceDimensionClockRules.index(dimension)];}
 @Override public long getDayTime(){var data=clocks();return data==null?original.getDayTime():data.day[SourceDimensionClockRules.index(dimension)];}
 @Override public void setGameTime(long value){value=Math.max(0,value);var data=clocks();if(data!=null){data.game[SourceDimensionClockRules.index(dimension)]=value;data.setDirty();}if(dimension==0||data==null)original.setGameTime(value);}
 @Override public void setDayTime(long value){value=Math.max(0,value);var data=clocks();if(data!=null){data.day[SourceDimensionClockRules.index(dimension)]=value;data.setDirty();}if(dimension==0||data==null)original.setDayTime(value);}
 @Override public void setXSpawn(int value){original.setXSpawn(value);}
 @Override public void setYSpawn(int value){original.setYSpawn(value);}
 @Override public void setZSpawn(int value){original.setZSpawn(value);}
 @Override public void setSpawnAngle(float value){original.setSpawnAngle(value);}
 @Override public void setSpawn(BlockPos pos,float angle){original.setSpawn(pos,angle);}
 @Override public void setClearWeatherTime(int value){original.setClearWeatherTime(value);}
 @Override public void setRaining(boolean value){original.setRaining(value);}
 @Override public void setRainTime(int value){original.setRainTime(value);}
 @Override public void setThundering(boolean value){original.setThundering(value);}
 @Override public void setThunderTime(int value){original.setThunderTime(value);}
 @Override public void setGameType(GameType value){original.setGameType(value);}
 @Override public void setInitialized(boolean value){original.setInitialized(value);}
 @Override public void setWorldBorder(WorldBorder.Settings value){original.setWorldBorder(value);}
 @Override public void setWanderingTraderSpawnDelay(int value){original.setWanderingTraderSpawnDelay(value);}
 @Override public void setWanderingTraderSpawnChance(int value){original.setWanderingTraderSpawnChance(value);}
 @Override public void setWanderingTraderId(UUID value){original.setWanderingTraderId(value);}
}
