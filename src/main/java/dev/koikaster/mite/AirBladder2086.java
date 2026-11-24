package dev.koikaster.mite;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

public final class AirBladder2086 extends Item {
    public static final RegistryObject<Item> ITEM = Content.item("leather_air_bladder", AirBladder2086::new);
    private static final int USES = 24;
    private static final int EXTRA_AIR = 160;
    private static final String FILLED = "MiteBladderFilled";

    private AirBladder2086() {
        super(new Item.Properties().durability(USES));
    }

    static void register() {
        // Attach to the base item registry before the mod bus registers its entries.
    }

    private static boolean filled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(FILLED);
    }

    @Override
    public Component getName(ItemStack stack) {
        return filled(stack) ? Component.translatable(getDescriptionId(stack) + ".filled") : super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator()) return InteractionResultHolder.pass(stack);

        boolean submerged = player.isEyeInFluid(FluidTags.WATER);
        if (!submerged) {
            if (!player.isInWater() || filled(stack)) return InteractionResultHolder.pass(stack);
            if (!level.isClientSide) stack.getOrCreateTag().putBoolean(FILLED, true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        int capacity = player.getMaxAirSupply();
        if (OriginalCurseEffects.has(player, 2, false)) capacity = Math.min(capacity, 90);
        int remaining = player.getAirSupply();
        if (!filled(stack) || remaining >= capacity) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            player.setAirSupply(Math.min(capacity, remaining + EXTRA_AIR));
            stack.removeTagKey(FILLED);
            stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
