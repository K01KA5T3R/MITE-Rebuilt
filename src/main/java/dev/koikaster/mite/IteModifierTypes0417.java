package dev.koikaster.mite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

/**
 * MITE-ITE 2.0.30 Tool/Weapon and Armor upgrade modifiers with weights, caps,
 * mutual-exclusion protection groups, and CompatUtil float->int migration.
 */
public final class IteModifierTypes0417 {
    public record ToolMod(
        String nbtName,
        String unlocalizedName,
        float levelAddition,
        ChatFormatting color,
        int weight,
        int maxLevel,
        Predicate<ItemStack> canApply
    ) {
        public int level(CompoundTag itemTag) {
            if (itemTag == null || !itemTag.contains("modifiers", 10)) return 0;
            var mods = itemTag.getCompound("modifiers");
            if (!mods.contains(nbtName)) return 0;
            var tag = mods.get(nbtName);
            return tag instanceof NumericTag num ? Math.max(0, num.getAsInt()) : 0;
        }

        public float value(CompoundTag itemTag) {
            return levelAddition * level(itemTag);
        }

        public MutableComponent label(int lvl) {
            return Component.translatable("modifier.tool." + unlocalizedName + ".name")
                .append(" " + lvl)
                .withStyle(color);
        }
    }

    public record ArmorMod(
        String nbtName,
        String unlocalizedName,
        float levelAddition,
        ChatFormatting color,
        int weight,
        int maxLevel,
        int exclusiveGroup,
        Predicate<ItemStack> canApply
    ) {
        public int level(CompoundTag itemTag) {
            if (itemTag == null || !itemTag.contains("modifiers", 10)) return 0;
            var mods = itemTag.getCompound("modifiers");
            if (!mods.contains(nbtName)) return 0;
            var tag = mods.get(nbtName);
            return tag instanceof NumericTag num ? Math.max(0, num.getAsInt()) : 0;
        }

        public float value(CompoundTag itemTag) {
            return levelAddition * level(itemTag);
        }

        public MutableComponent label(int lvl) {
            return Component.translatable("modifier.armor." + unlocalizedName + ".name")
                .append(" " + lvl)
                .withStyle(color);
        }
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var profile = ClassicToolWear.profile(stack);
        if (profile != null) {
            return switch (profile.kind()) {
                case SWORD, DAGGER, KNIFE, BATTLEAXE, WARHAMMER, CLUB, CUDGEL -> true;
                default -> false;
            };
        }
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof BowItem;
    }

    private static boolean isPickaxe(ItemStack stack) {
        var profile = ClassicToolWear.profile(stack);
        return profile != null && profile.kind() == LegacyToolWear.Kind.PICKAXE || stack.getItem() instanceof PickaxeItem;
    }

    private static boolean isArmorSlot(ItemStack stack, EquipmentSlot slot) {
        return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot;
    }

    public static final ToolMod EFFICIENCY = new ToolMod("efficiency_modifier", "efficiency", 0.25F, ChatFormatting.DARK_RED, 20, 10, s -> !isWeapon(s));
    public static final ToolMod AQUADYNAMIC = new ToolMod("aquadynamic_modifier", "aquaynamic", 1.25F, ChatFormatting.YELLOW, 10, 5, s -> !isWeapon(s));
    public static final ToolMod TOOL_DURABILITY = new ToolMod("durability_modifier", "durability", 0.1F, ChatFormatting.DARK_PURPLE, 40, 5, s -> true);
    public static final ToolMod DAMAGE = new ToolMod("damage_modifier", "damage", 1.0F, ChatFormatting.WHITE, 20, 10, IteModifierTypes0417::isWeapon);
    public static final ToolMod SLOWDOWN = new ToolMod("slowdown_modifier", "slowdown", 1.0F, ChatFormatting.AQUA, 10, 5, IteModifierTypes0417::isWeapon);
    public static final ToolMod UNNATURAL = new ToolMod("unnatural_modifier", "unnatural", 0.1F, ChatFormatting.GRAY, 6, 5, s -> !isWeapon(s));
    public static final ToolMod DEMON_POWER = new ToolMod("demon_power", "demon", 0.25F, ChatFormatting.RED, 4, 1, IteModifierTypes0417::isWeapon);
    public static final ToolMod GEOLOGY = new ToolMod("geology", "geology", 0.5F, ChatFormatting.GOLD, 2, 1, IteModifierTypes0417::isPickaxe);

    public static final List<ToolMod> TOOL_MODIFIERS = List.of(
        EFFICIENCY, AQUADYNAMIC, TOOL_DURABILITY, DAMAGE, SLOWDOWN, UNNATURAL, DEMON_POWER, GEOLOGY
    );

    // Protection exclusivity group 0..3 -> fire(0), general(1), explosion(2), projectile(3)
    public static final ArmorMod ARMOR_DURABILITY = new ArmorMod("durability_modifier", "durability", 0.1F, ChatFormatting.DARK_PURPLE, 20, 5, -1, s -> true);
    public static final ArmorMod PROJECTILE_PROTECTION = new ArmorMod("projectile_protection_modifier", "projectile_protection", 0.5F, ChatFormatting.DARK_GRAY, 7, 5, 3, s -> noOtherProtection(s, 3));
    public static final ArmorMod EXPLOSION_PROTECTION = new ArmorMod("explosion_protection_modifier", "explosion_protection", 0.5F, ChatFormatting.DARK_RED, 7, 5, 2, s -> noOtherProtection(s, 2));
    public static final ArmorMod FIRE_PROTECTION = new ArmorMod("fire_protection_modifier", "fire_protection", 0.5F, ChatFormatting.GOLD, 7, 5, 0, s -> noOtherProtection(s, 0));
    public static final ArmorMod PROTECTION = new ArmorMod("protection_modifier", "protection", 0.25F, ChatFormatting.WHITE, 5, 5, 1, s -> noOtherProtection(s, 1));
    public static final ArmorMod STEADY = new ArmorMod("steady_modifier", "steady", 0.15F, ChatFormatting.DARK_GRAY, 8, 5, -1, s -> true);
    public static final ArmorMod BLESSED = new ArmorMod("blessed_modifier", "blessed", 1.0F, ChatFormatting.GRAY, 8, 5, -1, s -> true);
    public static final ArmorMod INDOMITABLE = new ArmorMod("indomitable", "indomitable", 0.25F, ChatFormatting.YELLOW, 5, 4, -1, s -> isArmorSlot(s, EquipmentSlot.CHEST));
    public static final ArmorMod IMMUNITY = new ArmorMod("immunity", "immunity", 0.15F, ChatFormatting.GREEN, 3, 4, -1, s -> isArmorSlot(s, EquipmentSlot.HEAD));

    public static final List<ArmorMod> ARMOR_MODIFIERS = List.of(
        ARMOR_DURABILITY, PROJECTILE_PROTECTION, EXPLOSION_PROTECTION, FIRE_PROTECTION,
        PROTECTION, STEADY, BLESSED, INDOMITABLE, IMMUNITY
    );

    public static boolean noOtherProtection(ItemStack stack, int group) {
        if (stack.isEmpty() || !stack.hasTag()) return true;
        var tag = stack.getTag();
        if (group != 0 && FIRE_PROTECTION.level(tag) > 0) return false;
        if (group != 1 && PROTECTION.level(tag) > 0) return false;
        if (group != 2 && EXPLOSION_PROTECTION.level(tag) > 0) return false;
        if (group != 3 && PROJECTILE_PROTECTION.level(tag) > 0) return false;
        return true;
    }

    public static void ensureInitializedNbt(ItemStack stack) {
        if (stack.isEmpty()) return;
        var tag = stack.getOrCreateTag();
        if (!tag.contains("tool_level")) tag.putInt("tool_level", 0);
        if (!tag.contains("tool_exp")) tag.putInt("tool_exp", 0);
        if (!tag.contains("modifiers", 10)) tag.put("modifiers", new CompoundTag());
        var mods = tag.getCompound("modifiers");
        if (stack.getItem() instanceof ArmorItem) {
            for (var mod : ARMOR_MODIFIERS) {
                if (mods.contains(mod.nbtName, 5)) { // TAG_FLOAT legacy float migration
                    float orig = mods.getFloat(mod.nbtName);
                    int converted = mod.levelAddition > 0 ? Math.min(mod.maxLevel, Math.max(0, Math.round(orig / mod.levelAddition))) : 0;
                    mods.putInt(mod.nbtName, converted);
                }
            }
        } else {
            for (var mod : TOOL_MODIFIERS) {
                if (mods.contains(mod.nbtName, 5)) {
                    float orig = mods.getFloat(mod.nbtName);
                    int converted = mod.levelAddition > 0 ? Math.min(mod.maxLevel, Math.max(0, Math.round(orig / mod.levelAddition))) : 0;
                    mods.putInt(mod.nbtName, converted);
                }
            }
        }
    }

    public static List<ToolMod> applicableToolModifiers(ItemStack stack) {
        ensureInitializedNbt(stack);
        var tag = stack.getTag();
        List<ToolMod> out = new ArrayList<>();
        for (var m : TOOL_MODIFIERS) {
            if (m.canApply.test(stack) && m.level(tag) < m.maxLevel) out.add(m);
        }
        return out;
    }

    public static List<ArmorMod> applicableArmorModifiers(ItemStack stack) {
        ensureInitializedNbt(stack);
        var tag = stack.getTag();
        List<ArmorMod> out = new ArrayList<>();
        for (var m : ARMOR_MODIFIERS) {
            if (m.canApply.test(stack) && m.level(tag) < m.maxLevel) out.add(m);
        }
        return out;
    }

    @Nullable
    public static ToolMod rollToolModifier(ItemStack stack, RandomSource rand) {
        List<ToolMod> list = applicableToolModifiers(stack);
        int total = list.stream().mapToInt(ToolMod::weight).sum();
        if (total <= 0) return null;
        int roll = rand.nextInt(total);
        for (var m : list) {
            roll -= m.weight;
            if (roll < 0) return m;
        }
        return null;
    }

    @Nullable
    public static ArmorMod rollArmorModifier(ItemStack stack, RandomSource rand) {
        List<ArmorMod> list = applicableArmorModifiers(stack);
        int total = list.stream().mapToInt(ArmorMod::weight).sum();
        if (total <= 0) return null;
        int roll = rand.nextInt(total);
        for (var m : list) {
            roll -= m.weight;
            if (roll < 0) return m;
        }
        return null;
    }

    public static float equipmentExpBounce(ItemStack stack) {
        int fg = forgingGrade(stack);
        return (0.05F + fg * 0.05F) * fg / 2.0F;
    }

    public static int forgingGrade(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return 0;
        return Math.max(0, stack.getTag().getInt("forging_grade"));
    }

    public static int maxToolLevel(ItemStack stack) {
        int fg = forgingGrade(stack);
        if (stack.getItem() instanceof ArmorItem) {
            var p = OriginalArmorItems.profile(stack);
            int minHarvest = p != null ? Math.max(1, LegacyHarvest.material(p.material())) : 3;
            return minHarvest * 3 + fg;
        }
        return 15 + fg;
    }

    private IteModifierTypes0417() {}
}
