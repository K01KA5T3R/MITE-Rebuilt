package dev.koikaster.mite;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** aep/agn/ain over retained modern fortress structures. Worker threads enqueue
 * immutable bounds; only the server thread touches SavedData. Generated starts
 * remain known after unload/restart, as in the source generator's global map. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceFortressSpawns {
 static final String DATA="mite_reforged_source_fortresses";
 private static final class Rows {
 static final WeightedRandomList<MobSpawnSettings.SpawnerData> VALUE=WeightedRandomList.create(OriginalFortressSelection.ROWS.stream().map(row->new MobSpawnSettings.SpawnerData(Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(row.id()))),row.weight(),row.min(),row.max())).toList());
 }
 private record Change(long key,OriginalFortressSelection.Start start){}
 private static final Map<ServerLevel,ConcurrentLinkedQueue<Change>> PENDING=new IdentityHashMap<>();
 private static synchronized ConcurrentLinkedQueue<Change> queue(ServerLevel level){return PENDING.computeIfAbsent(level,w->new ConcurrentLinkedQueue<>());}
 private static OriginalFortressSelection.Bounds box(BoundingBox b){return new OriginalFortressSelection.Bounds(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ());}
 /** StructureManager.setStartForStructure after successful native insertion. */
 public static void generated(Object rawWorld,Object rawSection,Object rawStructure,Object rawStart){
  if(!(rawWorld instanceof ServerLevelAccessor accessor))return;var level=accessor.getLevel();if(level.dimension()!=Level.NETHER||rawStructure!=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.FORTRESS))return;
  var start=(StructureStart)rawStart;var value=start.isValid()?new OriginalFortressSelection.Start(true,box(start.getBoundingBox()),start.getPieces().stream().map(p->box(p.getBoundingBox())).toList()):new OriginalFortressSelection.Start(false,new OriginalFortressSelection.Bounds(0,0,0,0,0,0),List.of());
  queue(level).add(new Change(((SectionPos)rawSection).chunk().toLong(),value));
 }
 static Data state(ServerLevel level){
  if(!level.getServer().isSameThread())throw new IllegalStateException("Source fortress SavedData is server-thread owned");
  var data=level.getDataStorage().computeIfAbsent(Data::load,Data::new,DATA);Change change;var pending=queue(level);while((change=pending.poll())!=null)data.remember(change.key,change.start);return data;
 }
 static boolean usesFortress(ServerLevel level,BlockPos pos){
  if(level.dimension()!=Level.NETHER)return false;var data=state(level);var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);boolean brick=chunk!=null&&chunk.getBlockState(pos.below()).is(Blocks.NETHER_BRICKS);
  return OriginalFortressSelection.fortressPool(true,data.starts.values(),pos.getX(),pos.getY(),pos.getZ(),brick);
 }
 static WeightedRandomList<MobSpawnSettings.SpawnerData> pool(ServerLevel level,BlockPos pos){return usesFortress(level,pos)?Rows.VALUE:level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.MONSTER);}
 @SubscribeEvent public static void loaded(ChunkEvent.Load event){if(event.getLevel() instanceof ServerLevel level&&level.dimension()==Level.NETHER){var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.FORTRESS);var start=event.getChunk().getStartForStructure(structure);if(start!=null)generated(level,SectionPos.of(event.getChunk().getPos(),level.getMinSection()),structure,start);}}
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event){if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level&&level.dimension()==Level.NETHER)state(level);}
 @SubscribeEvent public static void save(LevelEvent.Save event){if(event.getLevel() instanceof ServerLevel level&&level.dimension()==Level.NETHER){var data=state(level);if(data.isDirty())level.getDataStorage().save();}} // Forge posts Save after its first SavedData flush.
 @SubscribeEvent public static synchronized void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)PENDING.remove(level);}
 static final class Data extends SavedData {
  final HashMap<Long,OriginalFortressSelection.Start> starts=new HashMap<>();
  void remember(long key,OriginalFortressSelection.Start start){if(!start.equals(starts.put(key,start)))setDirty();}
  private static int[] ints(OriginalFortressSelection.Bounds b){return new int[]{b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()};}
  private static OriginalFortressSelection.Bounds bounds(int[] a){if(a.length!=6)throw new IllegalArgumentException("Source fortress bounds require six coordinates");return new OriginalFortressSelection.Bounds(a[0],a[1],a[2],a[3],a[4],a[5]);}
  @Override public CompoundTag save(CompoundTag tag){var rows=new ListTag();for(var entry:starts.entrySet()){var row=new CompoundTag();row.putLong("chunk",entry.getKey());var value=entry.getValue();row.putBoolean("valid",value.valid());row.putIntArray("bounds",ints(value.bounds()));var pieces=new ListTag();for(var piece:value.pieces())pieces.add(new IntArrayTag(ints(piece)));row.put("pieces",pieces);rows.add(row);}tag.put("starts",rows);return tag;}
  static Data load(CompoundTag tag){var data=new Data();for(var raw:tag.getList("starts",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;var pieces=new ArrayList<OriginalFortressSelection.Bounds>();for(var piece:row.getList("pieces",Tag.TAG_INT_ARRAY))pieces.add(bounds(((IntArrayTag)piece).getAsIntArray()));data.starts.put(row.getLong("chunk"),new OriginalFortressSelection.Start(row.getBoolean("valid"),bounds(row.getIntArray("bounds")),List.copyOf(pieces)));}return data;}
 }
 private SourceFortressSpawns(){}
}
