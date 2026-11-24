package dev.koikaster.mite;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Source aee's integer persistence. Work only on the supplied chunk and tag;
 * no server-world lookup from asynchronous serialization or generation. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceAnimalQuotas {
 public static final String KEY="mite_reforged:animals_spawned";
 private static ChunkAccess actual(ChunkAccess chunk){return chunk instanceof ImposterProtoChunk p?p.getWrapped():chunk;}
 public static int get(ChunkAccess chunk){return ((SourceAnimalQuotaChunk)actual(chunk)).mite$initialAnimals();}
 public static void set(ChunkAccess chunk,int value){var target=actual(chunk);((SourceAnimalQuotaChunk)target).mite$initialAnimals(value);target.setUnsaved(true);}
 public static void promote(Object target,Object source){((SourceAnimalQuotaChunk)target).mite$initialAnimals(get((ChunkAccess)source));}
 @SubscribeEvent public static void load(ChunkDataEvent.Load event){((SourceAnimalQuotaChunk)actual(event.getChunk())).mite$initialAnimals(event.getData().getInt(KEY));}
 @SubscribeEvent public static void save(ChunkDataEvent.Save event){event.getData().putInt(KEY,get(event.getChunk()));}
 private SourceAnimalQuotas(){}
}
