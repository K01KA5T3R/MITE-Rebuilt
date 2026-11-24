package dev.koikaster.mite;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.RegistryObject;

/**
 * MITE-ITE 2.0.30 six custom enchantments (Defence, Crit, Extend, Emergency,
 * Beheading, Conqueror) plus Vibranium level cap extensions and cross-damage
 * enchantment coexistence rules (0.4.17).
 */
public final class IteEnchantments0417 {
    public record Def(String id, int difficulty, int maxLevels, EnchantmentCategory category, EquipmentSlot... slots) {}

    public static final Map<String, RegistryObject<Enchantment>> REGISTERED = new LinkedHashMap<>();

    public static final Def DEFENCE = new Def("defence", 20, 5, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND);
    public static final Def CRIT = new Def("crit", 10, 5, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND);
    public static final Def EXTEND = new Def("extend", 15, 5, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND);
    public static final Def EMERGENCY = new Def("emergency", 15, 5, EnchantmentCategory.ARMOR, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    public static final Def BEHEADING = new Def("beheading", 10, 5, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND);
    public static final Def CONQUEROR = new Def("conqueror", 15, 5, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND);

    static {
        for (Def def : new Def[]{DEFENCE, CRIT, EXTEND, EMERGENCY, BEHEADING, CONQUEROR}) {
            REGISTERED.put(def.id, Content.ENCHANTMENTS.register(def.id, () -> new IteEnchantment(def)));
        }
    }

    public static boolean isVibraniumEquipment(ItemStack stack) {
        return !stack.isEmpty() && "vibranium".equals(IteForgingRecipes0417.materialOf(stack));
    }

    /** Matches ItemUtil#isDamageEnchantmentCompatibleWith: Vibranium weapons allow stacking distinct damage enchantments. */
    public static boolean damageEnchantmentCompatible(Enchantment a, Enchantment b, ItemStack stack) {
        if (a == b) return false;
        if (isVibraniumEquipment(stack)) return true;
        return !(a instanceof DamageEnchantment && b instanceof DamageEnchantment);
    }

    /** Matches Vibranium 7-level offense & 5-level protection/thorns limits from MITE-ITE 2.0.30. */
    public static int maxAllowedLevel(Enchantment enchantment, ItemStack stack) {
        if (enchantment == REGISTERED.get("conqueror").get()) {
            return isVibraniumEquipment(stack) ? 7 : 5;
        }
        if (enchantment instanceof DamageEnchantment) {
            return isVibraniumEquipment(stack) ? 7 : enchantment.getMaxLevel();
        }
        if (enchantment instanceof ProtectionEnchantment || enchantment == Enchantments.THORNS) {
            return isVibraniumEquipment(stack) ? 5 : enchantment.getMaxLevel();
        }
        return enchantment.getMaxLevel();
    }

    public static final class IteEnchantment extends Enchantment {
        public final Def def;

        public IteEnchantment(Def def) {
            super(Rarity.RARE, def.category, def.slots);
            this.def = def;
        }

        @Override
        public int getMaxLevel() {
            return def.maxLevels;
        }

        @Override
        public int getMinCost(int level) {
            return 10 + (level - 1) * def.difficulty;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + def.difficulty;
        }
    }

    public static void init() {
        // Trigger registration during Content bootstrap.
    }

    private IteEnchantments0417() {}
}
