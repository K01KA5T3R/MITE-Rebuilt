package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT)
public final class ClientWorldContentMode {
    private static final String PREFIX = "selectWorld.miteExtraSettings";
    private static Connection owner;
    private static WorldExtraSettings received;
    private static ClientLevel appliedLevel;

    public static void addButton(GridLayout.RowHelper rows, CreateWorldScreen screen) {
        rows.addChild(Button.builder(Component.translatable(PREFIX),
            button -> Minecraft.getInstance().setScreen(new ExtraSettingsScreen(screen)))
            .bounds(0, 0, 210, 20).build());
    }

    static void receive(Connection sender, WorldExtraSettings settings) {
        var minecraft = Minecraft.getInstance();
        var listener = minecraft.getConnection();
        if (listener != null && listener.getConnection() != sender) return;
        owner = sender;
        received = settings;
        appliedLevel = null;
        apply(minecraft);
    }

    public static void inheritLevelData(ClientPacketListener listener, ClientLevel.ClientLevelData data) {
        if (received != null && listener.getConnection() == owner) {
            WorldContentMode.choose(data, received);
        }
    }

    private static void apply(Minecraft minecraft) {
        var target = minecraft.level;
        var listener = minecraft.getConnection();
        if (owner == null || target == null || target == appliedLevel || listener == null
                || listener.getConnection() != owner) return;
        WorldContentMode.receive(target, received);
        ClassicQuality.setDurabilityScalingSuppressed(WorldContentMode.ite(target));
        appliedLevel = target;
    }

    static boolean r196Equipment() {
        var level = Minecraft.getInstance().level;
        return level != null && WorldContentMode.r196(level);
    }

    public static boolean modernVisuals() {
        var minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.level != null && !WorldContentMode.settings(minecraft.level).classicVisuals();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) apply(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        owner = null;
        received = null;
        appliedLevel = null;
        IteConfig.clearClient();
        ClassicQuality.setDurabilityScalingSuppressed(false);
    }

    private static final class ExtraSettingsScreen extends Screen {
        private final CreateWorldScreen parent;
        private CycleButton<Boolean> modernTextures;
        private CycleButton<Boolean> modernMusic;

        ExtraSettingsScreen(CreateWorldScreen parent) {
            super(Component.translatable(PREFIX));
            this.parent = parent;
        }

        @Override
        protected void init() {
            var choice = WorldContentMode.settings((Object) parent);
            int buttonWidth = Math.min(width - 24, 360);
            int x = (width - buttonWidth) / 2;
            int y = top();
            addRenderableWidget(CycleButton.<Boolean>builder(classic ->
                Component.translatable(PREFIX + (classic ? ".classic" : ".modern")))
                .withValues(true, false).withInitialValue(choice.classicVisuals()).displayOnlyValue()
                .create(x, y, buttonWidth, 20, Component.translatable(PREFIX + ".experience"),
                    (button, classic) -> {
                        WorldContentMode.choose(parent, current().withVisuals(classic));
                        refreshModernOptions();
                    }));
            addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.extras())
                .create(x, y + 24, buttonWidth, 20, Component.translatable(PREFIX + ".content"),
                    (button, on) -> WorldContentMode.choose(parent, current().withExtras(on))));
            addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.rebuiltBosses())
                .create(x, y + 48, buttonWidth, 20, Component.translatable(PREFIX + ".rebuiltBosses"),
                    (button, on) -> WorldContentMode.choose(parent, current().withRebuiltBosses(on))));
            addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.calamityBosses())
                .create(x, y + 72, buttonWidth, 20, Component.translatable(PREFIX + ".calamityBosses"),
                    (button, on) -> WorldContentMode.choose(parent, current().withCalamityBosses(on))));
            addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.ite())
                .create(x, y + 96, buttonWidth, 20, Component.translatable(PREFIX + ".ite"),
                    (button, on) -> WorldContentMode.choose(parent, current().withIte(on))));
            addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.r196())
                .create(x, y + 120, buttonWidth, 20, Component.translatable(PREFIX + ".r196"),
                    (button, on) -> WorldContentMode.choose(parent, current().withR196(on))));
            modernTextures = addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.modernTexturesEnabled())
                .create(x, y + 144, buttonWidth, 20, Component.translatable(PREFIX + ".modernTextures"),
                    (button, enabled) -> WorldContentMode.choose(parent, current().withModernTextures(enabled))));
            modernMusic = addRenderableWidget(CycleButton.onOffBuilder().withInitialValue(choice.modernMusicEnabled())
                .create(x, y + 168, buttonWidth, 20, Component.translatable(PREFIX + ".modernMusic"),
                    (button, enabled) -> WorldContentMode.choose(parent, current().withModernMusic(enabled))));
            refreshModernOptions();
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((width - 150) / 2, y + 200, 150, 20).build());
        }

        private void refreshModernOptions() {
            var choice = current();
            boolean modern = !choice.classicVisuals();
            modernTextures.active = modern;
            modernMusic.active = modern;
            modernTextures.setValue(choice.modernTexturesEnabled());
            modernMusic.setValue(choice.modernMusicEnabled());
        }

        private int top() {
            return (height - 204) / 2;
        }

        private WorldExtraSettings current() {
            return WorldContentMode.settings((Object) parent);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            graphics.drawCenteredString(font, title, width / 2, top() - 16, 0xffffff);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            if (minecraft != null) minecraft.setScreen(parent);
        }
    }

    private ClientWorldContentMode() {}
}
