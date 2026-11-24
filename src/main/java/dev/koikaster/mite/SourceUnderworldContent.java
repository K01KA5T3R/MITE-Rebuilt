package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Separate world-generation owner; no Overworld or Nether noise replacement. */
public final class SourceUnderworldContent {
 public static final RegistryObject<Block> MANTLE=Content.block("mantle",()->new SourceMantleBlock(false));
 public static final RegistryObject<Block> CORE=Content.block("core",()->new SourceMantleBlock(true));
 public static final RegistryObject<SourceGiantBrownMushroomBlock> GIANT_BROWN=Content.BLOCKS.register("source_giant_brown_mushroom",SourceGiantBrownMushroomBlock::new);
 public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,MiteReforged.ID);
 public static final RegistryObject<Codec<? extends ChunkGenerator>> UNDERWORLD=GENERATORS.register("source_underworld",()->SourceUnderworldChunkGenerator.CODEC);
 public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.FEATURES,MiteReforged.ID);
 public static final RegistryObject<SourceUnderworldDungeons> DUNGEONS=FEATURES.register("underworld_dungeons",SourceUnderworldDungeons::new);
 public static final RegistryObject<SourceUnderworldMycelium> MYCELIUM=FEATURES.register("underworld_mycelium",SourceUnderworldMycelium::new);
 public static final RegistryObject<SourceUnderworldSprings> SPRINGS=FEATURES.register("underworld_springs",SourceUnderworldSprings::new);
 public static final RegistryObject<SourceBlueberryPatch> BLUEBERRIES=FEATURES.register("blueberry_bushes",SourceBlueberryPatch::new);
 public static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,MiteReforged.ID);
 public static final RegistryObject<StructureType<UnderworldMineshaftStructure>> MINESHAFT_TYPE=
  STRUCTURES.register("underworld_mineshaft",()->()->UnderworldMineshaftStructure.CODEC);
 static final ResourceKey<StructureSet> MINESHAFTS=
  ResourceKey.create(Registries.STRUCTURE_SET,Content.id("underworld_mineshafts"));
 private SourceUnderworldContent(){}
}
