package dev.koikaster.mite;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Selects the runtime terrain using the primary world's immutable mode and seed. */
public final class ClassicTerrainAdapters {
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOMES =
        DeferredRegister.create(Registries.BIOME_SOURCE, MiteReforged.ID);
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
        DeferredRegister.create(Registries.CHUNK_GENERATOR, MiteReforged.ID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
        DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, MiteReforged.ID);

    static final ResourceKey<StructureSet> FORTRESSES =
        ResourceKey.create(Registries.STRUCTURE_SET, Content.id("classic_fortresses"));
    static final RegistryObject<StructurePlacementType<ClassicFortressPlacement>> FORTRESS_PLACEMENT =
        PLACEMENTS.register("classic_fortress", () -> () -> ClassicFortressPlacement.SERIALIZER);

    static final ResourceKey<StructureSet> TEMPLES =
        ResourceKey.create(Registries.STRUCTURE_SET, Content.id("classic_temples"));
    static final RegistryObject<StructurePlacementType<ClassicTemplePlacement>> TEMPLE_PLACEMENT =
        PLACEMENTS.register("classic_temple", () -> () -> ClassicTemplePlacement.SERIALIZER);

    static final ResourceKey<StructureSet> MINESHAFTS =
        ResourceKey.create(Registries.STRUCTURE_SET, Content.id("classic_mineshafts"));
    static final RegistryObject<StructurePlacementType<ClassicMineshaftPlacement>> MINESHAFT_PLACEMENT =
        PLACEMENTS.register("classic_mineshaft", () -> () -> ClassicMineshaftPlacement.SERIALIZER);

    static {
        BIOMES.register("classic_overworld", () -> ClassicOverworldBiomeSource.SERIALIZER);
        GENERATORS.register("classic_overworld", () -> ClassicOverworldChunkGenerator.SERIALIZER);
        GENERATORS.register("classic_nether", () -> ClassicNetherChunkGenerator.SERIALIZER);
        GENERATORS.register("classic_end", () -> ClassicEndChunkGenerator.SERIALIZER);
    }

    static void register(IEventBus bus) {
        BIOMES.register(bus);
        GENERATORS.register(bus);
        PLACEMENTS.register(bus);
    }
    private static final ResourceKey<DimensionType> CLASSIC_OVERWORLD = ResourceKey.create(
        Registries.DIMENSION_TYPE, Content.id("classic_overworld"));
    private static final ResourceKey<DimensionType> CLASSIC_NETHER = ResourceKey.create(
        Registries.DIMENSION_TYPE, Content.id("classic_nether"));

    private static final ResourceKey<NoiseGeneratorSettings> NETHER_WITHOUT_CORE = ResourceKey.create(
        Registries.NOISE_SETTINGS, Content.id("nether_vanilla"));

    public static LevelStem select(LevelStem vanilla, MinecraftServer server, ResourceKey<Level> key) {
        if (!(vanilla.generator() instanceof NoiseBasedChunkGenerator noise)) return vanilla;
        var data = server.getWorldData();
        if (!WorldContentMode.classicTerrain(data)) {
            if (key == Level.NETHER && !WorldContentMode.settings(data).extras()
                    && noise.generatorSettings().is(NoiseGeneratorSettings.NETHER)) {
                // The shipped default nether surface opens onto the optional core.
                // Settings pinned in a previous release keep their own keys.
                var settings = server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS)
                    .getHolderOrThrow(NETHER_WITHOUT_CORE);
                return new LevelStem(vanilla.type(), new NoiseBasedChunkGenerator(noise.getBiomeSource(), settings));
            }
            return vanilla;
        }
        long seed = server.getWorldData().worldGenOptions().seed();
        var registries = server.registryAccess();
        var biomes = registries.registryOrThrow(Registries.BIOME);
        var dimensionTypes = registries.registryOrThrow(Registries.DIMENSION_TYPE);
        Holder<DimensionType> dimension = vanilla.type();
        ChunkGenerator generator;
        if (key == Level.OVERWORLD) {
            dimension = dimensionTypes.getHolderOrThrow(CLASSIC_OVERWORLD);
            generator = new ClassicOverworldChunkGenerator(new ClassicOverworldBiomeSource(seed, true, biomes), server);
        } else if (key == Level.NETHER) {
            dimension = dimensionTypes.getHolderOrThrow(CLASSIC_NETHER);
            generator = new ClassicNetherChunkGenerator(seed, biomes.getHolderOrThrow(Biomes.NETHER_WASTES));
        } else if (key == Level.END) {
            generator = new ClassicEndChunkGenerator(seed, biomes.getHolderOrThrow(Biomes.THE_END));
        } else {
            return vanilla;
        }
        return new LevelStem(dimension, generator);
    }

    private ClassicTerrainAdapters() {}
}
