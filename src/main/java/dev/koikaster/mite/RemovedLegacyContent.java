package dev.koikaster.mite;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.*;import net.minecraft.resources.ResourceKey;import net.minecraft.server.level.*;import net.minecraft.world.level.*;import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.Items;import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;import net.minecraftforge.registries.*;
/** Old-save input only; these obsolete prototypes have no registry entries. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class RemovedLegacyContent {
 private static final String LEGACY_ADDON="mite_afterlight";
 private static final Set<String> ITEMS=Set.of("ancient_sigil","ancient_core","ancient_guardian_spawn_egg","runic_gate");
 private static boolean unavailable(){var mods=net.minecraftforge.fml.ModList.get();return mods==null||!mods.isLoaded(LEGACY_ADDON);}
 private static boolean retired(String key){return key.startsWith(LEGACY_ADDON+":");}
 @SubscribeEvent public static void missing(MissingMappingsEvent event){
  for(var mapping:event.getMappings(ForgeRegistries.Keys.ITEMS,MiteReforged.ID))if(ITEMS.contains(mapping.getKey().getPath()))mapping.remap(Items.AIR);
  for(var mapping:event.getMappings(ForgeRegistries.Keys.BLOCKS,MiteReforged.ID))if(mapping.getKey().getPath().equals("runic_gate"))mapping.remap(Blocks.AIR);
  for(var mapping:event.getMappings(ForgeRegistries.Keys.ENTITY_TYPES,MiteReforged.ID))if(mapping.getKey().getPath().equals("ancient_guardian"))mapping.ignore();
  if(unavailable())ignoreAddonMappings(event);
 }
 @SuppressWarnings({"rawtypes","unchecked"})
 private static void ignoreAddonMappings(MissingMappingsEvent event){for(Object raw:event.getMappings((ResourceKey)event.getKey(),LEGACY_ADDON))((MissingMappingsEvent.Mapping<?>)raw).ignore();}
 /** LevelStorageSource calls this before DFU and the dimension/generator codecs. */
 public static <T> Dynamic<T> world(Dynamic<T> root){return world(root,unavailable());}
 public static <T> Dynamic<T> world(Dynamic<T> root,boolean missingAddon){
  if(!missingAddon)return root;var settings=root.get("WorldGenSettings").result();if(settings.isEmpty())return root;
  var dimensions=settings.get().get("dimensions").result();if(dimensions.isEmpty())return root;
  var entries=dimensions.get().getMapValues().result();if(entries.isEmpty())return root;
  var filtered=dimensions.get();boolean changed=false;for(var key:entries.get().keySet()){String name=key.asString("");if(retired(name)){filtered=filtered.remove(name);changed=true;}}
  return changed?root.set("WorldGenSettings",settings.get().set("dimensions",filtered)):root;
 }
 /** The same input object is used later by native login for dimension/vehicle data. */
 public static CompoundTag player(CompoundTag tag,Object raw){
  if(!(raw instanceof ServerPlayer player)||!unavailable())return tag;
  boolean displaced=retired(tag.getString("Dimension"));
  if(displaced){
   var safe=migrationLanding(player.getServer().overworld(),player);
   var position=new ListTag();position.add(DoubleTag.valueOf(safe.x));position.add(DoubleTag.valueOf(safe.y));position.add(DoubleTag.valueOf(safe.z));tag.put("Pos",position);
   var motion=new ListTag();for(int i=0;i<3;i++)motion.add(DoubleTag.valueOf(0));tag.put("Motion",motion);tag.putString("Dimension",Level.OVERWORLD.location().toString());tag.putFloat("FallDistance",0);tag.putInt("PortalCooldown",300);
   for(var key:Set.of("RootVehicle","enteredNetherPosition","SleepingX","SleepingY","SleepingZ"))tag.remove(key);tag.putShort("SleepTimer",(short)0);
  }
  if(retired(tag.getString("SpawnDimension")))for(var key:Set.of("SpawnDimension","SpawnX","SpawnY","SpawnZ","SpawnForced","SpawnAngle"))tag.remove(key);
  if(retired(tag.getCompound("LastDeathLocation").getString("dimension")))tag.remove("LastDeathLocation");
  return tag;
 }

 private static final int LOADED_SEARCH_RADIUS = 12;
 private static final int SUPPORT_SEARCH_RADIUS = 2;

 private static Vec3 migrationLanding(ServerLevel level, ServerPlayer player) {
  float width = player.getDimensions(Pose.STANDING).width;
  var border = level.getWorldBorder();
  var anchor = BiomePortalBorderLanding.fit(border, width, Vec3.atBottomCenterOf(level.getSharedSpawnPos()));
  if (anchor == null) return player.position();
  var center = new ChunkPos(BlockPos.containing(anchor));

  Vec3 nearby = originalSpawnSearch(level, player, center);
  if (nearby != null) return nearby;

  // A tight border can admit the fitted footprint without admitting any block center.
  var body = BiomePortalBorderLanding.standing(player, anchor);
  if (BiomePortalBorderLanding.loaded(level, body)) {
   var at = new Vec3(anchor.x, surface(level, body, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), anchor.z);
   if (BiomePortalBorderLanding.safe(level, player, at)) return at;
  }

  var loaded = loadedSurfaceSearch(level, player, center);
  if (loaded != null) return loaded;

  var support = airSupportSearch(level, player, anchor, width);
  // In an exceptional world with no physically possible stance, keep native login data readable.
  return support != null ? support : player.position();
 }

 private static Vec3 originalSpawnSearch(ServerLevel level, ServerPlayer player, ChunkPos center) {
  for (int ring = 0; ring <= 1; ring++) {
   for (int x = -ring; x <= ring; x++) {
    for (int z = -ring; z <= ring; z++) {
     if (Math.max(Math.abs(x), Math.abs(z)) != ring) continue;
     var chunk = new ChunkPos(center.x + x, center.z + z);
     if (!level.getWorldBorder().isWithinBounds(chunk)) continue;
     var candidate = PlayerRespawnLogic.getSpawnPosInChunk(level, chunk);
     if (candidate == null) continue;
     var at = Vec3.atBottomCenterOf(candidate);
     if (BiomePortalBorderLanding.safe(level, player, at)) return at;
    }
   }
  }
  return null;
 }

 private static Vec3 loadedSurfaceSearch(ServerLevel level, ServerPlayer player, ChunkPos center) {
  var chunks = level.getChunkSource();
  for (int ring = 0; ring <= LOADED_SEARCH_RADIUS; ring++) {
   for (int x = -ring; x <= ring; x++) {
    for (int z = -ring; z <= ring; z++) {
     if (Math.max(Math.abs(x), Math.abs(z)) != ring) continue;
     var pos = new ChunkPos(center.x + x, center.z + z);
     if (!level.getWorldBorder().isWithinBounds(pos)) continue;
     var chunk = chunks.getChunkNow(pos.x, pos.z);
     if (chunk == null) continue;
     for (int bx = pos.getMinBlockX(); bx <= pos.getMaxBlockX(); bx++) {
      for (int bz = pos.getMinBlockZ(); bz <= pos.getMaxBlockZ(); bz++) {
       int floor = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
       if (floor < level.getMinBuildHeight()) continue;
       var state = chunk.getBlockState(new BlockPos(bx, floor, bz));
       if (state.isAir() || !state.getFluidState().isEmpty()) continue;
       var at = new Vec3(bx + .5, floor + 1, bz + .5);
       if (BiomePortalBorderLanding.safe(level, player, at)) return at;
      }
     }
    }
   }
  }
  return null;
 }

 private static Vec3 airSupportSearch(ServerLevel level, ServerPlayer player, Vec3 anchor, float width) {
  var checked = new HashSet<Vec3>();
  for (int ring = 0; ring <= SUPPORT_SEARCH_RADIUS; ring++) {
   for (int x = -ring; x <= ring; x++) {
    for (int z = -ring; z <= ring; z++) {
     if (Math.max(Math.abs(x), Math.abs(z)) != ring) continue;
     var column = BiomePortalBorderLanding.fit(level.getWorldBorder(), width, anchor.add(x, 0, z));
     if (column == null || !checked.add(column)) continue;
     var at = prepareAirSupport(level, player, column);
     if (at != null) return at;
    }
   }
  }
  return null;
 }

 private static Vec3 prepareAirSupport(ServerLevel level, ServerPlayer player, Vec3 column) {
  var footprint = BiomePortalBorderLanding.standing(player, column);
  int minX = Mth.floor(footprint.minX), maxX = Mth.ceil(footprint.maxX) - 1;
  int minZ = Mth.floor(footprint.minZ), maxZ = Mth.ceil(footprint.maxZ) - 1;
  // The native safety check includes the one-block margin around the complete standing footprint.
  for (int cx = (minX - 1) >> 4; cx <= (maxX + 1) >> 4; cx++) {
   for (int cz = (minZ - 1) >> 4; cz <= (maxZ + 1) >> 4; cz++) {
    level.getChunk(cx, cz);
   }
  }
  int floor = Math.max(level.getMinBuildHeight() + 1, surface(level, footprint, Heightmap.Types.WORLD_SURFACE));
  var at = new Vec3(column.x, floor + 1, column.z);
  var standing = BiomePortalBorderLanding.standing(player, at);
  if (standing.maxY >= level.getMaxBuildHeight()) return null;

  // Only add a footprint in empty air. The shared transaction must never need to clear an old block.
  for (var pos : BlockPos.betweenClosed(minX, floor, minZ, maxX, Mth.ceil(standing.maxY) - 1, maxZ)) {
   if (!level.getBlockState(pos).isAir()) return null;
  }
  return BiomePortalBorderLanding.prepare(level, player, at) ? at : null;
 }

 private static int surface(ServerLevel level, AABB footprint, Heightmap.Types heightmap) {
  int top = level.getMinBuildHeight();
  for (var cell : BlockPos.betweenClosed(Mth.floor(footprint.minX), 0, Mth.floor(footprint.minZ),
          Mth.ceil(footprint.maxX) - 1, 0, Mth.ceil(footprint.maxZ) - 1)) {
   top = Math.max(top, level.getHeight(heightmap, cell.getX(), cell.getZ()));
  }
  return top;
 }
 private RemovedLegacyContent(){}
}
