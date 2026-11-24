package dev.koikaster.mite;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 0.4.16 Client-side original ye ItemQuality & Shift durability modifier tooltip renderer.
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ClientQualityTooltip0416 {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ClassicQuality.Level quality = ClassicQuality.get(stack);
        if (quality == null) return;

        var lines = event.getToolTip();
        int insertAt = lines.isEmpty() ? 0 : 1;
        lines.add(insertAt++, Component.translatable("quality." + quality.key()).withStyle(ChatFormatting.GRAY));

        boolean detailed = Screen.hasShiftDown() || event.getFlags().isAdvanced();
        if (detailed && !quality.isAverage()) {
            float mod = quality.durabilityModifier();
            if (mod < 1.0f) {
                int pct = Math.round((1.0f - mod) * 100.0f);
                lines.add(insertAt, Component.translatable("item.tooltip.durabilityPenalty", pct).withStyle(ChatFormatting.RED));
            } else if (mod > 1.0f) {
                int pct = Math.round((mod - 1.0f) * 100.0f);
                lines.add(insertAt, Component.translatable("item.tooltip.durabilityBonus", pct).withStyle(ChatFormatting.GREEN));
            }
        }
    }

    private ClientQualityTooltip0416() {}
}
