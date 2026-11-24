package dev.koikaster.mite;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MITE-ITE 2.0.30 Equipment EXP leveling, modifier rolling, full-Protection-V mitigation,
 * and Forging/Modifier combat hooks, strictly gated behind WorldContentMode.ite(level) (0.4.17).
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID)
public final class IteEquipmentHooks0417 {
    public static int expRequiredForLevel(ItemStack stack, int level) {
        if (stack.getItem() instanceof ArmorItem armor) {
            int base = 32 * (level + 1);
            return switch (armor.getEquipmentSlot()) {
                case FEET -> 2 * base;
                case LEGS -> 4 * base;
                case CHEST -> 3 * base;
                default -> base;
            };
        }
        boolean weapon = IteModifierTypes0417.isWeapon(stack);
        return (16 + level * 16) * (weapon ? 2 : 1);
    }

    /** Adds raw experience to a tool/weapon or armor piece, applying forging-grade bounce & rolling modifiers on level-up. */
    public static int addToolExp(ItemStack stack, Player player, int rawExp) {
        if (stack.isEmpty() || rawExp <= 0 || player == null || !WorldContentMode.ite(player.level())) return 0;
        IteModifierTypes0417.ensureInitializedNbt(stack);
        var tag = stack.getOrCreateTag();
        float bounce = IteModifierTypes0417.equipmentExpBounce(stack);
        int gained = Math.max(1, Math.round(rawExp * (1.0F + bounce))) * (stack.getItem() instanceof ArmorItem ? 2 : 1);
        int maxLevel = IteModifierTypes0417.maxToolLevel(stack);
        int curLevel = tag.getInt("tool_level");
        if (curLevel >= maxLevel) return 0;

        int curExp = tag.getInt("tool_exp") + gained;
        int levelsGained = 0;
        RandomSource rand = player.getRandom();
        while (curLevel < maxLevel) {
            int req = expRequiredForLevel(stack, curLevel);
            if (curExp < req) break;
            curExp -= req;
            curLevel++;
            levelsGained++;
            grantRandomModifier(stack, rand, player);
        }
        if (curLevel >= maxLevel) curExp = 0;
        tag.putInt("tool_level", curLevel);
        tag.putInt("tool_exp", curExp);
        return levelsGained;
    }

    private static void grantRandomModifier(ItemStack stack, RandomSource rand, Player player) {
        var mods = stack.getOrCreateTag().getCompound("modifiers");
        if (stack.getItem() instanceof ArmorItem) {
            var chosen = IteModifierTypes0417.rollArmorModifier(stack, rand);
            if (chosen != null) {
                int next = chosen.level(stack.getTag()) + 1;
                mods.putInt(chosen.nbtName(), next);
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("miteite.msg.modifier.upgrade",
                        stack.getHoverName(), stack.getTag().getInt("tool_level")), false);
                }
            }
        } else {
            var chosen = IteModifierTypes0417.rollToolModifier(stack, rand);
            if (chosen != null) {
                int next = chosen.level(stack.getTag()) + 1;
                mods.putInt(chosen.nbtName(), next);
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("miteite.msg.modifier.upgrade",
                        stack.getHoverName(), stack.getTag().getInt("tool_level")), false);
                }
            }
        }
    }

    /** Full Protection V Vibranium defense fraction (defaults to 0.1 = 10% flat reduction when full set has Protection >= 5). */
    public static float fullProtectionVDefenceFraction(Player player) {
        if (player == null || !WorldContentMode.ite(player.level())) return 0.0F;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.isEmpty() || !IteEnchantments0417.isVibraniumEquipment(piece)) return 0.0F;
            if (EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, piece) < 5) {
                return 0.0F;
            }
        }
        return 0.1F;
    }

    public static float indomitableDamageMultiplier(Player player) {
        if (player == null || !WorldContentMode.ite(player.level())) return 1.0F;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !chest.hasTag() || IteModifierTypes0417.INDOMITABLE.value(chest.getTag()) <= 0.0F) return 1.0F;
        float hpPct = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (hpPct <= 0.1F) return 2.0F;
        if (hpPct <= 0.2F) return 1.6F;
        if (hpPct <= 0.35F) return 1.35F;
        if (hpPct <= 0.5F) return 1.25F;
        return 1.0F;
    }

    public static float knockbackFactor(Player player) {
        if (player == null || !WorldContentMode.ite(player.level())) return 1.0F;
        float factor = 1.0F;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!piece.isEmpty() && piece.hasTag()) {
                factor *= Math.max(0.0F, 1.0F - IteModifierTypes0417.STEADY.value(piece.getTag()));
            }
        }
        return factor;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide || !WorldContentMode.ite(player.level())) return;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof DiggerItem) {
            addToolExp(held, player, 1);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || !WorldContentMode.ite(event.getEntity().level()) || event.getSource().is(DamageTypes.THORNS)) return;
        // Offense modifiers on Player main-hand weapon + Indomitable low-HP amp + Demon Power outside Overworld
        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (!weapon.isEmpty() && weapon.hasTag()) {
                var tag = weapon.getTag();
                float amount = event.getAmount() + IteModifierTypes0417.DAMAGE.value(tag);
                int crit = EnchantmentHelper.getItemEnchantmentLevel(IteEnchantments0417.REGISTERED.get("crit").get(), weapon);
                if (crit > 0 && attacker.getRandom().nextFloat() < crit * 0.08F) {
                    amount += crit * 1.5F;
                }
                int conq = EnchantmentHelper.getItemEnchantmentLevel(IteEnchantments0417.REGISTERED.get("conqueror").get(), weapon);
                if (conq > 0) {
                    amount += conq * 1.5F;
                }
                amount *= indomitableDamageMultiplier(attacker);
                if (attacker.level().dimension() != net.minecraft.world.level.Level.OVERWORLD
                    && IteModifierTypes0417.DEMON_POWER.level(tag) > 0) {
                    amount *= 1.0F + IteModifierTypes0417.DEMON_POWER.value(tag);
                }
                event.setAmount(Float.isFinite(amount) ? Math.max(0.0F, Math.min(Float.MAX_VALUE * 0.25F, amount)) : 0.0F);
                addToolExp(weapon, attacker, 2);
            }
        }
        // Full Prot-V fraction on hurt player
        if (event.getEntity() instanceof Player defender) {
            float fraction = fullProtectionVDefenceFraction(defender);
            if (fraction > 0.0F) {
                event.setAmount(Math.max(0.0F, event.getAmount() * (1.0F - fraction)));
            }
        }
    }

    private IteEquipmentHooks0417() {}
}
