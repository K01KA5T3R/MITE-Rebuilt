package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.RegistryObject;
/** Actual original normal-creature Stalker, distinct from Shadow. */
public final class SourceStalkerContent {
 public static final RegistryObject<EntityType<SourceInvisibleStalker>> STALKER=Mobs.ENTITIES.register("invisible_stalker",()->EntityType.Builder.of(SourceInvisibleStalker::new,MobCategory.MONSTER).sized(.6f,1.8f).build(MiteReforged.ID+":invisible_stalker"));
 static void register(){Content.item("invisible_stalker_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(STALKER,0x9c9c96,0x60605e,new net.minecraft.world.item.Item.Properties()));}
 static void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event){event.put(STALKER.get(),SourceInvisibleStalker.attributes().build());}
 static void placements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event){event.register(STALKER.get(),SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("invisible_stalker",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);event.register(EntityType.WITHER_SKELETON,SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("skeleton",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);}
 private SourceStalkerContent(){}
}
