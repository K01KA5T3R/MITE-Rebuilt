package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientDirectionalSorting2113 {
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null || !WorldContentMode.rebuilt(level)) return;
        if (!(event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof ContainerScreen)) return;
        var screen = (AbstractContainerScreen<?>)event.getScreen();
        event.addListener(new DirectionButton(screen, DirectionalSorting2113.ROWS, 30));
        event.addListener(new DirectionButton(screen, DirectionalSorting2113.COLUMNS, 14));
    }

    @SubscribeEvent public static void layout(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof ContainerScreen)) return;
        for (var child : event.getScreen().children()) {
            if (child instanceof DirectionButton button) button.position();
        }
    }

    private static final class DirectionButton extends Button {
        private final AbstractContainerScreen<?> screen;
        private final int offset;
        private final boolean columns;

        private DirectionButton(AbstractContainerScreen<?> screen, int mode, int offset) {
            super(0, 0, 14, 12, Component.translatable(mode == DirectionalSorting2113.COLUMNS
                ? "gui.mite_reforged.sort_columns" : "gui.mite_reforged.sort_rows"), pressed -> {
                var client = Minecraft.getInstance();
                if (client.gameMode != null && client.screen == screen && client.player != null
                    && client.player.containerMenu == screen.getMenu()) {
                    client.gameMode.handleInventoryButtonClick(screen.getMenu().containerId, mode);
                }
            }, DEFAULT_NARRATION);
            this.screen = screen;
            this.offset = offset;
            this.columns = mode == DirectionalSorting2113.COLUMNS;
            position();
        }

        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            int centerX = getX() + getWidth() / 2;
            int centerY = getY() + getHeight() / 2;
            int shadow = (color & 0xff000000) | ((color & 0x00fcfcfc) >> 2);
            for (int offset = -2; offset <= 1; offset += 3) {
                if (columns) {
                    graphics.fill(centerX + offset + 1, centerY - 3, centerX + offset + 2, centerY + 5, shadow);
                    graphics.fill(centerX + offset, centerY - 4, centerX + offset + 1, centerY + 4, color);
                } else {
                    graphics.fill(centerX - 3, centerY + offset + 1, centerX + 5, centerY + offset + 2, shadow);
                    graphics.fill(centerX - 4, centerY + offset, centerX + 4, centerY + offset + 1, color);
                }
            }
        }

        private void position() {
            setX(screen.getGuiLeft() + screen.getXSize() - 6 - offset);
            setY(screen.getGuiTop() + 4);
        }
    }

    private ClientDirectionalSorting2113() {}
}
