package dev.koikaster.mite;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/** R195 azr presentation using the native WinScreen completion callback. */
public final class ClassicEndCreditsScreen extends WinScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int WIDTH = 274;
    private static final int LINE_HEIGHT = 12;
    private static final float SCROLL_PER_TICK = .5f;
    private static final String OBFUSCATE = "§f§k§a§b";
    private static final ResourceLocation LOGO = ClassicTitleScreen.asset("minecraft.png");
    private static final ResourceLocation DIRT = texture("dirt.png");
    private static final ResourceLocation VIGNETTE = texture("vignette.png");
    private static final Line BLANK = new Line(FormattedCharSequence.EMPTY, false);
    private final boolean poem;
    private List<Line> credits;
    private int ticks;

    public ClassicEndCreditsScreen(boolean poem, Runnable onFinished) {
        super(poem, onFinished);
        this.poem = poem;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(MiteReforged.ID, "textures/gui/classic_credits/" + name);
    }

    boolean usesClassicCredits() {
        return credits != null;
    }

    @Override
    protected void init() {
        if (credits != null) return;
        if (poem) {
            var lines = new ArrayList<Line>();
            var random = new Random(8124371);
            try {
                load(lines, "profound.txt", random, true);
                lines.addAll(Collections.nCopies(8, BLANK));
                load(lines, "end.txt", random, true);
                lines.addAll(Collections.nCopies(8, BLANK));
                load(lines, "credits.txt", random, false);
                credits = List.copyOf(lines);
                return;
            } catch (IOException failure) {
                LOGGER.warn("Could not load the classic End credits", failure);
            }
        }
        super.init();
    }

    private void load(List<Line> lines, String name, Random random, boolean poemText) throws IOException {
        var resource = new ResourceLocation(MiteReforged.ID, "texts/classic_credits/" + name);
        String username = minecraft.getUser().getName();
        try (var reader = minecraft.getResourceManager().openAsReader(resource)) {
            for (String raw; (raw = reader.readLine()) != null;) {
                raw = raw.replace("PLAYERNAME", username);
                if (poemText) {
                    for (int marker; (marker = raw.indexOf(OBFUSCATE)) >= 0;) {
                        raw = raw.substring(0, marker) + "§f§k" + "X".repeat(3 + random.nextInt(4))
                            + raw.substring(marker + OBFUSCATE.length());
                    }
                } else raw = raw.replace("\t", "    ");
                var wrapped = font.getSplitter().splitLines(raw, WIDTH, Style.EMPTY);
                if (wrapped.isEmpty()) lines.add(BLANK);
                for (FormattedText segment : wrapped) {
                    boolean centered = segment.getString().startsWith("[C]");
                    if (centered) {
                        segment = segment.visit((style, text) -> Optional.of(FormattedText.of(text.substring(3), style)),
                            Style.EMPTY).orElse(FormattedText.EMPTY);
                    }
                    lines.add(new Line(Language.getInstance().getVisualOrder(segment), centered));
                }
                lines.add(BLANK);
            }
        }
    }

    private float endTick() {
        return (credits.size() * LINE_HEIGHT + height * 2 + 24) / SCROLL_PER_TICK;
    }

    @Override
    public void tick() {
        if (!usesClassicCredits()) {
            super.tick();
            return;
        }
        minecraft.getSoundManager().tick(false);
        if (++ticks > endTick()) onClose();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!usesClassicCredits()) return super.keyPressed(key, scanCode, modifiers);
        if (key != GLFW.GLFW_KEY_ESCAPE) return false;
        onClose();
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return !usesClassicCredits() && super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public void removed() {
        if (!usesClassicCredits()) super.removed();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!usesClassicCredits()) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        float time = ticks + partialTick;
        float scroll = -time * SCROLL_PER_TICK;
        float fade = Mth.clamp(Math.min(time * .02f, (endTick() - 20 - time) * .005f), 0, 1);
        float brightness = fade * fade * 96 / 255;
        graphics.flush();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(brightness, brightness, brightness, 1);
        quad(graphics, DIRT, width / 64f, (height + scroll / 2) / 64f, scroll / 128f);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, scroll, 0);
        int x = width / 2 - WIDTH / 2;
        int logoY = height + 50;
        graphics.blit(LOGO, x, logoY, 0, 0, 155, 44);
        graphics.blit(LOGO, x + 155, logoY, 0, 45, 155, 44);
        for (int row = 0; row < credits.size(); row++) {
            int y = logoY + 200 + row * LINE_HEIGHT;
            float visibleY = y + scroll;
            if (visibleY + LINE_HEIGHT + 8 <= 0 || visibleY >= height) continue;
            if (row == credits.size() - 1) y += Math.max(0, Mth.ceil(height / 2 - 6 - visibleY));
            var line = credits.get(row);
            int left = line.centered ? x + (WIDTH - font.width(line.text)) / 2 : x;
            graphics.drawString(font, line.text, left, y, 0xffffff, true);
        }
        pose.popPose();
        graphics.flush();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        quad(graphics, VIGNETTE, 1, 0, 1);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void quad(GuiGraphics graphics, ResourceLocation texture, float right, float top, float bottom) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        var matrix = graphics.pose().last().pose();
        var buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, 0, height, 0).uv(0, bottom).endVertex();
        buffer.vertex(matrix, width, height, 0).uv(right, bottom).endVertex();
        buffer.vertex(matrix, width, 0, 0).uv(right, top).endVertex();
        buffer.vertex(matrix, 0, 0, 0).uv(0, top).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private record Line(FormattedCharSequence text, boolean centered) {}
}
