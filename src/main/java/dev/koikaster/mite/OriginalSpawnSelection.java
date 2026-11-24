package dev.koikaster.mite;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.registries.ForgeRegistries;
/** js.getSuitableCreature's sixteen weighted retries at the native selection call.
 * Native Creeper candidates use the registered source owner and Infernal replacement. */
public final class OriginalSpawnSelection {
 @SuppressWarnings("unchecked") public static Optional<MobSpawnSettings.SpawnerData> choose(Object rawRows,Object rawRandom,Object rawLevel,Object rawPos){
  var rows=(WeightedRandomList<MobSpawnSettings.SpawnerData>)rawRows;var random=(RandomSource)rawRandom;var level=(ServerLevel)rawLevel;var pos=(BlockPos)rawPos;
  for(int attempt=0;attempt<16;attempt++){
   var entry=rows.getRandom(random);if(entry.isEmpty())return Optional.empty();
   if(entry.get().type==net.minecraft.world.entity.EntityType.WITCH)continue;
   if(entry.get().type==net.minecraft.world.entity.EntityType.SLIME&&!SourceSlimeSelectionRoof.accepts(level,pos))continue;
   if(entry.get().type==net.minecraft.world.entity.EntityType.SPIDER&&!SourceCreatureSelection.spider(level,pos,random))continue;
   // A rejected Ghast ends this selection, rather than taking another retry.
   if(entry.get().type==net.minecraft.world.entity.EntityType.GHAST&&!SourceCreatureSelection.ghast(level,pos,random))return Optional.empty();
   if(entry.get().type==net.minecraft.world.entity.EntityType.CREEPER){var chosen=SourceCreeperContent.choose(entry.get(),level,pos,random);if(chosen==null)continue;entry=Optional.of(chosen);}
   var id=ForgeRegistries.ENTITY_TYPES.getKey(entry.get().type);
   if(id!=null&&id.getNamespace().equals(MiteReforged.ID)&&!SourceCreatureSelection.custom(id.getPath(),level,pos,random))continue;
   return entry;
  }
  return Optional.empty();
 }
 private OriginalSpawnSelection(){}
}
