package dev.koikaster.mite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class FruitForaging {
    private static final int PLANTS = 4;
    private static final int DRY_BONUS = 1;

    private record Fruit(String name, String raw, int satiation, int nutrition, int sugar, boolean register) {
        Diet.Profile diet(boolean dried) {
            int bonus = dried ? DRY_BONUS : 0;
            return new Diet.Profile(satiation + bonus, nutrition + bonus, sugar, PLANTS, true);
        }
    }

    private static final List<Fruit> TROPICAL = List.of(
        existing("banana", "mite_reforged:banana", 2, 1, 1000),
        fresh("pineapple", 3, 2, 1200), fresh("mango", 3, 2, 1200));
    private static final List<Fruit> SUBTROPICAL = List.of(
        existing("orange", "mite_reforged:orange", 2, 1, 1000),
        fresh("pomegranate", 3, 2, 1100), fresh("loquat", 2, 1, 900));
    private static final List<Fruit> TEMPERATE = List.of(
        fresh("pear", 3, 2, 1000), fresh("peach", 2, 2, 1000), fresh("plum", 2, 1, 900));
    private static final List<Fruit> OTHER = List.of(
        existing("apple", "minecraft:apple", 2, 1, 1000),
        existing("blueberries", "mite_reforged:blueberries", 1, 1, 1000),
        existing("sweet_berries", "minecraft:sweet_berries", 0, 2, 0),
        existing("glow_berries", "minecraft:glow_berries", 0, 2, 0),
        existing("melon_slice", "minecraft:melon_slice", 1, 1, 1000),
        existing("chorus_fruit", "minecraft:chorus_fruit", 2, 4, 0));
    private static final List<Fruit> OAK = Stream.concat(Stream.concat(SUBTROPICAL.stream(), TEMPERATE.stream()),
        Stream.of(OTHER.get(0))).toList();
    private static final List<Fruit> BIRCH = Stream.concat(SUBTROPICAL.stream(), TEMPERATE.stream()).toList();
    private static final List<Fruit> ALL = Stream.of(TROPICAL, SUBTROPICAL, TEMPERATE, OTHER).flatMap(List::stream).toList();
    private static final java.util.Set<String> ADDED = ALL.stream().filter(Fruit::register).map(Fruit::name)
        .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final Map<String, Diet.Profile> DIET = diets();

    static void register() {
        for (var fruit : ALL) {
            if (fruit.register()) Content.food(fruit.name(), fruit.nutrition(), fruit.satiation() / (2F * fruit.nutrition()), false);
        }
    }

    static List<FoodPreservation1905.Food> preservation() {
        return ALL.stream().map(fruit -> new FoodPreservation1905.Food(fruit.name(), fruit.raw(), null,
            fruit.satiation(), fruit.nutrition(), PLANTS)).toList();
    }

    private static Map<String, Diet.Profile> diets() {
        var result = new HashMap<String, Diet.Profile>();
        for (var fruit : ALL) {
            if (fruit.register()) result.put(fruit.raw(), fruit.diet(false));
            result.put("mite_reforged:dried_" + fruit.name(), fruit.diet(true));
        }
        return Map.copyOf(result);
    }

    static Diet.Profile diet(String key) { return DIET.get(key); }
    static boolean addedFruit(String name) { return ADDED.contains(name); }

    static int driedStack(String name) {
        return switch (name) {
            case "sweet_berries", "glow_berries", "chorus_fruit" -> 64;
            default -> 32;
        };
    }

    static Item leaves(BlockState state, RandomSource random, Item fallback) {
        List<Fruit> pool;
        if (state.is(Blocks.JUNGLE_LEAVES)) pool = TROPICAL;
        else if (state.is(Blocks.OAK_LEAVES)) pool = OAK;
        else if (state.is(Blocks.BIRCH_LEAVES)) pool = BIRCH;
        else return fallback;
        return FoodPreservation1905.resolve(pool.get(random.nextInt(pool.size())).raw());
    }

    static Item berry(RandomSource random) {
        return random.nextBoolean() ? Content.get("blueberries") : Items.SWEET_BERRIES;
    }

    private static Fruit fresh(String name, int satiation, int nutrition, int sugar) {
        return new Fruit(name, "mite_reforged:" + name, satiation, nutrition, sugar, true);
    }

    private static Fruit existing(String name, String raw, int satiation, int nutrition, int sugar) {
        return new Fruit(name, raw, satiation, nutrition, sugar, false);
    }

    private FruitForaging() {}
}
