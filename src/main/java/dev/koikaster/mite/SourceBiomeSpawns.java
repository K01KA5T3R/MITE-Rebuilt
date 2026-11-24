package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.biome.*;
import net.minecraftforge.common.world.*;
import net.minecraftforge.registries.*;

/** acq and its biome constructors. AFTER_EVERYTHING replaces the early draft
 * additions, keeping the explicitly retained modern species in their native biomes. */
public final class SourceBiomeSpawns implements BiomeModifier {
 public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,MiteReforged.ID);
 public static final RegistryObject<Codec<SourceBiomeSpawns>> CODEC=SERIALIZERS.register("source_spawns",()->Codec.unit(SourceBiomeSpawns::new));
 record Row(String id,int weight,int min,int max){MobSpawnSettings.SpawnerData nativeRow(){var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));if(type==null)throw new IllegalStateException("Missing R195 spawn owner "+id);return new MobSpawnSettings.SpawnerData(type,weight,min,max);}}
 static Row vanilla(String name,int weight,int min,int max){return new Row("minecraft:"+name,weight,min,max);}
 static Row source(String name,int weight,int min,int max){return new Row(MiteReforged.ID+":"+name,weight,min,max);}
 static final List<Row> MONSTERS=List.of(vanilla("spider",80,1,2),vanilla("zombie",100,1,4),vanilla("skeleton",100,1,4),vanilla("creeper",100,1,2),vanilla("slime",100,1,4),vanilla("enderman",10,1,4),source("ghoul",10,1,1),source("wight",10,1,1),source("invisible_stalker",10,1,1),source("demon_spider",10,1,1),source("hellhound",10,1,2),source("wood_spider",20,1,1),source("shadow",10,1,1),source("revenant",10,1,1),source("earth_elemental",10,1,1),source("ochre_jelly",30,1,4),source("crimson_blob",30,1,4),source("gray_ooze",20,1,4),source("black_pudding",30,1,4),source("clay_golem",50,1,1),source("bone_lord",5,1,1),source("phase_spider",5,1,4));
 static final List<Row> ANIMALS=List.of(vanilla("sheep",10,1,1),vanilla("pig",10,1,1),vanilla("chicken",10,1,1),vanilla("cow",10,1,1));
 static final List<Row> AMBIENT=List.of(vanilla("bat",100,8,8),source("vampire_bat",20,8,8),source("nightwing",4,1,4));
 static final List<Row> AQUATIC=List.of(vanilla("squid",10,4,4));
 static final List<Row> NETHER=List.of(vanilla("ghast",50,1,2),vanilla("zombified_piglin",100,1,4),vanilla("magma_cube",10,4,4),source("earth_elemental",40,1,1));
 static final List<Row> END=List.of(vanilla("enderman",100,4,4),source("earth_elemental",20,1,4),source("ender_prowler",12,1,2),source("ender_sentinel",6,1,1));
 private static final Set<String> OWNED;
 static {
  var names=new HashSet<String>();for(var rows:List.of(MONSTERS,ANIMALS,AMBIENT,AQUATIC,NETHER,END))for(var row:rows)names.add(row.id);
  for(String name:List.of("wolf","horse","donkey","ocelot","cave_spider","witch","zombie_villager"))names.add("minecraft:"+name);
  for(String name:List.of("dire_wolf","black_widow_spider","longdead","longdead_guardian","ancient_bone_lord","giant_vampire_bat","fire_elemental","infernal_creeper","netherspawn","copperspine","hoary_silverfish"))names.add(MiteReforged.ID+":"+name);
  OWNED=Set.copyOf(names);
 }
 static List<Row> underworldMonsters(){var rows=new ArrayList<>(MONSTERS);rows.removeIf(r->Set.of("minecraft:skeleton","minecraft:zombie","mite_reforged:ghoul","mite_reforged:revenant","mite_reforged:bone_lord").contains(r.id));rows.add(vanilla("cave_spider",40,1,2));rows.add(source("longdead",40,1,2));rows.add(source("ancient_bone_lord",5,1,1));return rows;}
 static boolean owned(String id){return OWNED.contains(id);}
 private static boolean nether(Holder<Biome> biome){return biome.is(BiomeTags.IS_NETHER);}
 private static boolean end(Holder<Biome> biome){return biome.is(BiomeTags.IS_END);}
 static List<Row> rows(Holder<Biome> biome,MobCategory category){
  if(TerrainProfiles2187.isUnderworldBiome(biome))return switch(category){case MONSTER->underworldMonsters();case CREATURE->ANIMALS;case AMBIENT->AMBIENT;case WATER_CREATURE->AQUATIC;default->List.of();};
  if(nether(biome))return category==MobCategory.MONSTER?NETHER:List.of();
  if(end(biome))return category==MobCategory.MONSTER?END:List.of();
  if(biome.is(Biomes.MUSHROOM_FIELDS))return List.of();
  if(category==MobCategory.MONSTER){var rows=new ArrayList<>(MONSTERS);if(biome.is(BiomeTags.IS_JUNGLE))rows.add(source("black_widow_spider",10,1,1));if(biome.is(Biomes.SWAMP)||biome.is(Biomes.MANGROVE_SWAMP))rows.add(vanilla("slime",10,1,1));return rows;}
  if(category==MobCategory.AMBIENT)return AMBIENT;if(category==MobCategory.WATER_CREATURE)return AQUATIC;if(category!=MobCategory.CREATURE)return List.of();
  if(biome.is(BiomeTags.IS_OCEAN)||biome.is(BiomeTags.IS_RIVER)||biome.is(BiomeTags.IS_BEACH)||biome.is(Biomes.DESERT))return List.of();
  var rows=new ArrayList<>(ANIMALS);
  if(biome.is(BiomeTags.IS_TAIGA)){rows.add(vanilla("wolf",10,1,3));rows.add(source("dire_wolf",5,1,3));}
  else if(biome.value().getBaseTemperature()<=.15f){rows.add(vanilla("wolf",4,1,3));rows.add(source("dire_wolf",1,1,3));}
  else if(biome.is(BiomeTags.IS_FOREST))rows.add(vanilla("wolf",10,1,3));
  if(biome.is(BiomeTags.IS_JUNGLE)){rows.add(vanilla("ocelot",10,1,1));rows.add(vanilla("chicken",10,1,1));}
  if(biome.is(Biomes.PLAINS)||biome.is(Biomes.SUNFLOWER_PLAINS))rows.add(vanilla("horse",5,1,2));return rows;
 }
 @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
  if(phase!=Phase.AFTER_EVERYTHING||!biome.is(BiomeTags.IS_OVERWORLD)&&!nether(biome)&&!end(biome)&&!TerrainProfiles2187.isUnderworldBiome(biome))return;
  for(var category:MobCategory.values()){
   var list=builder.getMobSpawnSettings().getSpawner(category);var retained=new ArrayList<MobSpawnSettings.SpawnerData>();
   for(var row:list){var id=ForgeRegistries.ENTITY_TYPES.getKey(row.type);if(id!=null&&!OWNED.contains(id.toString())&&!ModernContentPolicy.removed(row.type))retained.add(row);}
   list.clear();for(var row:rows(biome,category))list.add(row.nativeRow());list.addAll(retained);
  }
 }
 @Override public Codec<? extends BiomeModifier> codec(){return CODEC.get();}
}
