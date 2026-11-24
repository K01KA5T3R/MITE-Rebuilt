package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

enum BiomePortalStone {
    PLAINS("plains", Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS),
    FOREST("forest", Biomes.FOREST, Biomes.FLOWER_FOREST),
    BIRCH_FOREST("birch_forest", Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST),
    DARK_FOREST("dark_forest", Biomes.DARK_FOREST),
    TAIGA("taiga", Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
    JUNGLE("jungle", Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE),
    SAVANNA("savanna", Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA),
    SWAMP("swamp", Biomes.SWAMP),
    MANGROVE_SWAMP("mangrove_swamp", Biomes.MANGROVE_SWAMP),
    DESERT("desert", Biomes.DESERT),
    BADLANDS("badlands", Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS),
    SNOWY("snowy", Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES, Biomes.SNOWY_BEACH),
    MOUNTAIN("mountain", Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
        Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS),
    MEADOW("meadow", Biomes.MEADOW),
    CHERRY_GROVE("cherry_grove", Biomes.CHERRY_GROVE),
    MUSHROOM_FIELDS("mushroom_fields", Biomes.MUSHROOM_FIELDS),
    BEACH("beach", Biomes.BEACH, Biomes.STONY_SHORE);

    private final List<ResourceKey<Biome>> biomes;
    private final RegistryObject<Block> block;

    @SafeVarargs
    BiomePortalStone(String name, ResourceKey<Biome>... biomes) {
        this.biomes = List.of(biomes);
        block = Content.block(name + "_obsidian", () -> new Marker(this));
    }

    static void register() {}

    Block block() { return block.get(); }

    boolean matches(Holder<Biome> biome) {
        return biomes.stream().anyMatch(biome::is);
    }

    static final class Marker extends Block {
        final BiomePortalStone kind;

        private Marker(BiomePortalStone kind) {
            super(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN));
            this.kind = kind;
        }
    }
}
