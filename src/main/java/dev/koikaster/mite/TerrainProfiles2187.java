package dev.koikaster.mite;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldDimensions;

/** Persist the shipped generator used by an existing save before the normal worldgen codecs read it. */
public final class TerrainProfiles2187 {
    private static final Pattern RELEASE = Pattern.compile("0\\.1\\.([0-9]{1,3})");
    private static final String CORE = "mite_reforged:core";
    private static final String CORE_FIRST = "mite_reforged:core_167";
    private static final String CORE_REEFS = "mite_reforged:core_169";
    private static final String NETHER_VANILLA = "mite_reforged:nether_vanilla";
    private static final String NETHER_BEDROCK = "mite_reforged:nether_007";
    private static final String NETHER_COVERED_CORE = "mite_reforged:nether_167";
    private static final String UNDERWORLD = "mite_reforged:underworld";
    private static final String UNDERWORLD_ORE_032 = "mite_reforged:underworld_ore_032";
    private static final String UNDERWORLD_ORE_120 = "mite_reforged:underworld_ore_120";
    private static final String UNDERWORLD_ORE_137 = "mite_reforged:underworld_ore_137";
    private static final Set<String> UNDERWORLD_ORE_PROFILES = Set.of(UNDERWORLD_ORE_032, UNDERWORLD_ORE_120, UNDERWORLD_ORE_137);

    public static CompoundTag pin(CompoundTag root) {
        int release = previousRelease(root);
        if (release < 2 || release >= 172) return root;
        var dimensions = root.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions");
        if (release >= 32 && release < 167) {
            var generator = dimensions.getCompound(UNDERWORLD).getCompound("generator");
            var biome = generator.getCompound("biome_source");
            if (generator.getString("type").equals("mite_reforged:source_underworld")
                && biome.getString("type").equals("minecraft:fixed") && biome.getString("biome").equals(UNDERWORLD)) {
                biome.putString("biome", release < 120 ? UNDERWORLD_ORE_032 : release < 137 ? UNDERWORLD_ORE_120 : UNDERWORLD_ORE_137);
            }
        }
        var nether = dimensions.getCompound("minecraft:the_nether").getCompound("generator");
        if (nether.getString("type").equals("minecraft:noise") && nether.getString("settings").equals("minecraft:nether")) {
            if (release < 7) nether.putString("settings", NETHER_VANILLA);
            else if (release < 167) nether.putString("settings", NETHER_BEDROCK);
            else if (release < 169) nether.putString("settings", NETHER_COVERED_CORE);
        }
        var core = dimensions.getCompound(CORE).getCompound("generator");
        var biome = core.getCompound("biome_source");
        if (release >= 167 && core.getString("type").equals("minecraft:noise") && core.getString("settings").equals(CORE)
            && biome.getString("type").equals("minecraft:fixed") && biome.getString("biome").equals(CORE)) {
            var profile = release < 169 ? CORE_FIRST : CORE_REEFS;
            core.putString("settings", profile);
            biome.putString("biome", profile);
        }
        return root;
    }

    private static int previousRelease(CompoundTag root) {
        var mods = root.getCompound("fml").getList("LoadingModList", Tag.TAG_COMPOUND);
        for (int i = 0; i < mods.size(); i++) {
            var mod = mods.getCompound(i);
            if (!mod.getString("ModId").equals(MiteReforged.ID)) continue;
            var match = RELEASE.matcher(mod.getString("ModVersion"));
            return match.matches() ? Integer.parseInt(match.group(1)) : -1;
        }
        return -1;
    }

    private static String settings(ChunkGenerator chunkGenerator) {
        if (!(chunkGenerator instanceof NoiseBasedChunkGenerator generator)) return "";
        return generator.generatorSettings().unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    private static String biomeId(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    static boolean isUnderworldBiome(Holder<Biome> biome) {
        String id = biomeId(biome);
        return id.equals(UNDERWORLD) || UNDERWORLD_ORE_PROFILES.contains(id);
    }

    public static Optional<LevelStem> packOrPinned(Registry<LevelStem> pack, ResourceKey<LevelStem> key, WorldDimensions saved) {
        if (key.location().toString().equals(UNDERWORLD)) {
            var existing = saved.get(key);
            if (existing.isPresent() && existing.get().generator() instanceof SourceUnderworldChunkGenerator generator
                && generator.getBiomeSource() instanceof FixedBiomeSource fixed
                && fixed.possibleBiomes().stream().map(TerrainProfiles2187::biomeId).anyMatch(UNDERWORLD_ORE_PROFILES::contains)) {
                return existing;
            }
        }
        if (key.equals(LevelStem.NETHER) || key.location().toString().equals(CORE)) {
            var existing = saved.get(key);
            if (existing.isPresent()) {
                var profile = settings(existing.get().generator());
                boolean pinned = key.equals(LevelStem.NETHER)
                    ? profile.equals(NETHER_VANILLA) || profile.equals(NETHER_BEDROCK) || profile.equals(NETHER_COVERED_CORE)
                    : profile.equals(CORE_FIRST) || profile.equals(CORE_REEFS);
                if (pinned) return existing;
            }
        }
        return pack.getOptional(key);
    }

    static int coreArrivalDepth(ChunkGenerator generator) {
        var profile = settings(generator);
        if (profile.equals(CORE_FIRST)) return -1;
        return profile.equals(CORE_REEFS) ? 12 : 48;
    }

    private TerrainProfiles2187() {}
}
