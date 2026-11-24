package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.registries.RegistryObject;
/** tf/Infernal identities and the original js weighted-selection substitution. */
public final class SourceCreeperContent {
 public static final RegistryObject<EntityType<SourceCreeper>> INFERNAL=Mobs.ENTITIES.register("infernal_creeper",()->EntityType.Builder.of(SourceCreeper::new,MobCategory.MONSTER).sized(.6f,1.8f).fireImmune().build(MiteReforged.ID+":infernal_creeper"));
 static void register(){Content.item("infernal_creeper_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(INFERNAL,0x522c25,0xe08b35,new net.minecraft.world.item.Item.Properties()));SourceCreeperFragments.registerItems();}
 static void attributes(EntityAttributeCreationEvent event){event.put(INFERNAL.get(),SourceCreeper.attributes().build());}
 static void placements(SpawnPlacementRegisterEvent event){for(EntityType<? extends Creeper> type:java.util.List.of(EntityType.CREEPER,INFERNAL.get()))event.register(type,SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(entry,world,reason,pos,random)->SourceSpecialSpawns.allow("creeper",world,reason,pos,random),SpawnPlacementRegisterEvent.Operation.REPLACE);}
 static MobSpawnSettings.SpawnerData choose(MobSpawnSettings.SpawnerData entry,ServerLevel world,BlockPos pos,RandomSource random){
  if(world.dimensionType().hasSkyLight()&&!SourceSpecialSpawns.sourceDay(world)&&random.nextInt(4)!=0&&SourceSpawnOutdoors.isOutdoors(world,pos))return null;
  if(random.nextInt(40)>=pos.getY()&&random.nextFloat()<.5f)return new MobSpawnSettings.SpawnerData(INFERNAL.get(),entry.getWeight().asInt(),entry.minCount,entry.maxCount);return entry;
 }
 private SourceCreeperContent(){}
}
