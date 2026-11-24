package dev.koikaster.mite;

import java.util.Set;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Identities of systems added by Rebuilt, independent of the shared MITE namespace. */
public final class RebuiltContent {
    private static final Set<String> ITEMS = Set.of(
        "torch_bow", "drying_rack", "shared_plate", "advanced_enchanting_table", "mounted_crossbow", "grave",
        "scorched_food", "measuring_rule", "leather_air_bladder", "plant_fibre",
        "rusted_iron_nugget", "rusted_iron_ingot", "mithril_auto_workbench",
        "high_heat_ore", "high_energy_ender_eye", "ender_obsidian",
        "raw_end_porkchop", "cooked_end_porkchop", "raw_end_beef", "cooked_end_beef"
    );
    private static final Set<String> MODERN_COOKING_ITEMS = Set.of(
        "campfire", "soul_campfire", "smoker", "blast_furnace"
    );
    private static final Set<String> ENTITIES = Set.of(
        "torch_shot", "mounted_crossbow_seat",
        "ender_prowler", "ender_sentinel", "end_pig", "end_cow"
    );
    private static final Set<String> RECIPE_IDS = Set.of(
        "gunpowder_from_charcoal", "advanced_enchanting_table", "torch_bow",
        "rusted_iron_reclaim"
    );

    private static final Set<String> REBUILT_BOSS_ITEMS = Set.of(
        "raider_banner", "shock_star", "large_bone", "large_bone_pickaxe", "undead_sigil", "bandage",
        "giant_call", "giant_trophy", "monument_call", "elder_guardian_trophy", "wither_trophy"
    );
    private static final Set<String> CALAMITY_ITEMS = Set.of(
        "slime_crown", "fungal_sprout", "fungal_ingot", "fungal_pickaxe", "fungal_spear",
        "slime_hook", "ninja_hood", "slime_cushion", "honey_bait", "stinger_boomerang"
    );
    private static final Set<String> REBUILT_BOSS_ENTITIES = Set.of(
        "mythril_ravager", "mythril_knight", "redstone_iron_golem", "underworld_giant_skeleton"
    );
    private static final Set<String> CALAMITY_ENTITIES = Set.of(
        "king_slime", "king_slime_jewel", "king_slime_jewel_bolt", "king_slime_minion",
        "crabulon", "crabulon_spore", "crabulon_brood",
        "queen_bee", "queen_bee_guard", "queen_bee_stinger", "thrown_stinger_boomerang"
    );
    private static final Set<String> MONUMENT_ITEMS = Set.of(
        "prismarine_shard", "prismarine_crystals"
    );
    private static final Set<String> HONEY_ITEMS = Set.of("bee_nest", "beehive", "honeycomb", "honey_bottle");

    public static boolean allows(ItemStack stack, WorldExtraSettings settings) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof R196Workbench bench) return bench.tier() == R196Workbench.Tier.VIBRANIUM ? settings.ite() : settings.r196();
        if (stack.getItem() instanceof SpawnEggItem egg) return allows(egg.getType(stack.getTag()), settings);
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (mine(id)) {
            if (IteContent0416.isIteItem(id.getPath())) return settings.ite();
            if (REBUILT_BOSS_ITEMS.contains(id.getPath())) return settings.rebuiltBossesEnabled();
            if (CALAMITY_ITEMS.contains(id.getPath())) return settings.calamityBossesEnabled();
        }
        if (settings.classicVisuals() && modernItem(stack, id) && !vanillaBossSupply(id, settings)) return false;
        return settings.extras() || !rebuiltItem(stack, id);
    }

    static boolean allows(BlockState state, WorldExtraSettings settings) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (settings.classicVisuals() && ClassicModernContent.block(state) && !vanillaBossSupply(id, settings)) return false;
        return allows(new ItemStack(state.getBlock().asItem()), settings);
    }

    static boolean allows(EntityType<?> type, WorldExtraSettings settings) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (mine(id)) {
            if (IteEntities0418.isIteEntity(id.getPath())) return settings.ite();
            if (REBUILT_BOSS_ENTITIES.contains(id.getPath())) return settings.rebuiltBossesEnabled();
            if (CALAMITY_ENTITIES.contains(id.getPath())) return settings.calamityBossesEnabled();
        }
        if (settings.classicVisuals() && ClassicModernContent.entity(type)) return false;
        return settings.extras() || !mine(id) || !ENTITIES.contains(id.getPath());
    }

    static boolean allows(Entity entity, WorldExtraSettings settings) {
        if (entity instanceof SourceGiantZombieBoss giant && giant.ritual()
                || entity instanceof SourceElderGuardianBoss elder && elder.ritual()) return settings.rebuiltBossesEnabled();
        if (settings.classicVisuals()) {
            var type = entity.getType();
            if (type == EntityType.CAT && entity instanceof net.minecraft.world.entity.animal.Cat cat) {
                return ClassicCats.allows(cat);
            }
            // Fixed monument residents and managed beehives are separate from ordinary native spawn pools.
            if (type == EntityType.GUARDIAN && settings.rebuiltBossesEnabled()
                    || type == EntityType.BEE && settings.calamityBossesEnabled()) return true;
            if (entity instanceof net.minecraft.world.entity.vehicle.Boat boat
                    && boat.getVariant() != net.minecraft.world.entity.vehicle.Boat.Type.OAK) return false;
        }
        return allows(entity.getType(), settings);
    }

    static boolean wasStandaloneBoss(Entity entity, WorldExtraSettings settings) {
        if (settings.extras()) return false;
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (mine(id)) {
            return settings.rebuiltBosses() && REBUILT_BOSS_ENTITIES.contains(id.getPath())
                || settings.calamityBosses() && CALAMITY_ENTITIES.contains(id.getPath());
        }
        if (!settings.rebuiltBosses()) return false;
        return entity instanceof SourceGiantZombieBoss giant && giant.ritual()
            || entity instanceof SourceElderGuardianBoss elder && elder.ritual()
            || entity instanceof SourceWitherBoss wither && wither.hasInactiveRitual()
            || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon
                && entity.level() instanceof net.minecraft.server.level.ServerLevel server
                && DragonRitual.storedRitualDragon(server, dragon);
    }

    private static boolean vanillaBossSupply(ResourceLocation id, WorldExtraSettings settings) {
        return id != null && id.getNamespace().equals("minecraft")
            && (settings.rebuiltBossesEnabled() && MONUMENT_ITEMS.contains(id.getPath())
                || settings.calamityBossesEnabled() && HONEY_ITEMS.contains(id.getPath()));
    }

    public static boolean item(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return modernItem(stack, id) || rebuiltItem(stack, id);
    }

    private static boolean modernItem(ItemStack stack, ResourceLocation id) {
        return ClassicModernContent.item(stack) || id != null && "minecraft".equals(id.getNamespace())
            && MODERN_COOKING_ITEMS.contains(id.getPath());
    }

    private static boolean rebuiltItem(ItemStack stack, ResourceLocation id) {
        if (stack.getItem() instanceof PotionItem && AdvancedFireResistance.isAdvanced(stack)) return true;
        if (stack.getItem() instanceof BlockItem block && block.getBlock() instanceof BiomePortalStone.Marker) return true;
        if (!mine(id)) return false;
        String name = id.getPath();
        return ITEMS.contains(name) || REBUILT_BOSS_ITEMS.contains(name) || CALAMITY_ITEMS.contains(name)
            || name.startsWith("holy_steel_") || FruitForaging.addedFruit(name)
            || name.startsWith("tin_") || name.startsWith("bronze_") || name.startsWith("steel_") || name.startsWith("netherite_")
            || (stack.getItem() instanceof SpawnEggItem && name.endsWith("_spawn_egg")
                && entity(((SpawnEggItem) stack.getItem()).getType(stack.getTag())))
            || FoodPreservation1905.addedFood(name);
    }

    static boolean entity(EntityType<?> type) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return ClassicModernContent.entity(type) || mine(id) && (ENTITIES.contains(id.getPath())
            || REBUILT_BOSS_ENTITIES.contains(id.getPath()) || CALAMITY_ENTITIES.contains(id.getPath()));
    }

    static boolean allows(Recipe<?> recipe, RegistryAccess registries, WorldExtraSettings settings) {
        var id = recipe.getId();
        if (settings.classicVisuals() && recipe instanceof net.minecraft.world.item.crafting.TippedArrowRecipe) return false;
        boolean r196Bench = recipe instanceof R196BenchRecipe;
        if (r196Bench && !settings.r196()) return false;
        if (settings.r196() && R196BenchRecipe.originalUnlashedTool(id)) return false;
        var output = recipe.getResultItem(registries);
        if (settings.r196() && recipe.getType() == net.minecraft.world.item.crafting.RecipeType.CRAFTING
            && output.is(Items.CRAFTING_TABLE)) return false;
        if (!allows(output, settings)) return false;
        if (mine(id)) {
            String path = id.getPath();
            if ((path.equals("raider_banner") || path.equals("bandage_refill")) && !settings.rebuiltBossesEnabled()) return false;
            if (RECIPE_IDS.contains(path) && !settings.extras()) return false;
        }
        return (!r196Bench && !mine(id) && !id.getNamespace().equals("minecraft"))
            || recipe.getIngredients().stream().noneMatch(ingredient -> excludedIngredient(ingredient.toJson(), settings));
    }

    static boolean excludedIngredient(JsonElement json, WorldExtraSettings settings) {
        if (json.isJsonArray()) {
            var choices = json.getAsJsonArray();
            if (choices.isEmpty()) return false;
            for (var choice : choices) if (!excludedIngredient(choice, settings)) return false;
            return true;
        }
        if (!json.isJsonObject()) return false;
        var ingredient = json.getAsJsonObject();
        if (ingredient.has("item") && ingredient.get("item").isJsonPrimitive()) {
            var id = ResourceLocation.tryParse(ingredient.get("item").getAsString());
            var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            return item != null && !allows(new ItemStack(item), settings);
        }
        if (ingredient.has("tag") && ingredient.get("tag").isJsonPrimitive()) {
            var id = ResourceLocation.tryParse(ingredient.get("tag").getAsString());
            var tags = id == null ? null : ForgeRegistries.ITEMS.tags();
            if (tags == null) return false;
            var tag = tags.getTag(TagKey.create(Registries.ITEM, id));
            return !tag.isEmpty() && tag.stream().allMatch(item -> !allows(new ItemStack(item), settings));
        }
        return false;
    }

    private static boolean mine(ResourceLocation id) {
        return id != null && id.getNamespace().equals(MiteReforged.ID);
    }

    private RebuiltContent() {}
}
