package dev.koikaster.mite;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
/** Fixed V069/V076 production rules. Admission is separate from native
 * material consumption, bee release, pollination, loot and storage. */
public final class ModernProduction321 {
 private static final Map<Level,HoneyRolls> HONEY=new WeakHashMap<>();
 private static final class HoneyRolls {
  long tick;final Map<BlockPos,Boolean> hives=new HashMap<>();
  HoneyRolls(long tick){this.tick=tick;}
 }
 public static boolean acceptCompost(LevelAccessor level) {
  return !WorldContentMode.rebuilt(level) || level.getRandom().nextBoolean();
 }
 private static boolean honeyDecision(Level level,BlockPos pos){
  synchronized(HONEY){
   long tick=level.getGameTime();var rolls=HONEY.computeIfAbsent(level,k->new HoneyRolls(tick));
   if(rolls.tick!=tick){rolls.hives.clear();rolls.tick=tick;}
   var previous=rolls.hives.get(pos);
   if(previous==null){boolean accepted=level.getRandom().nextBoolean();rolls.hives.put(pos.immutable(),accepted);return accepted;}
   return previous;
  }
 }
 public static boolean setHoney(Level level, BlockPos pos, BlockState state) {
  return (!WorldContentMode.rebuilt(level) || honeyDecision(level, pos)) && level.setBlockAndUpdate(pos, state);
 }
 private ModernProduction321(){}
}
