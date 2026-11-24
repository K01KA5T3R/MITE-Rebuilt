package dev.koikaster.mite;

import com.mojang.serialization.Dynamic;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** The server's primary world data owns the selection for every dimension. */
public final class WorldContentMode {
    private static final String REBUILT = "MiteRebuiltContent";
    private static final String SETTINGS_V1 = "MiteExtraSettingsV1";
    private static final String SETTINGS_V2 = "MiteExtraSettingsV2";
    private static final String SETTINGS_V3 = "MiteExtraSettingsV3";
    private static final String SETTINGS = "MiteExtraSettingsV4";
    private static final String TERRAIN = "MiteTerrainPolicyV1";
    private static final Set<String> NATIVE_NETHER_ORES = Set.of(
        "ore_gold_nether", "ore_gold_deltas", "ore_quartz_nether", "ore_quartz_deltas",
        "ore_ancient_debris_large", "ore_debris_small");
    private static final int TERRAIN_LOCKED = 1 << 8;
    private static final int CLASSIC_TERRAIN = 1 << 9;
    static final ResourceKey<StructureSet> CLASSIC_MONUMENTS =
        ResourceKey.create(Registries.STRUCTURE_SET, Content.id("classic_monuments"));

    public interface Access {
        int mite$worldOptions();
        void mite$readWorldOptions(int options);
    }

    public interface OrbAgeAccess {
        void mite$extendClassicDeathAge();
    }

    public static boolean rebuilt(Level level) {
        return settings(level).extras();
    }

    static boolean rebuilt(LevelAccessor access) {
        if (access instanceof Level level) return rebuilt(level);
        return access instanceof ServerLevelAccessor world && rebuilt(world.getLevel());
    }

    public static boolean rebuiltBosses(Level level) {
        return settings(level).rebuiltBossesEnabled();
    }

    public static boolean calamityBosses(Level level) {
        return settings(level).calamityBossesEnabled();
    }

    public static boolean ite(Level level) {
        return settings(level).ite();
    }

    public static boolean r196(Level level) {
        return settings(level).r196();
    }

    public static boolean modernTextures(Level level) {
        return settings(level).modernTexturesEnabled();
    }

    public static boolean modernMusic(Level level) {
        return settings(level).modernMusicEnabled();
    }

    public static WorldExtraSettings settings(Level level) {
        var server = level.getServer();
        return settings(server == null ? level.getLevelData() : server.getWorldData());
    }

    public static boolean modernEndGatewayAllowed(Level level) {
        var server = level.getServer();
        return server != null && !settings(level).classicVisuals() && !classicTerrain(server.getWorldData());
    }

    static WorldExtraSettings settings(Object owner) {
        int state = state(owner);
        return state < 0 ? WorldExtraSettings.DEFAULT
            : WorldExtraSettings.decode(state & WorldExtraSettings.SETTINGS_MASK, WorldExtraSettings.DEFAULT);
    }

    static boolean classicTerrain(Object owner) {
        int state = state(owner);
        if (state >= 0 && (state & TERRAIN_LOCKED) != 0) {
            return (state & CLASSIC_TERRAIN) != 0;
        }
        return settings(owner).classicVisuals();
    }

    private static int state(Object owner) {
        return owner instanceof Access mode ? mode.mite$worldOptions() - 1 : -1;
    }

    private static void setState(Object target, int state) {
        // A zero-initialized host uses defaults. A stored selection of zero is explicit.
        ((Access) target).mite$readWorldOptions(state + 1);
    }

    static void choose(Object target, WorldExtraSettings settings) {
        setState(target, settings.encode());
    }

    static void inherit(Object target, Object source) {
        setState(target, state(source));
    }

    static void load(Object world, Dynamic<?> data) {
        var legacy = WorldExtraSettings.legacy(data.get(REBUILT).asBoolean(true));
        var choice = decodeStored(
            data.get(SETTINGS).result().map(Dynamic::getValue).orElse(null),
            data.get(SETTINGS_V3).result().map(Dynamic::getValue).orElse(null),
            data.get(SETTINGS_V2).result().map(Dynamic::getValue).orElse(null),
            data.get(SETTINGS_V1).result().map(Dynamic::getValue).orElse(null), legacy);
        var storedTerrain = data.get(TERRAIN).result().map(Dynamic::getValue).orElse(null);
        // Before 0.4.0 terrain was selected by the extras flag. Keep that selection for saved worlds.
        int lockedTerrain = !choice.extras() ? CLASSIC_TERRAIN : 0;
        if (storedTerrain instanceof IntTag terrain) {
            if (terrain.getAsInt() == 0) lockedTerrain = 0;
            if (terrain.getAsInt() == 1) lockedTerrain = CLASSIC_TERRAIN;
        }
        setState(world, choice.encode() | TERRAIN_LOCKED | lockedTerrain);
    }

    private static WorldExtraSettings decodeStored(Object current, Object v3, Object v2, Object v1, WorldExtraSettings legacy) {
        var first = v1 instanceof IntTag flags ? WorldExtraSettings.decodeLegacy(flags.getAsInt(), legacy) : legacy;
        var second = v2 instanceof IntTag flags ? WorldExtraSettings.decodeV2(flags.getAsInt(), first) : first;
        var third = v3 instanceof IntTag flags ? WorldExtraSettings.decodeV3(flags.getAsInt(), second) : second;
        return current instanceof IntTag flags ? WorldExtraSettings.decode(flags.getAsInt(), third) : third;
    }

    private static void saveSettings(WorldExtraSettings settings, CompoundTag data) {
        data.putBoolean(REBUILT, settings.extras());
        data.putInt(SETTINGS_V1, settings.encode() & WorldExtraSettings.LEGACY_MASK);
        data.putInt(SETTINGS_V2, settings.encode() & WorldExtraSettings.V2_MASK);
        data.putInt(SETTINGS_V3, settings.encode() & WorldExtraSettings.V3_MASK);
        data.putInt(SETTINGS, settings.encode());
    }

    static void save(Object world, CompoundTag data) {
        saveSettings(settings(world), data);
        data.putInt(TERRAIN, classicTerrain(world) ? 1 : 0);
    }

    static void snapshot(Level level, CompoundTag data) {
        saveSettings(settings(level), data);
    }

    static void receive(Level level, CompoundTag data) {
        if (!data.contains(SETTINGS, Tag.TAG_INT) && !data.contains(SETTINGS_V3, Tag.TAG_INT)
                && !data.contains(SETTINGS_V2, Tag.TAG_INT)
                && !data.contains(SETTINGS_V1, Tag.TAG_INT)
                && !data.contains(REBUILT, Tag.TAG_BYTE)) return;
        var legacy = WorldExtraSettings.legacy(!data.contains(REBUILT, Tag.TAG_BYTE) || data.getBoolean(REBUILT));
        receive(level, decodeStored(data.get(SETTINGS), data.get(SETTINGS_V3), data.get(SETTINGS_V2),
            data.get(SETTINGS_V1), legacy));
    }

    static void receive(Level level, WorldExtraSettings settings) {
        if (level.isClientSide && level.getLevelData() instanceof Access mode) choose(mode, settings);
    }

    public static HolderLookup<StructureSet> structures(HolderLookup<StructureSet> all, ServerLevel level) {
        var excluded = new HashSet<StructureSet>();
        for (var removed : List.of(BuiltinStructureSets.ANCIENT_CITIES,
                BuiltinStructureSets.RUINED_PORTALS, BuiltinStructureSets.TRAIL_RUINS)) {
            all.get(removed).ifPresent(holder -> excluded.add(holder.value()));
        }
        if (!UnderworldExplorationRules.enabled(level)) {
            all.get(SourceUnderworldContent.MINESHAFTS).ifPresent(holder -> excluded.add(holder.value()));
        }
        var settings = settings(level);
        boolean classicTerrain = classicTerrain(level.getServer().getWorldData());
        if (!settings.classicVisuals() || !settings.rebuiltBossesEnabled() || !Level.OVERWORLD.equals(level.dimension())) {
            all.get(CLASSIC_MONUMENTS).ifPresent(holder -> excluded.add(holder.value()));
        }
        if (!classicTerrain) {
            if (settings.extras()) {
                all.get(BuiltinStructureSets.STRONGHOLDS).ifPresent(holder -> excluded.add(holder.value()));
            }
            all.get(ClassicTerrainAdapters.FORTRESSES).ifPresent(holder -> excluded.add(holder.value()));
            all.get(ClassicTerrainAdapters.TEMPLES).ifPresent(holder -> excluded.add(holder.value()));
            all.get(ClassicTerrainAdapters.MINESHAFTS).ifPresent(holder -> excluded.add(holder.value()));
            if (!settings.classicVisuals()) return all.filterElements(set -> !excluded.contains(set));
        }
        all.get(BuiltinStructureSets.VILLAGES).ifPresent(holder -> excluded.add(holder.value()));
        if (level.dimension() == Level.NETHER) {
            all.get(BuiltinStructureSets.NETHER_COMPLEXES).ifPresent(holder -> excluded.add(holder.value()));
        }
        for (var key : List.of(BuiltinStructureSets.BURIED_TREASURES,
                BuiltinStructureSets.MINESHAFTS, BuiltinStructureSets.DESERT_PYRAMIDS, BuiltinStructureSets.JUNGLE_TEMPLES, BuiltinStructureSets.SWAMP_HUTS,
                BuiltinStructureSets.END_CITIES, BuiltinStructureSets.IGLOOS, BuiltinStructureSets.NETHER_FOSSILS,
                BuiltinStructureSets.OCEAN_MONUMENTS, BuiltinStructureSets.OCEAN_RUINS, BuiltinStructureSets.PILLAGER_OUTPOSTS,
                BuiltinStructureSets.SHIPWRECKS,
                BuiltinStructureSets.WOODLAND_MANSIONS)) {
            all.get(key).ifPresent(holder -> excluded.add(holder.value()));
        }
        return all.filterElements(set -> !excluded.contains(set));
    }

    public static ChunkGeneratorStructureState structureState(ChunkGeneratorStructureState state, ServerLevel level) {
        inherit(state, level.getServer().getWorldData());
        return state;
    }

    public static Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> dimensions(
            Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> original, MinecraftServer server) {
        if (settings(server.getWorldData()).extras()) return original;
        var filtered = new LinkedHashMap<ResourceKey<LevelStem>, LevelStem>();
        for (var entry : original) {
            var key = entry.getKey();
            if (key.location().equals(CoreProgression.WORLD.location())) continue;
            filtered.put(key, entry.getValue());
        }
        return filtered.entrySet();
    }

    public static boolean allowsPlacement(PlacedFeature feature, PlacementContext context) {
        var world = context.getLevel();
        var level = world.getLevel();
        var settings = settings(level);
        var key = world.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getKey(feature);
        if (key == null) return true;
        var name = key.getPath();
        if (MiteReforged.ID.equals(key.getNamespace())) {
            return settings.extras() || !name.startsWith("design_overworld_") && !name.startsWith("design_underworld_")
                && (level.dimension() != Level.NETHER || !name.startsWith("design_nether_") && !name.equals("source_witherwood"));
        }
        if (!"minecraft".equals(key.getNamespace())) return true;
        if (settings.classicVisuals() && ClassicModernContent.placement(key)) return false;
        boolean classicTerrain = classicTerrain(level.getServer().getWorldData());
        if (level.dimension() == Level.NETHER) {
            return !classicTerrain && (!settings.extras() || !NATIVE_NETHER_ORES.contains(name));
        }
        if (!classicTerrain) return true;
        if (level.dimension() == Level.END) {
            return !name.equals("end_spike") && !name.equals("end_gateway_return")
                && !name.equals("end_island_decorated") && !name.equals("chorus_plant");
        }
        return level.dimension() != Level.OVERWORLD
            || (!name.startsWith("ore_dirt") && !name.startsWith("ore_gravel") && !name.startsWith("ore_infested")
                && (!name.equals("freeze_top_layer") || !(context.generator() instanceof ClassicOverworldChunkGenerator)));
    }

    private WorldContentMode() {}
}
