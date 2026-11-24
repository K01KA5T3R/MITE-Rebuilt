package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;

/** Initial land-animal and squid lists from the R195 acq constructors. */
final class ClassicInitialPopulation {
    private static final List<SourceSpawningKernel.Entry> BASE_ANIMALS = List.of(
        new SourceSpawningKernel.Entry(EntityType.SHEEP, 10, 1, 1),
        new SourceSpawningKernel.Entry(EntityType.PIG, 10, 1, 1),
        new SourceSpawningKernel.Entry(EntityType.CHICKEN, 10, 1, 1),
        new SourceSpawningKernel.Entry(EntityType.COW, 10, 1, 1));
    private static final List<SourceSpawningKernel.Entry> SQUID =
        List.of(new SourceSpawningKernel.Entry(EntityType.SQUID, 10, 4, 4));

    static List<SourceSpawningKernel.Entry> rows(int biome, SourceSpawningKernel.Group group) {
        if (group == SourceSpawningKernel.Group.AQUATIC) {
            return SQUID;
        }
        if (group != SourceSpawningKernel.Group.ANIMAL) return List.of();
        if (switch (biome) {
            case OriginalOverworldBiomes.OCEAN, OriginalOverworldBiomes.FROZEN_OCEAN,
                 OriginalOverworldBiomes.DESERT, OriginalOverworldBiomes.DESERT_HILLS,
                 OriginalOverworldBiomes.BEACH, OriginalOverworldBiomes.RIVER,
                 OriginalOverworldBiomes.FROZEN_RIVER, OriginalOverworldBiomes.DESERT_RIVER,
                 OriginalOverworldBiomes.JUNGLE_RIVER, OriginalOverworldBiomes.SWAMP_RIVER -> true;
            default -> false;
        }) return List.of();

        var rows = new ArrayList<>(BASE_ANIMALS);
        switch (biome) {
            case OriginalOverworldBiomes.PLAINS -> rows.add(new SourceSpawningKernel.Entry(EntityType.HORSE, 5, 1, 2));
            case OriginalOverworldBiomes.FOREST, OriginalOverworldBiomes.FOREST_HILLS ->
                rows.add(new SourceSpawningKernel.Entry(EntityType.WOLF, 10, 1, 3));
            case OriginalOverworldBiomes.TAIGA, OriginalOverworldBiomes.TAIGA_HILLS -> {
                rows.add(new SourceSpawningKernel.Entry(EntityType.WOLF, 10, 1, 3));
                rows.add(new SourceSpawningKernel.Entry(Mobs.WOLVES.get("dire_wolf").get(), 5, 1, 3));
            }
            case OriginalOverworldBiomes.ICE_PLAINS, OriginalOverworldBiomes.ICE_MOUNTAINS -> {
                rows.add(new SourceSpawningKernel.Entry(EntityType.WOLF, 4, 1, 3));
                rows.add(new SourceSpawningKernel.Entry(Mobs.WOLVES.get("dire_wolf").get(), 1, 1, 3));
            }
            case OriginalOverworldBiomes.JUNGLE, OriginalOverworldBiomes.JUNGLE_HILLS -> {
                rows.add(new SourceSpawningKernel.Entry(EntityType.OCELOT, 10, 1, 1));
                rows.add(new SourceSpawningKernel.Entry(EntityType.CHICKEN, 10, 1, 1));
            }
            default -> { }
        }
        return rows;
    }

    private ClassicInitialPopulation() {}
}
