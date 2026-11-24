package dev.koikaster.mite;

import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.storage.WorldData;

/** Select the advancement view using the world that owns the resource load. */
public final class ModernAdvancementPolicy548 {
    private static final Map<ResourceManager, Boolean> WORLD_LOADS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerAdvancementManager, Map<ResourceLocation, JsonElement>> PREVIEWS =
        Collections.synchronizedMap(new WeakHashMap<>());

    public interface LiveManager {
        void mite$applyWorldAdvancements(Map<ResourceLocation, JsonElement> source, ResourceManager resources, ProfilerFiller profiler);
    }

    public static CompletableFuture<ReloadableServerResources> loadForWorld(ResourceManager resources, RegistryAccess.Frozen registry,
            FeatureFlagSet features, Commands.CommandSelection commands, int functionLevel, Executor background, Executor foreground,
            Object owner) {
        Object resolved = owner instanceof WorldLoader.DataLoadOutput<?> output ? output.cookie() : owner;
        if (resolved instanceof MinecraftServer server) resolved = server.getWorldData();
        if (!(resolved instanceof WorldData world)) {
            return ReloadableServerResources.loadResources(resources, registry, features, commands, functionLevel, background, foreground);
        }
        WORLD_LOADS.put(resources, displaysVanillaAdvancements(world));
        try {
            return ReloadableServerResources.loadResources(resources, registry, features, commands, functionLevel, background, foreground)
                .whenComplete((result, failure) -> WORLD_LOADS.remove(resources));
        } catch (RuntimeException | Error failure) {
            WORLD_LOADS.remove(resources);
            throw failure;
        }
    }

    public static Map<ResourceLocation, JsonElement> forManager(ServerAdvancementManager manager,
            Map<ResourceLocation, JsonElement> source, ResourceManager resources) {
        var mode = WORLD_LOADS.get(resources);
        if (mode == null) {
            var snapshot = new LinkedHashMap<ResourceLocation, JsonElement>();
            source.forEach((id, value) -> snapshot.put(id, value.deepCopy()));
            PREVIEWS.put(manager, snapshot);
        }
        return select(source, mode == null || mode);
    }

    public static void finishPreview(ReloadableServerResources loaded, WorldData world, ResourceManager resources) {
        var advances = loaded.getAdvancements();
        var source = PREVIEWS.remove(advances);
        if (source == null || displaysVanillaAdvancements(world)) return;
        WORLD_LOADS.put(resources, Boolean.FALSE);
        try {
            ((LiveManager) advances).mite$applyWorldAdvancements(source, resources, InactiveProfiler.INSTANCE);
        } finally {
            WORLD_LOADS.remove(resources);
        }
    }

    static Map<ResourceLocation, JsonElement> select(Map<ResourceLocation, JsonElement> source, boolean showVanilla) {
        var result = new LinkedHashMap<ResourceLocation, JsonElement>();
        source.forEach((id, value) -> {
            boolean hide = !id.getPath().startsWith("recipes/") && (id.getNamespace().equals(MiteReforged.ID)
                || (!showVanilla && id.getNamespace().equals("minecraft")));
            if (hide && value.isJsonObject() && (value.getAsJsonObject().has("display") || value.getAsJsonObject().has("rewards"))) {
                var copy = value.getAsJsonObject().deepCopy();
                copy.remove("display");
                copy.remove("rewards");
                result.put(id, copy);
            } else {
                result.put(id, value);
            }
        });
        return result;
    }

    private static boolean displaysVanillaAdvancements(WorldData world) {
        var mode = WorldContentMode.settings(world);
        return !mode.classicVisuals() && !mode.extras();
    }

    private ModernAdvancementPolicy548() {}
}

