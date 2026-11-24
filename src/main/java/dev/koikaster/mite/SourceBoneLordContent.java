package dev.koikaster.mite;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.RegistryObject;
/** Actual R195 species. */
public final class SourceBoneLordContent {
 public static final RegistryObject<EntityType<SourceBoneLord>> LORD=Mobs.ENTITIES.register("bone_lord",()->EntityType.Builder.of(SourceBoneLord::new,MobCategory.MONSTER).sized(.6f,1.8f).build(MiteReforged.ID+":bone_lord"));
 public static final RegistryObject<EntityType<SourceAncientBoneLord>> ANCIENT=Mobs.ENTITIES.register("ancient_bone_lord",()->EntityType.Builder.of(SourceAncientBoneLord::new,MobCategory.MONSTER).sized(.6f,1.8f).build(MiteReforged.ID+":ancient_bone_lord"));
 static void register(){Content.item("bone_lord_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(LORD,0x9e9e89,0x5b3526,new net.minecraft.world.item.Item.Properties()));Content.item("ancient_bone_lord_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ANCIENT,0x715849,0x8b6840,new net.minecraft.world.item.Item.Properties()));}
 static void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event){event.put(LORD.get(),SourceBoneLord.attributes(false).build());event.put(ANCIENT.get(),SourceBoneLord.attributes(true).build());}
 static void placements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event){
  event.register(EntityType.SKELETON,SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("skeleton",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
  event.register(LORD.get(),SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("bone_lord",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
  event.register(ANCIENT.get(),SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("ancient_bone_lord",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
 }
 private SourceBoneLordContent(){}
}
