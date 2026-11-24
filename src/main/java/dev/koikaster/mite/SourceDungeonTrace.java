package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Bounded observations for explicitly enabled new development cases. No saved
 * state, player setting, RNG consumption or normal-game collection. */
final class SourceDungeonTrace {
 record Decoration(BlockPos origin,List<List<Integer>> attempts,List<List<Integer>> rooms){}
 private static final Map<ServerLevel,List<Decoration>> DATA=new WeakHashMap<>();
 static synchronized void record(ServerLevel level,BlockPos origin,List<List<Integer>> attempts,List<List<Integer>> rooms){
  if(!Boolean.getBoolean("mite.sourcePortValidation"))return;
  var list=DATA.computeIfAbsent(level,key->new ArrayList<>());if(list.size()<2048)list.add(new Decoration(origin.immutable(),List.copyOf(attempts),List.copyOf(rooms)));
 }
 static synchronized List<Decoration> read(ServerLevel level){return List.copyOf(DATA.getOrDefault(level,List.of()));}
 private SourceDungeonTrace(){}
}
