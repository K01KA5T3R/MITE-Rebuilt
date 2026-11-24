package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Actual world inputs for the shared source Driver. R195 abw.q samples the
 * candidate block and world weather; it does not sample actor.y+height*.66 or
 * apply the actor's dry-biome one-shot weather exemption. Native light storage
 * and voxel propagation adapt the original block light records. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceArachnidWorld {
 private static final String SPAWNER="MiteSourceArachnidSpawner";
 public static float pointBrightness(Level level,BlockPos pos){
  int dimension=level.dimension()==Level.NETHER?-1:level.dimension()==Level.END?1:level.dimension()==WorldEvents.UNDERWORLD?-2:0;
  float angle=LegacyLighting.celestialAngle(level.getDayTime(),1,dimension);
  float darkness=LegacyLighting.clamp(1-(LegacyLighting.cos(angle*(float)Math.PI*2)*2+.5f));
  float light=1-darkness;light=(float)((double)light*(1-(double)(level.getRainLevel(1)*5f)/16));light=(float)((double)light*(1-(double)(level.getThunderLevel(1)*5f)/16));
  int skySubtraction=(int)((1-light)*11f);
  return LegacyLighting.brightness(level.getRawBrightness(pos,skySubtraction),dimension==-1?.1f:0);
 }
 /** nn.isOutdoors uses the actor's block position and includes side openings. */
 public static boolean outdoors(Mob actor) {
  var level = actor.level();
  return level.dimension() == Level.OVERWORLD && SourceSpawnOutdoors.isOutdoors(level, actor.blockPosition());
 }
 /** This marker already has a real native cactus-damage caller in
  * OriginalCreatureVitals. Its native-boolean/source-seven-flag distinction
  * remains documented; it is not replaced by a permanent zero timestamp. */
 public static long lastCactusTick(Mob actor){return actor.getPersistentData().getLong("MiteLastCactusHarm");}
 public static boolean cameFromSpawner(Mob actor){return actor.getSpawnType()==MobSpawnType.SPAWNER||actor.getPersistentData().getBoolean(SPAWNER);}
 /** Covers native spawner position checks even when custom spawn NBT skips the
  * ordinary finalizeSpawn path which stores Forge's spawn type. */
 @SubscribeEvent public static void spawner(MobSpawnEvent.PositionCheck event){if(event.getSpawnType()==MobSpawnType.SPAWNER&&SourceArachnidOwners.species(event.getEntity())!=null)event.getEntity().getPersistentData().putBoolean(SPAWNER,true);}
 public static void save(Mob actor,CompoundTag tag){if(cameFromSpawner(actor))tag.putBoolean("came_from_spawner",true);}
 /** og363 tests key presence, matching source's true-only saved representation. */
 public static void load(Mob actor,CompoundTag tag){if(tag.contains("came_from_spawner"))actor.getPersistentData().putBoolean(SPAWNER,true);}
 private SourceArachnidWorld(){}
}
