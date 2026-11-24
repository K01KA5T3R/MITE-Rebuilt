package dev.koikaster.mite;

import java.time.LocalDate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.RegistryObject;

public final class SourceBatContent {
 public static final RegistryObject<EntityType<SourceGiantVampireBat>> GIANT=Mobs.ENTITIES.register("giant_vampire_bat",()->EntityType.Builder.of(SourceGiantVampireBat::new,MobCategory.AMBIENT).sized(1,1.8f).build(MiteReforged.ID+":giant_vampire_bat"));
 public static final RegistryObject<EntityType<SourceNightwing>> NIGHTWING=Mobs.ENTITIES.register("nightwing",()->EntityType.Builder.of(SourceNightwing::new,MobCategory.AMBIENT).sized(.5f,.9f).fireImmune().build(MiteReforged.ID+":nightwing"));
 static void register(){Content.item("giant_vampire_bat_spawn_egg",()->new ForgeSpawnEggItem(GIANT,0x302a32,0xb33941,new Item.Properties()));Content.item("nightwing_spawn_egg",()->new ForgeSpawnEggItem(NIGHTWING,0x15151e,0x8656af,new Item.Properties()));}
 static void attributes(EntityAttributeCreationEvent event){event.put(GIANT.get(),SourceBat.attributes(2).build());event.put(NIGHTWING.get(),SourceBat.attributes(1).build());}
 static void nativeAttributes(EntityAttributeModificationEvent event){event.add(EntityType.BAT,Attributes.MAX_HEALTH,3);}
 static void placements(SpawnPlacementRegisterEvent event){
  event.register(EntityType.BAT,SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,SourceBatContent::allow,SpawnPlacementRegisterEvent.Operation.REPLACE);
  event.register(Mobs.BAT.get(),SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,SourceBatContent::allow,SpawnPlacementRegisterEvent.Operation.REPLACE);
  event.register(GIANT.get(),SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,SourceBatContent::allow,SpawnPlacementRegisterEvent.Operation.REPLACE);
  event.register(NIGHTWING.get(),SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,SourceBatContent::allow,SpawnPlacementRegisterEvent.Operation.REPLACE);
 }
 static boolean sunlight(Level level,BlockPos pos){return SourceSpecialSpawns.sourceDay(level)&&level.canSeeSky(pos)&&!((double)level.getRainLevel(1)>.2);}
 static boolean allow(EntityType<? extends Mob> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){return allowOnDate(type,level,pos,random,LocalDate.now());}
 static boolean allowOnDate(EntityType<? extends Mob> type,ServerLevelAccessor level,BlockPos pos,RandomSource random,LocalDate date){
  if(level.getLevel().dimension()==Level.OVERWORLD&&pos.getY()>=63)return false;
  if(type==NIGHTWING.get()&&sunlight(level.getLevel(),pos))return false;
  boolean holiday=date.getMonthValue()==10&&date.getDayOfMonth()>=20||date.getMonthValue()==11&&date.getDayOfMonth()<=3;
  if(!holiday&&random.nextBoolean())return false;
  int light=level.getMaxLocalRawBrightness(pos);
  for(int y=pos.getY();y>level.getMinBuildHeight();){var at=new BlockPos(pos.getX(),--y,pos.getZ());var state=level.getBlockState(at);if(state.isSolidRender(level,at)&&state.isCollisionShapeFullBlock(level,at))break;light=Math.max(light,level.getMaxLocalRawBrightness(at));}
  return light<=random.nextInt(holiday?7:4);
 }
 static boolean unobstructed(SourceBat bat,LevelReader reader){return reader.isUnobstructed(bat)&&reader.noCollision(bat)&&!(reader.getBlockState(bat.blockPosition().below()).getBlock() instanceof LeavesBlock)&&!reader.containsAnyLiquid(bat.getBoundingBox());}
 private SourceBatContent(){}
}
