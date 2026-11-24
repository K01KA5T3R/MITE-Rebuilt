package dev.koikaster.mite;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** adr + aee persistence. Volatile per-chunk storage survives native proto promotion;
 * Forge's async read/write events operate only on their supplied chunk/tag. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceMyceliumFlags {
 public static final String KEY="mite_reforged:had_natural_mycelium";
 private static ChunkAccess unwrap(ChunkAccess chunk){return chunk instanceof ImposterProtoChunk wrapper?wrapper.getWrapped():chunk;}
 public static boolean get(ChunkAccess chunk){return ((SourceMyceliumChunkAccess)unwrap(chunk)).mite$naturalMycelium();}
 public static void mark(ChunkAccess chunk){var actual=unwrap(chunk);((SourceMyceliumChunkAccess)actual).mite$naturalMycelium(true);actual.setUnsaved(true);}
 public static void promote(Object target,Object source){if(get((ChunkAccess)source))((SourceMyceliumChunkAccess)target).mite$naturalMycelium(true);}
 public static boolean nearby(WorldGenLevel world,int chunkX,int chunkZ){
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   var chunk=world.getChunk(chunkX+dx,chunkZ+dz,ChunkStatus.EMPTY,false);if(chunk!=null&&get(chunk))return true;
  }
  return false;
 }
 @SubscribeEvent public static void load(ChunkDataEvent.Load event){if(event.getData().getBoolean(KEY))((SourceMyceliumChunkAccess)unwrap(event.getChunk())).mite$naturalMycelium(true);}
 @SubscribeEvent public static void save(ChunkDataEvent.Save event){if(get(event.getChunk()))event.getData().putBoolean(KEY,true);}
 private SourceMyceliumFlags(){}
}
