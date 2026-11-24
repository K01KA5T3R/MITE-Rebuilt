package dev.koikaster.mite;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ClientModernAdvancement548 {
    public static boolean showModernProgress() {
        var client = Minecraft.getInstance();
        if (client.level == null || client.getConnection() == null) return false;
        var settings = WorldContentMode.settings(client.level);
        return !settings.classicVisuals() && !settings.extras();
    }

    public static void addMiteAchievementRow(GridLayout.RowHelper rows) {
        if (showModernProgress()) rows.addChild(achievementButton(0, 0, 204), 2);
    }

    public static final class Setup {
        public static KeyMapping[] shownKeyMappings(Options options, KeyMapping[] originals) {
            if (Minecraft.getInstance().level == null || showModernProgress()) return originals;
            return Arrays.stream(originals).filter(mapping -> mapping != options.keyAdvancements).toArray(KeyMapping[]::new);
        }

        private Setup() {}
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof AdvancementsScreen && !showModernProgress()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void initialized(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen) || showModernProgress()) return;
        for (var listener : List.copyOf(event.getListenersList())) {
            if (listener instanceof AbstractWidget widget && widget.getMessage().getContents() instanceof TranslatableContents label
                    && label.getKey().equals("gui.advancements")) {
                event.removeListener(listener);
                event.addListener(achievementButton(widget.getX(), widget.getY(), widget.getWidth()));
                return;
            }
        }
    }

    private static ClassicTitleButton achievementButton(int x, int y, int width) {
        return new ClassicTitleButton(x, y, width, Component.translatable("gui.mite_reforged.achievements"),
            button -> Minecraft.getInstance().setScreen(new SourceAchievementScreen1500()), false);
    }

    private ClientModernAdvancement548() {}
}

