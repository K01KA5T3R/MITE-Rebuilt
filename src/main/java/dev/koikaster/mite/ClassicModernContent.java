package dev.koikaster.mite;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraftforge.registries.ForgeRegistries;

/** Vanilla additions that have no gameplay counterpart in the R195 catalogue. */
public final class ClassicModernContent {
    private static final Set<String> IDS = Set.of(
        "bee_nest", "beehive", "honey_block", "honeycomb_block", "honeycomb", "honey_bottle",
        "amethyst_shard", "amethyst_block", "budding_amethyst", "amethyst_cluster",
        "small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud",
        "calcite", "tuff", "pointed_dripstone", "dripstone_block", "glow_berries", "cave_vines",
        "cave_vines_plant", "moss_block", "moss_carpet", "glow_lichen", "spore_blossom",
        "azalea", "flowering_azalea", "azalea_leaves", "flowering_azalea_leaves",
        "big_dripleaf", "big_dripleaf_stem", "small_dripleaf", "rooted_dirt", "hanging_roots",
        "crossbow", "trident", "elytra", "totem_of_undying", "spyglass", "spectral_arrow", "tipped_arrow",
        "bell", "lectern", "cartography_table", "smithing_table", "fletching_table", "loom", "barrel",
        "grindstone", "composter", "target", "lightning_rod",
        "cod_bucket", "salmon_bucket", "tropical_fish", "tropical_fish_bucket", "pufferfish", "pufferfish_bucket",
        "dried_kelp", "dried_kelp_block", "kelp", "kelp_plant", "seagrass", "tall_seagrass", "sea_pickle",
        "bubble_column", "bamboo", "bamboo_sapling", "sweet_berries", "sweet_berry_bush",
        "chorus_fruit", "popped_chorus_fruit", "chorus_flower", "chorus_plant", "purpur_block",
        "purpur_pillar", "purpur_stairs", "purpur_slab", "end_rod", "shulker_shell", "shulker_box",
        "dragon_breath", "crying_obsidian", "gilded_blackstone", "blackstone", "polished_blackstone",
        "chiseled_polished_blackstone", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",
        "basalt", "polished_basalt", "smooth_basalt", "soul_soil", "nether_sprouts", "shroomlight",
        "twisting_vines", "twisting_vines_plant", "weeping_vines", "weeping_vines_plant",
        "lantern", "soul_lantern", "soul_torch", "soul_wall_torch", "chain", "lodestone", "netherite_block",
        "magma_block", "prismarine", "prismarine_bricks", "dark_prismarine", "prismarine_shard",
        "prismarine_crystals", "sea_lantern", "wet_sponge", "lily_of_the_valley", "cornflower",
        "azure_bluet", "sunflower", "lilac", "rose_bush",
        "peony", "red_tulip", "white_tulip", "pink_tulip", "orange_tulip",
        "allium", "blue_orchid", "oxeye_daisy", "wither_rose", "torchflower",
        "pink_petals", "azalea_bush", "flowering_azalea_bush", "end_crystal",
        "glow_ink_sac", "glow_item_frame", "goat_horn", "powder_snow", "powder_snow_bucket", "tinted_glass",
        "raw_copper", "raw_iron", "raw_gold", "raw_copper_block", "raw_iron_block", "raw_gold_block",
        "beetroot", "beetroot_seeds", "beetroot_soup", "beetroots", "rabbit", "cooked_rabbit",
        "rabbit_stew", "rabbit_hide", "rabbit_foot", "suspicious_stew",
        "red_sand", "slime_block", "packed_ice", "blue_ice",
        "granite", "diorite", "andesite", "smooth_stone", "leather_horse_armor", "armor_stand"
    );
    private static final Set<String> CLASSIC_PLATES = Set.of("oak_pressure_plate", "stone_pressure_plate",
        "light_weighted_pressure_plate", "heavy_weighted_pressure_plate");
    private static final Set<String> STONES = Set.of("granite", "diorite", "andesite", "blackstone", "prismarine", "dark_prismarine");
    private static final Set<String> WOODS = Set.of("acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped");
    private static final Set<String> ENTITIES = Set.of(
        "bee", "cat", "drowned", "husk", "stray", "phantom", "endermite", "parrot", "pillager", "vindicator", "evoker", "evoker_fangs",
        "illusioner", "vex", "ravager", "guardian", "elder_guardian", "shulker", "shulker_bullet",
        "cod", "salmon", "pufferfish", "tropical_fish", "glow_squid", "axolotl", "goat",
        "fox", "rabbit", "llama", "panda", "camel", "piglin", "piglin_brute", "hoglin", "zoglin", "strider",
        "warden", "armor_stand", "glow_item_frame", "chest_boat"
    );
    private static final Set<String> PLACEMENTS = Set.of(
        "amethyst_geode", "cave_vines", "dripstone_cluster", "large_dripstone", "pointed_dripstone",
        "glow_lichen", "spore_blossom", "lush_caves_ceiling_vegetation", "lush_caves_clay",
        "lush_caves_vegetation", "rooted_azalea_tree", "kelp_cold", "kelp_warm", "sea_pickle",
        "warm_ocean_vegetation", "underwater_magma", "iceberg_blue", "iceberg_packed", "blue_ice",
        "patch_berry_bush", "patch_berry_common", "patch_berry_rare", "patch_sunflower",
        "cherry_bees_005", "cherry_checked", "flower_cherry", "trees_cherry",
        "ore_andesite_lower", "ore_andesite_upper", "ore_diorite_lower", "ore_diorite_upper",
        "ore_granite_lower", "ore_granite_upper", "ore_tuff", "ore_emerald", "forest_rock",
        "fossil_lower", "fossil_upper", "disk_grass"
    );

    static boolean item(ItemStack stack) {
        return itemId(ForgeRegistries.ITEMS.getKey(stack.getItem()))
            || stack.getItem() instanceof SpawnEggItem egg && entity(egg.getType(stack.getTag()));
    }

    static boolean itemId(ResourceLocation id) {
        if (id == null) return false;
        if (MiteReforged.ID.equals(id.getNamespace())) {
            String path = id.getPath();
            return path.endsWith("_cod_bucket") || path.endsWith("_salmon_bucket")
                || path.endsWith("_pufferfish_bucket") || path.endsWith("_tropical_fish_bucket")
                || path.endsWith("_axolotl_bucket");
        }
        return vanilla(id) && modernPath(id.getPath());
    }

    static boolean block(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return vanilla(id) && modernPath(id.getPath());
    }

    static boolean entity(EntityType<?> type) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return vanilla(id) && ENTITIES.contains(id.getPath());
    }

    static boolean entity(Entity entity) {
        return entity(entity.getType()) || entity instanceof Boat boat && boat.getVariant() != Boat.Type.OAK;
    }

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> nativeSpawnPool(
            WeightedRandomList<MobSpawnSettings.SpawnerData> pool, ServerLevel level) {
        var settings = WorldContentMode.settings(level);
        if (!settings.classicVisuals() && settings.rebuiltBossesEnabled() && settings.calamityBossesEnabled()) return pool;
        return WeightedRandomList.create(pool.unwrap().stream()
            .filter(row -> RebuiltContent.allows(row.type, settings)).toList());
    }

    static boolean placement(ResourceLocation id) {
        if (!vanilla(id)) return false;
        String path = id.getPath();
        return PLACEMENTS.contains(path) || path.startsWith("seagrass_") || path.startsWith("bamboo")
            || path.startsWith("sculk_");
    }

    /** The tree still generates; only its native beehive decorator is gated. */
    public static boolean beehive(TreeDecorator.Context context) {
        if (!(context.level() instanceof ServerLevelAccessor world)) return true;
        var settings = WorldContentMode.settings(world.getLevel());
        // Chunk generation uses WorldGenRegion. A grown sapling uses the live ServerLevel.
        // Classic worlds can supply honey through grown trees for the explicitly enabled Bee encounter.
        return !settings.classicVisuals() || world instanceof ServerLevel && settings.calamityBossesEnabled();
    }

    private static boolean modernPath(String path) {
        if (path.startsWith("potted_")) path = path.substring("potted_".length());
        if (IDS.contains(path) || path.equals("ancient_debris") || path.startsWith("netherite_")) return true;
        if (path.endsWith("_banner") || path.endsWith("_banner_pattern")) return true;
        if (path.endsWith("_shulker_box") || path.endsWith("_hanging_sign") || path.endsWith("_chest_boat")
                || path.endsWith("_chest_raft") || path.endsWith("_coral") || path.endsWith("_coral_fan")
                || path.endsWith("_coral_block") || path.endsWith("_coral_wall_fan") || path.endsWith("_concrete")
                || path.endsWith("_concrete_powder") || path.endsWith("_glazed_terracotta")
                || path.startsWith("waxed_") || path.startsWith("cut_copper") || path.startsWith("exposed_copper")
                || path.startsWith("weathered_copper") || path.startsWith("oxidized_copper")) return true;
        if (path.startsWith("stripped_") || path.startsWith("dyed_")) return true;
        if (path.endsWith("_door") && !path.equals("oak_door") && !path.equals("iron_door")) return true;
        if (path.endsWith("_trapdoor") && !path.equals("oak_trapdoor")) return true;
        if (path.endsWith("_button") && !path.equals("oak_button") && !path.equals("stone_button")) return true;
        if (path.endsWith("_pressure_plate") && !CLASSIC_PLATES.contains(path)) return true;
        if (path.endsWith("_boat") && !path.equals("oak_boat")) return true;
        for (String wood : WOODS) if (path.startsWith(wood + "_")) return true;
        for (String modern : STONES) {
            if (path.startsWith(modern + "_") || path.startsWith("polished_" + modern + "_")) return true;
        }
        return false;
    }

    private static boolean vanilla(ResourceLocation id) {
        return id != null && "minecraft".equals(id.getNamespace());
    }

    private ClassicModernContent() {}
}
