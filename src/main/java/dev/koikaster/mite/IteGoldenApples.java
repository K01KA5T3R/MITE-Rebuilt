package dev.koikaster.mite;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Official ITE 2.0.30 ItemGoldenAppleTrans integrated with IteConfig and WorldContentMode.ite(level). */
public final class IteGoldenApples {
    private static boolean enabled(ItemStack stack, Player player) {
        return player != null
            && (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))
            && WorldContentMode.ite(player.level());
    }

    public static int duration(Object rawStack, int original, Object rawPlayer) {
        if (!(rawPlayer instanceof Player player)) return original;
        var stack = (ItemStack) rawStack;
        if (!enabled(stack, player)) return original;
        var cfg = IteConfig.effective(player.level());
        return stack.is(Items.ENCHANTED_GOLDEN_APPLE) ? cfg.enchantedGoldenAppleEatTime() : cfg.goldenAppleEatTime();
    }

    public static Diet.Profile profile(Diet.Profile original, ItemStack stack, Player player) {
        if (original == null || !enabled(stack, player)) return original;
        return new Diet.Profile(original.satiation(), original.nutrition(), 1500,
            original.mask(), original.counterpart());
    }

    public static boolean applyEffects(Object rawStack, Object rawLevel, Object rawPlayer) {
        if (!(rawPlayer instanceof Player player)) return false;
        var stack = (ItemStack) rawStack;
        if (!enabled(stack, player)) return false;
        var level = (Level) rawLevel;
        if (level.isClientSide) return true;

        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            add(player, MobEffects.REGENERATION, 1200, 1);
            var absorption = player.getEffect(MobEffects.ABSORPTION);
            if (absorption == null || absorption.getAmplifier() <= 0) {
                add(player, MobEffects.HEAL, 2, 0);
                player.removeEffect(MobEffects.ABSORPTION);
                add(player, MobEffects.ABSORPTION, 400, 2);
            }
            add(player, MobEffects.DAMAGE_RESISTANCE, 1200, 0);
            add(player, MobEffects.FIRE_RESISTANCE, 1200, 0);
        } else {
            add(player, MobEffects.REGENERATION, 1200, 0);
            var absorption = player.getEffect(MobEffects.ABSORPTION);
            if (absorption == null || absorption.getAmplifier() != 0) {
                player.removeEffect(MobEffects.ABSORPTION);
                add(player, MobEffects.ABSORPTION, 400, 0);
            }
            add(player, MobEffects.FIRE_RESISTANCE, 200, 0);
            add(player, MobEffects.DAMAGE_RESISTANCE, 200, 0);
        }
        return true;
    }

    private static void add(Player player, MobEffect effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    private IteGoldenApples() {}
}
