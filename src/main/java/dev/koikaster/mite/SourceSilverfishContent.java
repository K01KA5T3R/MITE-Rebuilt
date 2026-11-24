package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.registries.RegistryObject;

/** The two source leaf classes have no overrides; their behavior belongs to tq. */
public final class SourceSilverfishContent {
 public static final RegistryObject<EntityType<SourceSilverfish>> COPPERSPINE=type("copperspine");
 public static final RegistryObject<EntityType<SourceSilverfish>> HOARY=type("hoary_silverfish");
 private static RegistryObject<EntityType<SourceSilverfish>> type(String name){return Mobs.ENTITIES.register(name,()->EntityType.Builder.of(SourceSilverfish::new,MobCategory.MONSTER).sized(.3f,.7f).build(MiteReforged.ID+":"+name));}
 static void register(){for(var type:java.util.List.of(COPPERSPINE,HOARY))Content.item(type.getId().getPath()+"_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(type,0x73736c,type==COPPERSPINE?0xae6839:0xe0e0d7,new net.minecraft.world.item.Item.Properties()));SourceInfestation.register();}
 static void attributes(EntityAttributeCreationEvent event){event.put(COPPERSPINE.get(),SourceSilverfish.attributes().build());event.put(HOARY.get(),SourceSilverfish.attributes().build());}
 static void placements(SpawnPlacementRegisterEvent event){
  for(EntityType<? extends Silverfish> type:java.util.List.of(EntityType.SILVERFISH,COPPERSPINE.get(),HOARY.get()))event.register(type,SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,SourceSilverfishContent::allow,SpawnPlacementRegisterEvent.Operation.REPLACE);
 }
 static boolean allow(EntityType<? extends Mob> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
  if(!SourceSpecialSpawns.allow("silverfish",level,reason,pos,random))return false;
  // tq's additional 5-block alive-player exclusion includes creative players.
  for(var player:level.getLevel().players())if(player.isAlive()&&!player.isSpectator()&&player.distanceToSqr(pos.getX()+.5,pos.getY(),pos.getZ()+.5)<25)return false;
  return true;
 }
 private SourceSilverfishContent(){}
}
