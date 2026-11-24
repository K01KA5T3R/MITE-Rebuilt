package dev.koikaster.mite;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

/** Produces scorched food from valid cooking recipes above coal heat. */
public final class ScorchedFood2072 extends Item {
    private static final int COAL_HEAT = LegacyFuel.heat(LegacyFuel.Kind.COAL);
    private static final String SATIATION = "Satiation";
    private static final FoodProperties FOOD = new FoodProperties.Builder()
        .nutrition(0).saturationMod(0)
        .effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 1200, 0), 1f).build();

    public static final RegistryObject<Item> ITEM = Content.item("scorched_food", ScorchedFood2072::new);

    private ScorchedFood2072() {
        super(new Item.Properties().stacksTo(16).food(FOOD));
    }

    static void register() {
        // Class initialization adds the item before the common item registry is attached.
    }

    static ClassicFurnaces.Plan atHeat(ItemStack input, int heat, ClassicFurnaces.Plan ordinary) {
        if (heat <= COAL_HEAT || heat < ordinary.requiredHeat() || !ordinary.exists()
            || !input.isEdible() || !ordinary.output().isEdible()) return ordinary;

        int satiation = Mth.clamp(SurvivalFood.energy(input, null).satiation(), 0, 40);
        ItemStack result = new ItemStack(ITEM.get(), ordinary.output().getCount());
        result.getOrCreateTag().putInt(SATIATION, satiation);
        return new ClassicFurnaces.Plan(result, Math.max(COAL_HEAT + 1, ordinary.requiredHeat()),
            ordinary.consumed(), ordinary.xp());
    }

    static Diet.Profile diet(ItemStack stack) {
        var tag = stack.getTag();
        int satiation = tag == null ? 0 : Mth.clamp(tag.getInt(SATIATION), 0, 40);
        return new Diet.Profile(satiation, 0, 0, 0, true);
    }
}
