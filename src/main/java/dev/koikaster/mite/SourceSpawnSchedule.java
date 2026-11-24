package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** R195 aci's global natural-spawn gates, separate from species placement rules.
 * The native 400-tick cadence is applied by the caller, not by a second local
 * clock check here. World generation has its own source entry and is exempt.
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceSpawnSchedule {
 // nt registry + acq/adb/adj animal pools. rs initializes normal horses and
 // donkeys (rs:1213-1232); modern village cats and other later animals are not
 // additions to those pools. Hellhound belongs to the hostile category.
 private static final Set<String> SOURCE_ANIMALS=Set.of(
  "pig","sheep","cow","chicken","wolf","mooshroom","ocelot","horse","donkey");

 static boolean sourceAnimal(EntityType<?> type){
  var id=ForgeRegistries.ENTITY_TYPES.getKey(type);
  return id!=null&&(id.getNamespace().equals("minecraft")&&SOURCE_ANIMALS.contains(id.getPath())
   ||id.getNamespace().equals(MiteReforged.ID)&&id.getPath().equals("dire_wolf"));
 }

 /** SpawnPlacements calls Forge's runtime event from the actual native entry.
  * Denial adds the missing global restriction without allowing a failed grass,
  * light, biome, fluid or other species check.
  */
 @SubscribeEvent(priority=EventPriority.LOWEST)
 public static void naturalAnimals(MobSpawnEvent.SpawnPlacementCheck event){
  if(event.getSpawnType()==MobSpawnType.NATURAL&&sourceAnimal(event.getEntityType())
   &&!SourceSpecialSpawns.blueUp(event.getLevel().getLevel()))event.setResult(Event.Result.DENY);
 }

 /** aci:197 checks the created hostile entity's eye block, before its local
  * spawn check. It is not js.getSuitableCreature's weighted-selection rule.
  */
 @SubscribeEvent(priority=EventPriority.LOWEST)
 public static void naturalHostilePosition(MobSpawnEvent.PositionCheck event){
  if(event.getSpawnType()!=MobSpawnType.NATURAL
   ||event.getEntity().getType().getCategory()!=MobCategory.MONSTER
   ||!SourceSpecialSpawns.blueUp(event.getLevel().getLevel()))return;
  var mob=event.getEntity();
  var eye=BlockPos.containing(mob.getX(),mob.getEyeY(),mob.getZ());
  if(event.getLevel().getBrightness(LightLayer.SKY,eye)>0)event.setResult(Event.Result.DENY);
 }

 private SourceSpawnSchedule(){}
}
