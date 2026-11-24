package dev.koikaster.mite;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public final class AdvancedFireResistance implements IBrewingRecipe {
    private static final String NAME = "advanced_fire_resistance";
    private static final int REGULAR_DURATION = 3600;
    private static final int LONG_DURATION = 9600;
    private static final int AMPLIFIER = 1;
    private static final net.minecraftforge.registries.RegistryObject<Potion> STANDARD =
        SourceWitchContent.POTIONS.register(NAME, () -> potion(REGULAR_DURATION));
    private static final net.minecraftforge.registries.RegistryObject<Potion> EXTENDED =
        SourceWitchContent.POTIONS.register("long_" + NAME, () -> potion(LONG_DURATION));

    private static Potion potion(int duration) {
        return new Potion(NAME, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, AMPLIFIER));
    }

    static void register() {}

    static void registerRecipe() {
        BrewingRecipeRegistry.addRecipe(new AdvancedFireResistance());
    }

    static boolean isAdvanced(ItemStack stack) {
        var potion = PotionUtils.getPotion(stack);
        return potion == STANDARD.get() || potion == EXTENDED.get();
    }

    static boolean protectsFromLava(LivingEntity actor) {
        return protectsFromLavaFor(actor, 0);
    }

    static boolean protectsFromLavaFor(LivingEntity actor, int minimumTicks) {
        var effect = actor.getEffect(MobEffects.FIRE_RESISTANCE);
        return effect != null && effect.getAmplifier() >= AMPLIFIER
            && (effect.isInfiniteDuration() || effect.getDuration() >= minimumTicks);
    }

    private static MobEffectInstance baseResistance(ItemStack input) {
        if (!input.is(Items.POTION)) return null;
        var effects = PotionUtils.getMobEffects(input);
        if (effects.size() != 1) return null;
        var effect = effects.get(0);
        return effect.getEffect() == MobEffects.FIRE_RESISTANCE && effect.getAmplifier() == 0 ? effect : null;
    }

    @Override public boolean isInput(ItemStack input) { return baseResistance(input) != null; }
    @Override public boolean isIngredient(ItemStack ingredient) { return ingredient.is(Items.GOLDEN_APPLE); }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isIngredient(ingredient)) return ItemStack.EMPTY;
        var effect = baseResistance(input);
        if (effect == null) return ItemStack.EMPTY;
        return PotionUtils.setPotion(new ItemStack(Items.POTION),
            (effect.getDuration() > REGULAR_DURATION ? EXTENDED : STANDARD).get());
    }

    private AdvancedFireResistance() {}
}
