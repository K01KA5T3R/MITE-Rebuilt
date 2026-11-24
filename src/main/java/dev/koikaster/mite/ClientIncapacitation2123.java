package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientIncapacitation2123 {
    private static final int RESCUE_WIDTH = 80;
    private static final Component RESCUING = Component.translatable("gui.mite_reforged.reviving");
    private static int remaining, rescue;
    private static int helperProgress;
    private static LocalPlayer helperPlayer;
    private static ClientLevel helperLevel;

    static void update(int left,int progress) {
        remaining=Math.max(0,Math.min(2000,left));
        rescue=Math.max(0,Math.min(Incapacitation2123.RESCUE_TICKS,progress));
    }

    static void updateHelper(int progress) {
        var client = Minecraft.getInstance();
        helperProgress = Math.max(0, Math.min(Incapacitation2123.RESCUE_TICKS, progress));
        helperPlayer = helperProgress > 0 ? client.player : null;
        helperLevel = helperProgress > 0 ? client.level : null;
    }

    @SubscribeEvent
    public static void renderHelper(RenderGuiEvent.Post event) {
        var client = Minecraft.getInstance();
        if (helperProgress == 0 || client.player == null || client.player != helperPlayer
                || client.level != helperLevel || !client.player.isAlive() || remaining != 0
                || client.options.hideGui || client.screen != null) return;
        var gui = event.getGuiGraphics();
        int center = gui.guiWidth() / 2;
        int top = gui.guiHeight() / 2 + 15;
        gui.drawCenteredString(client.font, RESCUING, center, top, 0xFFFFFF);
        drawProgress(gui, center - RESCUE_WIDTH / 2, top + 11, helperProgress);
    }

    private static void drawProgress(GuiGraphics gui, int left, int top, int progress) {
        gui.fill(left, top, left + RESCUE_WIDTH, top + 3, 0xFF404040);
        int fill = Math.max(1, progress * RESCUE_WIDTH / Incapacitation2123.RESCUE_TICKS);
        gui.fill(left, top, left + fill, top + 3, 0xFF80C448);
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        updateHelper(0);
        update(0,0);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        var client=Minecraft.getInstance();
        if (helperProgress > 0 && (client.player != helperPlayer || client.level != helperLevel)) updateHelper(0);
        if (client.player==null || remaining==0) {
            if (client.screen instanceof DownedScreen) client.setScreen(null);
        } else if (client.screen==null && client.player.isAlive()) client.setScreen(new DownedScreen());
    }

    private static final class DownedScreen extends Screen {
        private DownedScreen() { super(Component.translatable("screen.mite_reforged.downed")); }

        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.translatable("screen.mite_reforged.surrender"), button -> {
                button.active=false;
                Incapacitation2123.requestSurrender();
            }).bounds(width/2-60,height/2+30,120,20).build());
        }

        @Override public void render(GuiGraphics gui,int x,int y,float partialTick) {
            gui.fill(0,0,width,height,0x65000000);
            gui.drawCenteredString(font,title,width/2,height/2-23,0xFFFFFF);
            int seconds=(remaining+19)/20;
            String time=seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;
            gui.drawCenteredString(font,time,width/2,height/2-7,0xFFFFFF);
            if (rescue>0) {
                drawProgress(gui, width / 2 - RESCUE_WIDTH / 2, height / 2 + 14, rescue);
            }
            super.render(gui,x,y,partialTick);
        }

        @Override public boolean isPauseScreen() { return false; }

        @Override public void onClose() { minecraft.setScreen(new PauseScreen(true)); }
    }

    private ClientIncapacitation2123() {}
}
