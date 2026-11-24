package dev.koikaster.mite;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

public final class ClassicDesertClimate {
    // Match the classic desert family as a share of land. Preserve all native
    // desert bands and extend them into the warm, dry edges of two land biomes.
    private static final long WARM_TEMPERATURE = 2000;
    private static final long DRY_SAVANNA = -2000;
    private static final long DRY_PLAINS = 0;

    private ClassicDesertClimate() {}

    public static Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> balanceSurface(
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> sink) {
        return entry -> {
            ResourceKey<Biome> biome = entry.getSecond();
            long moistureLimit;
            if (biome.equals(Biomes.SAVANNA)) moistureLimit = DRY_SAVANNA;
            else if (biome.equals(Biomes.PLAINS)) moistureLimit = DRY_PLAINS;
            else {
                sink.accept(entry);
                return;
            }

            Climate.ParameterPoint point = entry.getFirst();
            Climate.Parameter temperature = point.temperature();
            Climate.Parameter humidity = point.humidity();
            if (temperature.max() < WARM_TEMPERATURE || humidity.min() >= moistureLimit) {
                sink.accept(entry);
                return;
            }
            if (temperature.min() < WARM_TEMPERATURE) {
                var cool = new Climate.Parameter(temperature.min(), WARM_TEMPERATURE - 1);
                sink.accept(Pair.of(withClimate(point, cool, humidity), biome));
            }
            var warm = new Climate.Parameter(Math.max(temperature.min(), WARM_TEMPERATURE), temperature.max());
            if (humidity.max() >= moistureLimit) {
                var wet = new Climate.Parameter(moistureLimit, humidity.max());
                sink.accept(Pair.of(withClimate(point, warm, wet), biome));
            }
            var dry = new Climate.Parameter(humidity.min(), Math.min(humidity.max(), moistureLimit - 1));
            sink.accept(Pair.of(withClimate(point, warm, dry), Biomes.DESERT));
        };
    }

    private static Climate.ParameterPoint withClimate(Climate.ParameterPoint point,
            Climate.Parameter temperature, Climate.Parameter humidity) {
        return new Climate.ParameterPoint(temperature, humidity,
                point.continentalness(), point.erosion(), point.depth(), point.weirdness(), point.offset());
    }
}
