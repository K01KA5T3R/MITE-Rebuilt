package dev.koikaster.mite;

import java.math.BigInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.registries.RegistryObject;

public final class MeasuringRule2082 extends Item {
    public static final RegistryObject<Item> ITEM = Content.item("measuring_rule", MeasuringRule2082::new);
    private static final String SELECTION = "MiteMeasure";

    private MeasuringRule2082() {
        super(new Item.Properties().stacksTo(1));
    }

    static void register() {
        // Attach to the base item registry before the mod bus registers its entries.
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var point = context.getClickedPos();
        if (player == null || player.isSpectator() || !level.mayInteract(player, point)
            || !player.mayUseItemAt(point, context.getClickedFace(), stack)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        String dimension = level.dimension().location().toString();
        var previous = stack.getTagElement(SELECTION);
        if (previous == null || !previous.contains("Pos", Tag.TAG_LONG)
            || !dimension.equals(previous.getString("Dimension"))
            || !previous.hasUUID("Owner") || !player.getUUID().equals(previous.getUUID("Owner"))) {
            CompoundTag first = new CompoundTag();
            first.putLong("Pos", point.asLong());
            first.putString("Dimension", dimension);
            first.putUUID("Owner", player.getUUID());
            stack.addTagElement(SELECTION, first);
            player.displayClientMessage(Component.translatable("item.mite_reforged.measuring_rule.first",
                point.getX(), point.getY(), point.getZ()), true);
            return InteractionResult.CONSUME;
        }

        var origin = BlockPos.of(previous.getLong("Pos"));
        stack.removeTagKey(SELECTION);
        var result = measure(origin, point);
        player.displayClientMessage(Component.translatable("item.mite_reforged.measuring_rule.result",
            result.length(), result.width(), result.height(), result.volume().toString()), true);
        return InteractionResult.CONSUME;
    }

    record Measurement(long length, long width, long height, BigInteger volume) {}

    static Measurement measure(BlockPos first, BlockPos second) {
        long length = Math.abs((long) first.getX() - second.getX()) + 1;
        long width = Math.abs((long) first.getZ() - second.getZ()) + 1;
        long height = Math.abs((long) first.getY() - second.getY()) + 1;
        BigInteger volume = BigInteger.valueOf(length).multiply(BigInteger.valueOf(width))
            .multiply(BigInteger.valueOf(height));
        return new Measurement(length, width, height, volume);
    }
}
