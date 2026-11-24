package dev.koikaster.mite;

import java.util.regex.Pattern;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ClientTournamentReconnect extends Screen {
    private static final String RECONNECT = "message.mite_reforged.tournament.reconnect_wait";
    private static final String SHARE = "message.mite_reforged.tournament.share_wait";
    private static final Pattern CLOCK = Pattern.compile("([0-9]{1,2}):([0-5][0-9]):([0-5][0-9])");

    private final Screen parent;
    private final Component originalReason;
    private final Component buttonText;
    private final String key;
    private final long deadline;
    private GridLayout layout;
    private MultiLineTextWidget message;
    private int displayedSeconds;

    private ClientTournamentReconnect(Screen parent, Component title, Component originalReason,
                                      Component buttonText, String key, int seconds) {
        super(title);
        this.parent = parent;
        this.originalReason = originalReason;
        this.buttonText = buttonText;
        this.key = key;
        deadline = Util.getMillis() + seconds * 1000L;
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof DisconnectedScreen screen)
                || screen.getClass() != DisconnectedScreen.class) return;
        Component reason = ObfuscationReflectionHelper.getPrivateValue(DisconnectedScreen.class, screen, "f_95988_");
        if (!(reason.getContents() instanceof TranslatableContents translation)) return;
        String key = translation.getKey();
        if (!RECONNECT.equals(key) && !SHARE.equals(key)) return;
        if (translation.getArgs().length != 1 || !(translation.getArgs()[0] instanceof String text)) return;
        var clock = CLOCK.matcher(text);
        if (!clock.matches()) return;
        int seconds = Integer.parseInt(clock.group(1)) * 3600 + Integer.parseInt(clock.group(2)) * 60
                + Integer.parseInt(clock.group(3));
        Screen parent = ObfuscationReflectionHelper.getPrivateValue(DisconnectedScreen.class, screen, "f_95990_");
        Component button = ObfuscationReflectionHelper.getPrivateValue(DisconnectedScreen.class, screen, "f_278396_");
        event.setNewScreen(new ClientTournamentReconnect(parent, screen.getTitle(), reason, button, key, seconds));
    }

    @Override
    protected void init() {
        layout = new GridLayout();
        layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
        var rows = layout.createRowHelper(1);
        rows.addChild(new StringWidget(title, font));
        displayedSeconds = secondsRemaining();
        message = rows.addChild(new MultiLineTextWidget(countdown(displayedSeconds), font)
                .setMaxWidth(Math.max(1, width - 50)).setCentered(true));
        if (minecraft.allowsMultiplayer()) {
            rows.addChild(Button.builder(buttonText, b -> minecraft.setScreen(parent)).build());
        } else {
            rows.addChild(Button.builder(Component.translatable("gui.toTitle"),
                    b -> minecraft.setScreen(new TitleScreen())).build());
        }
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        message.setMaxWidth(Math.max(1, width - 50));
        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    @Override
    public void tick() {
        int remaining = secondsRemaining();
        if (remaining == displayedSeconds) return;
        displayedSeconds = remaining;
        message.setMessage(countdown(remaining));
        repositionElements();
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(title, countdown(secondsRemaining()));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int secondsRemaining() {
        return (int) ((Math.max(0L, deadline - Util.getMillis()) + 999L) / 1000L);
    }

    private Component countdown(int seconds) {
        return Component.translatable(key, OriginalTournamentReconnect.format(seconds * 20))
                .withStyle(originalReason.getStyle());
    }
}

