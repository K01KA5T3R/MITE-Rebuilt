package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**  js.getSuitableCreature lines492,529..532,550..553.
 * Depth is checked only in the Overworld; the blood moon exception uses the
 * existing source clock. Native body/light placement remains its separate owner.
 */
public final class SourceAmbientSpawnSelection {
 public static boolean accepts(String species,ServerLevelAccessor level,BlockPos pos){
  if(level.getLevel().dimension()!=Level.OVERWORLD)return true;
  return switch(species){
   case "vampire_bat"->pos.getY()<=48||SourceSpecialSpawns.bloodUp(level.getLevel());
   case "nightwing"->pos.getY()<=32||SourceSpecialSpawns.bloodUp(level.getLevel());
   default->true;
  };
 }
 private SourceAmbientSpawnSelection(){}
}
