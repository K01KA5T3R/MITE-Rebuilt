package dev.koikaster.mite;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;

/** Actual source species, distinct from Revenant and the human-selected boss. */
public final class SourceLongdeadContent {
 public static final RegistryObject<EntityType<SourceLongdead>> LONGDEAD=Mobs.ENTITIES.register("longdead",()->EntityType.Builder.of(SourceLongdead::new,MobCategory.MONSTER).sized(.6f,1.8f).build(MiteReforged.ID+":longdead"));
 public static final RegistryObject<EntityType<SourceLongdeadGuardian>> GUARDIAN=Mobs.ENTITIES.register("longdead_guardian",()->EntityType.Builder.of(SourceLongdeadGuardian::new,MobCategory.MONSTER).sized(.6f,1.8f).build(MiteReforged.ID+":longdead_guardian"));
 static void register(){
  Content.item("longdead_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(LONGDEAD,0x715849,0xb49f7d,new net.minecraft.world.item.Item.Properties()));
  Content.item("longdead_guardian_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(GUARDIAN,0x3d3830,0xb97628,new net.minecraft.world.item.Item.Properties()));
 }
 static void placements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event){
  event.register(LONGDEAD.get(),net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("longdead",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
  event.register(GUARDIAN.get(),net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->SourceSpecialSpawns.allow("longdead_guardian",level,reason,pos,random),net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
 }
 private SourceLongdeadContent(){}
}
