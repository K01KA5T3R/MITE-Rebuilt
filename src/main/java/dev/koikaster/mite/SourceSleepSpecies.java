package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.registries.ForgeRegistries;
import static dev.koikaster.mite.SourceSleepSpawnRules.Family.*;

/** R195 nt identities plus og/on/tm/rp/tn/EarthElemental spawn parents. No
 * SpawnPlacements or native Monster light check is called for sleep's false
 * perform_light_check argument. The preceding js candidate checks still run. */
public final class SourceSleepSpecies {
 private static final Set<String> CUBES=Set.of("ochre_jelly","crimson_blob","gray_ooze","black_pudding");
 private static final Set<String> SOURCE_MONSTERS=Set.of("ghoul","wight","invisible_stalker","demon_spider","hellhound","wood_spider","black_widow_spider","shadow","revenant","earth_elemental","clay_golem","bone_lord","phase_spider","fire_elemental","infernal_creeper");
 private static final Set<String> NATIVE_MONSTERS=Set.of("spider","cave_spider","zombie","zombie_villager","skeleton","wither_skeleton","creeper","enderman","silverfish","blaze","zombified_piglin");
 static String name(EntityType<?> type){var id=ForgeRegistries.ENTITY_TYPES.getKey(type);return id==null?"":id.getPath();}
 static boolean source(EntityType<?> type){var id=ForgeRegistries.ENTITY_TYPES.getKey(type);return id!=null&&id.getNamespace().equals(MiteReforged.ID);}
 public static boolean hostile(Mob mob){return mob instanceof Enemy||mob instanceof Mobs.DireWolf wolf&&wolf.sourceHellhound();}
 public static boolean excluded(Object raw){return raw==EntityType.ENDERMAN||raw==EntityType.SLIME||raw instanceof EntityType<?> type&&source(type)&&CUBES.contains(name(type));}
 public static SourceSleepSpawnRules.Profile profile(EntityType<?> type){
  String name=name(type);boolean custom=source(type);
  if(!(custom?SOURCE_MONSTERS:NATIVE_MONSTERS).contains(name))return null;
  var family=custom&&name.equals("hellhound")?HELLHOUND:custom&&name.equals("earth_elemental")?EARTH:custom&&name.equals("clay_golem")?CLAY:type==EntityType.SILVERFISH?SILVERFISH:type==EntityType.ZOMBIFIED_PIGLIN?PIGMAN:MONSTER;
  boolean leaves=custom&&(name.equals("wood_spider")||name.equals("black_widow_spider"));
  // tt/te are EntityArachnid children and inherit tm's true. tg=Enderman,
  // td=Blaze; their explicit false overrides must not be assigned to Spider.
  boolean shallow=family!=HELLHOUND&&type!=EntityType.ENDERMAN&&type!=EntityType.BLAZE&&!(custom&&(name.equals("demon_spider")||name.equals("fire_elemental")));
  return new SourceSleepSpawnRules.Profile(family,leaves,shallow);
 }
 public static boolean accepts(Mob mob){
  if(!(mob.level() instanceof ServerLevel level))return false;
  var profile=profile(mob.getType());if(profile==null)return false;
  int x=mob.getBlockX(),y=Mth.floor(mob.getBoundingBox().minY),z=mob.getBlockZ();
  var feet=new BlockPos(x,y,z);var above=feet.above();var below=level.getBlockState(feet.below());
  return SourceSleepSpawnRules.accept(profile,new SourceSleepSpawnRules.Host(){
   public int difficulty(){return level.getDifficulty().getId();}
   public boolean validEarthSupport(){return below.is(Blocks.STONE)||below.is(Blocks.OBSIDIAN)||below.is(Blocks.NETHERRACK)||below.is(Blocks.END_STONE);}
   public boolean validClaySupport(){return below.is(Blocks.CLAY);}
   public boolean unobstructedByEntities(){return level.isUnobstructed(mob);}public boolean noCollidingBoxes(){return level.noCollision(mob);}
   public boolean leavesBelow(){return below.getBlock() instanceof LeavesBlock;}
   public boolean abovePresent(){return !level.getBlockState(above).isAir();}public boolean waterAbove(){return level.getFluidState(above).is(FluidTags.WATER);}public boolean waterAtFeet(){return level.getFluidState(feet).is(FluidTags.WATER);}
   public boolean solidAbove(){return SourcePhaseNativeWorld.solid(level.getBlockState(above));}public boolean cactusAbove(){return level.getBlockState(above).is(Blocks.CACTUS);}
   public boolean lavaInBody(){return level.getBlockStates(mob.getBoundingBox()).anyMatch(s->s.getFluidState().is(FluidTags.LAVA));}public boolean anyLiquidInBody(){return level.containsAnyLiquid(mob.getBoundingBox());}
   public float pathWeight(){
    if(profile.family()==EARTH||profile.family()==CLAY)return 0;
    if(profile.family()==HELLHOUND)return SourceSpecialSpawns.sourceDay(level)&&level.canSeeSky(feet)?-.5f:10;
    return (mob.getSpawnType()==MobSpawnType.SPAWNER?.7f:.5f)-LegacyLighting.brightness(level.getMaxLocalRawBrightness(feet),0);
   }
   public boolean livingPlayerWithinFive(){return level.players().stream().anyMatch(p->!p.isSpectator()&&p.isAlive()&&!p.isRemoved()&&p.distanceToSqr(mob)<25);}
  });
 }
 private SourceSleepSpecies(){}
}
