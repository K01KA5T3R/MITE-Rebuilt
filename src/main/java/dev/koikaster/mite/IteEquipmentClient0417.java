package dev.koikaster.mite;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only tooltip renderer & Forging Table screen for 0.4.17 ITE Forging Grade,
 * Equipment Level/EXP, and weighted Modifiers, silent outside WorldContentMode.ite(level).
 */
@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class IteEquipmentClient0417 {
    private static final ResourceLocation FORGING_GUI = Content.id("textures/gui/container/forging_table.png");

    @Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(IteForgingMenu0417.TYPE.get(), ForgingScreen::new));
        }
    }

    public static final class ForgingScreen extends AbstractContainerScreen<IteForgingMenu0417> {
        public ForgingScreen(IteForgingMenu0417 menu, Inventory inv, Component title) {
            super(menu, inv, title);
        }

        @Override
        protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
            gfx.blit(FORGING_GUI, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
            super.renderLabels(gfx, mouseX, mouseY);
            if (menu.active() && menu.totalTime() > 0) {
                int pct = Math.min(100, (menu.progress() * 100) / menu.totalTime());
                gfx.drawString(font, pct + "%", 128, 38, 0xFFAA00, false);
            }
        }
    }
    @SubscribeEvent
    public static void appendTooltip(ItemTooltipEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !WorldContentMode.ite(mc.level)) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof TieredItem || stack.getItem() instanceof ArmorItem || stack.getItem() instanceof BowItem)) {
            return;
        }
        var tag = stack.getTag();
        if (tag == null) return;
        var lines = event.getToolTip();
        int fg = IteModifierTypes0417.forgingGrade(stack);
        if (fg > 0) {
            lines.add(Component.translatable("miteite.tool.forging.grade").append(String.valueOf(fg)));
            float expPct = IteModifierTypes0417.equipmentExpBounce(stack) * 100.0F;
            lines.add(Component.translatable("miteite.tool.forging.exp").append(String.format("%.0f%%", expPct)));
        }
        if (tag.contains("tool_level")) {
            int lvl = tag.getInt("tool_level");
            int maxLvl = IteModifierTypes0417.maxToolLevel(stack);
            if (lvl >= maxLvl) {
                lines.add(Component.translatable("miteite.tool.modifier.maxlevel", maxLvl));
            } else {
                lines.add(Component.translatable("miteite.tool.modifier.level").append(lvl + "/" + maxLvl));
                int req = IteEquipmentHooks0417.expRequiredForLevel(stack, lvl);
                lines.add(Component.translatable("miteite.tool.modifier.exp")
                    .append(Component.literal(tag.getInt("tool_exp") + "/" + req).withStyle(ChatFormatting.WHITE)));
            }
        }
        if (tag.contains("modifiers", 10) && !tag.getCompound("modifiers").isEmpty()) {
            lines.add(Component.translatable("miteite.tool.modifier.modifiers"));
            if (stack.getItem() instanceof ArmorItem) {
                for (var mod : IteModifierTypes0417.ARMOR_MODIFIERS) {
                    int lvl = mod.level(tag);
                    if (lvl > 0) lines.add(mod.label(lvl));
                }
            } else {
                for (var mod : IteModifierTypes0417.TOOL_MODIFIERS) {
                    int lvl = mod.level(tag);
                    if (lvl > 0) lines.add(mod.label(lvl));
                }
            }
        }
    }

    private IteEquipmentClient0417() {}
}
