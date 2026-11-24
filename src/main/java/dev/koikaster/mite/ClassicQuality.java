package dev.koikaster.mite;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * 0.4.16 Original MITE R195/R196 8-tier EnumQuality restoration.
 */
public final class ClassicQuality {
    public static final String NBT_KEY = "quality";
    private static volatile boolean durabilityScalingSuppressed = false;

    public enum Level {
        WRETCHED("wretched", 0.5f),
        POOR("poor", 0.75f),
        AVERAGE("average", 1.0f),
        FINE("fine", 1.5f),
        EXCELLENT("excellent", 2.0f),
        SUPERB("superb", 2.5f),
        MASTERWORK("masterwork", 3.0f),
        LEGENDARY("legendary", 3.5f);

        private final String key;
        private final float durabilityModifier;

        Level(String key, float durabilityModifier) {
            this.key = key;
            this.durabilityModifier = durabilityModifier;
        }

        public String key() { return key; }
        public float durabilityModifier() { return durabilityModifier; }
        public boolean isAverage() { return this == AVERAGE; }
        public boolean isLowerThan(Level other) { return ordinal() < other.ordinal(); }
        public boolean isHigherThan(Level other) { return ordinal() > other.ordinal(); }
        public boolean isAverageOrLower() { return ordinal() <= AVERAGE.ordinal(); }
        public boolean isAverageOrHigher() { return ordinal() >= AVERAGE.ordinal(); }

        public Level nextLower() {
            return ordinal() > 0 ? values()[ordinal() - 1] : WRETCHED;
        }

        public float craftingDifficultyMultiplier() {
            int above = ordinal() - AVERAGE.ordinal();
            float mul = 1.0f;
            for (int i = 0; i < above; i++) mul *= 2.0f;
            return mul;
        }

        public static Level byOrdinal(int idx) {
            var all = values();
            if (idx < 0) return WRETCHED;
            if (idx >= all.length) return LEGENDARY;
            return all[idx];
        }
    }

    public static void setDurabilityScalingSuppressed(boolean suppressed) {
        durabilityScalingSuppressed = suppressed;
    }

    public static boolean isDurabilityScalingSuppressed() {
        return durabilityScalingSuppressed;
    }

    public static boolean eligible(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        if (item instanceof BlockItem || item instanceof FlintAndSteelItem || item instanceof ElytraItem
            || item instanceof SpecialItems.Bandage || item instanceof SourceRunestoneItem
            || item instanceof SourceReferencedBooks.Book) {
            return false;
        }
        return ClassicToolWear.profile(stack) != null
            || OriginalArmorItems.profile(stack) != null
            || (ClassicToolCatalogue.sourceItem(stack) && (item instanceof BowItem || item instanceof FishingRodItem || item instanceof ShieldItem));
    }

    public static String materialOf(ItemStack stack) {
        if (!eligible(stack)) return "";
        var tool = ClassicToolWear.profile(stack);
        if (tool != null) return tool.material();
        var armor = OriginalArmorItems.profile(stack);
        if (armor != null) return armor.material();
        return ClassicToolCatalogue.material(stack);
    }

    public static Level maxQualityForMaterial(String material) {
        if (material == null) return Level.AVERAGE;
        return switch (material) {
            case "rusted_iron" -> Level.POOR;
            case "leather", "wood", "stone", "flint", "obsidian", "quartz", "turtle" -> Level.FINE;
            case "copper", "silver", "bronze", "emerald" -> Level.EXCELLENT;
            case "gold", "diamond" -> Level.SUPERB;
            case "iron", "ancient_metal", "steel" -> Level.MASTERWORK;
            case "mithril", "adamantium", "holy_steel", "netherite", "vibranium" -> Level.LEGENDARY;
            default -> Level.AVERAGE;
        };
    }

    public static Level maxQualityForMaterials(List<String> materials) {
        if (materials == null || materials.isEmpty()) return Level.AVERAGE;
        Level lowest = Level.LEGENDARY;
        Level highest = Level.WRETCHED;
        for (String m : materials) {
            Level q = maxQualityForMaterial(m);
            if (q.isLowerThan(lowest)) lowest = q;
            if (q.isHigherThan(highest)) highest = q;
        }
        return lowest.isLowerThan(Level.AVERAGE) ? lowest : highest;
    }

    public static Level maxQuality(ItemStack stack) {
        if (!eligible(stack)) return null;
        return maxQualityForMaterial(materialOf(stack));
    }

    public static Level defaultQuality(ItemStack stack) {
        Level cap = maxQuality(stack);
        if (cap == null) return null;
        return cap.isLowerThan(Level.AVERAGE) ? cap : Level.AVERAGE;
    }

    public static Level get(ItemStack stack) {
        Level cap = maxQuality(stack);
        if (cap == null) return null;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_KEY, Tag.TAG_ANY_NUMERIC)) {
            Level parsed = Level.byOrdinal(tag.getInt(NBT_KEY));
            return parsed.isHigherThan(cap) ? cap : parsed;
        }
        return defaultQuality(stack);
    }

    public static ItemStack set(ItemStack stack, Level quality) {
        if (stack == null || stack.isEmpty()) return stack;
        Level cap = maxQuality(stack);
        if (cap == null) {
            if (stack.hasTag() && stack.getTag().contains(NBT_KEY)) {
                stack.removeTagKey(NBT_KEY);
            }
            return stack;
        }
        Level chosen = quality == null ? defaultQuality(stack) : quality;
        if (chosen.isHigherThan(cap)) chosen = cap;
        stack.getOrCreateTag().putByte(NBT_KEY, (byte) chosen.ordinal());
        return stack;
    }

    public static boolean sameQuality(ItemStack first, ItemStack second) {
        return get(first) == get(second);
    }

    public static int applyDurabilityModifier(int base, ItemStack stack) {
        if (durabilityScalingSuppressed || base <= 0 || stack == null || stack.isEmpty() || !eligible(stack)) {
            return base;
        }
        Level q = get(stack);
        if (q == null || q == Level.AVERAGE) return base;
        return Math.max(Math.round((float) base * q.durabilityModifier()), 1);
    }

    public static void lowerOnDamage(ItemStack stack) {
        if (!eligible(stack) || SourceHeldConversion1037.Inventory.artifact(stack)) return;
        Level q = get(stack);
        if (q != null && q.isAverageOrHigher()) {
            set(stack, q.nextLower());
        }
    }

    public static void applyMobQuality(ItemStack stack, RandomSource random) {
        if (!eligible(stack) || SourceHeldConversion1037.Inventory.artifact(stack)) return;
        float roll = random.nextFloat();
        Level target = roll < 0.25f ? Level.WRETCHED : roll < 0.75f ? Level.POOR : Level.AVERAGE;
        set(stack, target);
    }

    private ClassicQuality() {}
}
