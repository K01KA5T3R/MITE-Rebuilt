package dev.koikaster.mite;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** R195 tv is absent from acq's ordinary pools. ahp creates its resident once
 * and saves Witch=true. Modern raids remain a separate explicit human choice.
 * The native swamp-hut STRUCTURE resident and its saved marker are retained. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceWitchSpawns {
 public static boolean ordinary(MobSpawnType reason){return reason==MobSpawnType.NATURAL||reason==MobSpawnType.CHUNK_GENERATION;}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void placement(MobSpawnEvent.SpawnPlacementCheck event){if(event.getEntityType()==EntityType.WITCH&&ordinary(event.getSpawnType()))event.setResult(Event.Result.DENY);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void position(MobSpawnEvent.PositionCheck event){if(event.getEntity().getType()==EntityType.WITCH&&ordinary(event.getSpawnType()))event.setResult(Event.Result.DENY);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void finalize(MobSpawnEvent.FinalizeSpawn event){if(event.getEntity().getType()==EntityType.WITCH&&ordinary(event.getSpawnType()))event.setSpawnCancelled(true);}
 private SourceWitchSpawns(){}
}
