package dev.koikaster.mite;

import com.mojang.serialization.Lifecycle;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

/** Preserve the exact vanilla feature, with an optional per-world reduction in attempts. */
public final class WorldBerryPatch {
    private static final int VANILLA_TRIES = 96;
    private static final int REBUILT_TRIES = 17;
    private static final ResourceKey<ConfiguredFeature<?, ?>> BERRIES = ResourceKey.create(
        Registries.CONFIGURED_FEATURE, new ResourceLocation("minecraft", "patch_berry_bush"));
    private static final Map<Registry<?>, Boolean> VANILLA_REGISTRIES = Collections.synchronizedMap(new WeakHashMap<>());

    public static <T> Holder.Reference<T> register(WritableRegistry<T> registry, ResourceKey<T> key, T value,
            Lifecycle lifecycle, Resource source) {
        var result = registry.register(key, value, lifecycle);
        if (BERRIES.equals(key) && source.source() instanceof VanillaPackResources && source.sourcePackId().equals("vanilla")) {
            VANILLA_REGISTRIES.put(registry, Boolean.TRUE);
        }
        return result;
    }

    public static int tries(RandomPatchConfiguration config, FeaturePlaceContext<?> context) {
        int original = config.tries();
        var world = context.level();
        if (original != VANILLA_TRIES || !WorldContentMode.rebuilt(world)) return original;
        var registry = world.registryAccess().registry(Registries.CONFIGURED_FEATURE).orElse(null);
        if (registry == null) return original;
        var registered = registry.get(BERRIES);
        if (registered == null || registered.config() != config || !VANILLA_REGISTRIES.containsKey(registry)) return original;
        return REBUILT_TRIES;
    }

    private WorldBerryPatch() {}
}
