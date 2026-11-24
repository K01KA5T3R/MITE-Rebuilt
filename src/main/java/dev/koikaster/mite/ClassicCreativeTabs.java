package dev.koikaster.mite;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.CreativeModeTabRegistry;

@OnlyIn(Dist.CLIENT)
public final class ClassicCreativeTabs {
    enum Group {
        BUILDING("buildingBlocks", () -> new ItemStack(Items.BRICKS), "sacred_sandstone", "sacred_sandstone"),
        DECORATION("decorations", () -> new ItemStack(Items.POPPY), "copper_anvil", "copper_anvil"),
        REDSTONE("redstone", () -> new ItemStack(Items.REDSTONE), "mithril_auto_workbench", "copper_block"),
        TRANSPORT("transportation", () -> new ItemStack(Items.POWERED_RAIL), "forest_obsidian", "mithril_runestone"),
        MISC("misc", () -> new ItemStack(Items.LAVA_BUCKET), "runic_key", "runic_key"),
        FOOD("food", () -> new ItemStack(Items.APPLE), "vegetable_stew", "vegetable_stew"),
        TOOLS("tools", () -> new ItemStack(Items.IRON_AXE), "copper_pickaxe", "copper_pickaxe"),
        COMBAT("combat", () -> new ItemStack(Items.GOLDEN_SWORD), "copper_sword", "copper_sword"),
        BREWING("brewing", () -> PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER),
            "bottle_of_disenchanting", "bottle_of_disenchanting"),
        MATERIALS("materials", () -> new ItemStack(Items.STICK), "mithril_ingot", "mithril_ingot");

        final String key;
        final Supplier<ItemStack> vanillaIcon;
        final String miteIconId;
        final String classicIconId;

        Group(String key, Supplier<ItemStack> vanillaIcon, String miteIconId, String classicIconId) {
            this.key = key;
            this.vanillaIcon = vanillaIcon;
            this.miteIconId = miteIconId;
            this.classicIconId = classicIconId;
        }
    }

    private static final List<ViewTab> VANILLA = views(false, false);
    private static final List<ViewTab> MITE = views(true, false);
    private static final List<ViewTab> CLASSIC_MITE = views(true, true);
    private static final Map<String, Group> ORIGINAL = originalGroups();
    private static final Set<String> MITE_COMBAT = Set.of("stinger_boomerang", "wood_club", "wood_cudgel", "brick_weapon");
    private static final Set<String> MITE_TOOLS = Set.of("measuring_rule", "air_bladder", "fishing_hook");
    private static final Set<String> MITE_DECORATION = Set.of("advanced_enchanting_table", "mithril_auto_workbench", "shared_plate");
    private static List<CreativeModeTab> shown = List.of();

    private static final class ViewTab extends CreativeModeTab {
        private Set<ItemStack> stacks = ItemStackLinkedSet.createTypeAndTagSet();

        ViewTab(Builder builder) {
            super(builder);
        }

        @Override public boolean shouldDisplay() { return true; }
        @Override public boolean hasAnyItems() { return !stacks.isEmpty(); }
        @Override public Collection<ItemStack> getDisplayItems() { return stacks; }
        @Override public Collection<ItemStack> getSearchTabDisplayItems() { return stacks; }
        @Override public boolean contains(ItemStack stack) { return stacks.contains(stack); }
    }

    private static List<ViewTab> views(boolean mite, boolean classic) {
        var tabs = new ArrayList<ViewTab>();
        for (Group group : Group.values()) {
            var tab = CreativeModeTab.builder(group.ordinal() < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM, group.ordinal() % 5)
                .title(Component.translatable("itemGroup.mite_reforged.classic." + group.key))
                .icon(mite ? () -> {
                    var id = Content.id(classic ? group.classicIconId : group.miteIconId);
                    return BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElseGet(group.vanillaIcon);
                } : group.vanillaIcon)
                .withTabFactory(ViewTab::new)
                .build();
            tabs.add((ViewTab)tab);
        }
        return List.copyOf(tabs);
    }

    private static Map<String, Group> originalGroups() {
        try (var stream = ClassicCreativeTabs.class.getResourceAsStream("/assets/mite_reforged/classic_creative_tabs.json")) {
            if (stream == null) throw new IOException("Missing 1.6.4 creative categories");
            var data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var mapped = new HashMap<String, Group>();
            for (Group group : Group.values()) {
                for (var id : data.getAsJsonArray(group.key)) {
                    if (mapped.put(id.getAsString(), group) != null) throw new IOException("Duplicate legacy item " + id);
                }
            }
            return Map.copyOf(mapped);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read 1.6.4 creative categories", error);
        }
    }

    /** Replaces the category list consumed by Forge's ten-tabs-per-page screen layout. */
    public static List<CreativeModeTab> pages() {
        rebuild();
        var ordered = new ArrayList<CreativeModeTab>(VANILLA);
        ordered.addAll(miteTabs());
        for (var tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && !id.getNamespace().equals("minecraft") && tab != Content.TAB.get()) ordered.add(tab);
        }
        shown = List.copyOf(ordered);
        return shown;
    }

    public static List<CreativeModeTab> refreshedTabs() {
        rebuild();
        var tabs = new ArrayList<>(CreativeModeTabs.allTabs());
        tabs.addAll(VANILLA);
        tabs.addAll(miteTabs());
        return tabs;
    }

    public static List<CreativeModeTab> shownTabs() { return shown; }

    public static CreativeModeTab defaultTab() { return VANILLA.get(0); }

    public static List<CreativeModeTab> defaults() {
        return List.of(BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.SEARCH),
            BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INVENTORY));
    }

    public static boolean emptyAfterRefresh(Collection<ItemStack> stacks, CreativeModeTab tab) {
        return !(tab instanceof ViewTab) && stacks.isEmpty();
    }

    private static void rebuild() {
        var level = Minecraft.getInstance().level;
        var settings = level == null ? WorldExtraSettings.DEFAULT : WorldContentMode.settings(level);
        boolean rebuilt = settings.extras();
        var nativeGroups = buckets();
        var miteGroups = buckets();
        var seen = ItemStackLinkedSet.createTypeAndTagSet();
        for (var tab : CreativeModeTabs.allTabs()) {
            var id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id == null || !id.getNamespace().equals("minecraft") || tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            var assigned = nativeFallback(id.getPath());
            for (ItemStack stack : tab.getDisplayItems()) {
                if (!RebuiltContent.allows(stack, settings)) continue;
                var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (item.getNamespace().equals("minecraft") && seen.add(stack))
                    nativeGroups.get(vanillaGroup(stack, item, assigned)).add(stack);
            }
        }
        for (ItemStack stack : Content.TAB.get().getDisplayItems()) {
            if (!RebuiltContent.allows(stack, settings)) continue;
            var item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (item.getNamespace().equals(MiteReforged.ID)) miteGroups.get(miteGroup(stack, item.getPath())).add(stack);
        }
        for (Group group : Group.values()) {
            VANILLA.get(group.ordinal()).stacks = nativeGroups.get(group);
            (rebuilt ? MITE : CLASSIC_MITE).get(group.ordinal()).stacks = miteGroups.get(group);
        }
    }

    private static List<ViewTab> miteTabs() {
        var level = Minecraft.getInstance().level;
        return level == null || WorldContentMode.rebuilt(level) ? MITE : CLASSIC_MITE;
    }

    private static EnumMap<Group, Set<ItemStack>> buckets() {
        var groups = new EnumMap<Group, Set<ItemStack>>(Group.class);
        for (Group group : Group.values()) groups.put(group, ItemStackLinkedSet.createTypeAndTagSet());
        return groups;
    }

    private static Group vanillaGroup(ItemStack stack, ResourceLocation item, Group fallback) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            var enchantments = EnchantmentHelper.getEnchantments(stack).keySet();
            boolean tool = enchantments.stream().anyMatch(enchantment -> enchantment.canEnchant(new ItemStack(Items.IRON_PICKAXE))
                || enchantment.canEnchant(new ItemStack(Items.FISHING_ROD)));
            return tool ? Group.TOOLS : Group.COMBAT;
        }
        var original = ORIGINAL.get(item.getPath());
        return original != null ? original : byUse(stack, item.getPath(), fallback);
    }

    private static Group nativeFallback(String tab) {
        return switch (tab) {
            case "building_blocks", "colored_blocks", "natural_blocks" -> Group.BUILDING;
            case "functional_blocks" -> Group.DECORATION;
            case "redstone_blocks" -> Group.REDSTONE;
            case "food_and_drinks" -> Group.FOOD;
            case "tools_and_utilities" -> Group.TOOLS;
            case "combat" -> Group.COMBAT;
            case "ingredients" -> Group.MATERIALS;
            default -> Group.MISC;
        };
    }

    private static Group miteGroup(ItemStack stack, String path) {
        if (MITE_COMBAT.contains(path) || ends(path, "warhammer", "battleaxe", "dagger", "knife", "cudgel", "club")) return Group.COMBAT;
        if (MITE_TOOLS.contains(path) || ends(path, "mattock", "scythe", "hatchet", "shears")) return Group.TOOLS;
        if (stack.getItem() instanceof DisenchantingBottle) return Group.BREWING;
        if (MITE_DECORATION.contains(path) || ends(path, "trophy", "anvil", "oven", "furnace", "workbench", "rack", "bowl")) {
            return stack.getItem().isEdible() ? Group.FOOD : Group.DECORATION;
        }
        if (path.equals("ender_obsidian") || stack.getItem() instanceof SourceRunestoneItem) return Group.TRANSPORT;
        if (ends(path, "ingot", "nugget", "shard", "chip", "powder", "dust", "fragment", "leather", "hide", "stick", "seeds")) return Group.MATERIALS;
        return byUse(stack, path, stack.getItem() instanceof BlockItem ? Group.BUILDING : Group.MISC);
    }

    private static Group byUse(ItemStack stack, String path, Group fallback) {
        var item = stack.getItem();
        if (item instanceof PotionItem || path.equals("brewing_stand")) return Group.BREWING;
        if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ElytraItem) return Group.TRANSPORT;
        if (item instanceof DiggerItem || item instanceof ShearsItem || item instanceof FishingRodItem
                || item instanceof FlintAndSteelItem) return Group.TOOLS;
        if (item instanceof ArmorItem || item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof ArrowItem || item instanceof TridentItem) return Group.COMBAT;
        if (item.isEdible() || path.equals("cake")) return Group.FOOD;
        if (item instanceof SpawnEggItem || item instanceof RecordItem) return Group.MISC;
        if (item instanceof BlockItem placed) {
            var block = placed.getBlock();
            if (block instanceof BaseRailBlock || block instanceof BiomePortalStone.Marker) return Group.TRANSPORT;
            if (block instanceof BushBlock || block instanceof BedBlock || block instanceof FenceBlock || block instanceof FlowerPotBlock
                    || block instanceof SignBlock || block instanceof TorchBlock || block instanceof WallBlock) return Group.DECORATION;
        }
        return fallback;
    }

    private static boolean ends(String path, String... names) {
        for (String name : names) if (path.equals(name) || path.endsWith("_" + name)) return true;
        return false;
    }

    private ClassicCreativeTabs() {}
}
