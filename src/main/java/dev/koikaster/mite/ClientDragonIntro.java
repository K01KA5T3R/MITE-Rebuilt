package dev.koikaster.mite;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MiteReforged.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientDragonIntro {
    private static final Map<ClientLevel, Set<UUID>> CRABULON_INTROS_SEEN = new WeakHashMap<>();
    private static final int FADE_START = 56;
    private static final int FADE_END = 76;
    private static final int EPITHET_START = 14;
    private static final Palette DRAGON_NAME = new Palette(0xBA8AEC, 0xEED0FF, 0x81E6F3, 0xAB56F0, 0x241239, .72f);
    private static final Palette DRAGON_EPITHET = new Palette(0xAA91E1, 0xDDD2FD, 0x81E6F3, 0x6357BA, 0x241239, .30f);
    private static final Palette WITHER_NAME = new Palette(0x9BB789, 0xDFE9C4, 0xDAED6E, 0x708E5D, 0x1B2819, .72f);
    private static final Palette WITHER_EPITHET = new Palette(0x92AC7C, 0xE9F1C9, 0xDAED6E, 0x708E5D, 0x1B2819, .30f);
    private static final Palette GIANT_NAME = new Palette(0x879753, 0xE0D19A, 0xF4BC70, 0x566C41, 0x272714, .64f);
    private static final Palette GIANT_EPITHET = new Palette(0xAF995F, 0xF0DDA8, 0xF4BC70, 0x807049, 0x272714, .30f);
    private static final Palette ELDER_NAME = new Palette(0x78B1A0, 0xD1EEE0, 0xA8F9DC, 0x52A8B0, 0x132A2E, .72f);
    private static final Palette ELDER_EPITHET = new Palette(0x79B9AD, 0xD6F4EE, 0xA8F9DC, 0x52A8B0, 0x132A2E, .30f);
    private static final Palette SLIME_NAME = new Palette(0x8ECD72, 0xDDF5B6, 0xFFCF70, 0x478B50, 0x24381B, .72f);
    private static final Palette SLIME_EPITHET = new Palette(0xA1BA74, 0xE4EBB1, 0xFFCF70, 0x788B46, 0x24381B, .31f);
    private static final Palette CRAB_NAME = new Palette(0x56B9BB, 0xE5EBE7, 0xEAE182, 0x458388, 0x15282F, .72f);
    private static final Palette CRAB_EPITHET = new Palette(0x8DABB8, 0xDEEFE8, 0xF2D99C, 0x47747E, 0x15282F, .31f);
    private static final Palette QUEEN_NAME = new Palette(0xECA841, 0xFFF1A8, 0xFFF2B9, 0xBA681B, 0x402817, .72f);
    private static final Palette QUEEN_EPITHET = new Palette(0xE7B668, 0xFFE7A7, 0xFFF4C2, 0x9B6B26, 0x402817, .31f);
    private static final Palette MYTHRIL_NAME = new Palette(0x85BEDF, 0xE4F6FD, 0xB9F2E6, 0x407EAF, 0x142639, .72f);
    private static final Palette MYTHRIL_EPITHET = new Palette(0x9DC1CC, 0xD8EAF4, 0xACF1DC, 0x4F8595, 0x142639, .31f);
    private static final Palette REDSTONE_NAME = new Palette(0xE98B75, 0xFFE0B7, 0xFFA47C, 0xAB4632, 0x321C19, .72f);
    private static final Palette REDSTONE_EPITHET = new Palette(0xCDA68B, 0xF2D4B1, 0xEF967C, 0x996450, 0x321C19, .31f);
    private static final Palette GIANT_SKELETON_NAME = new Palette(0xD2C19F, 0xF2E9D9, 0xBAD8DA, 0x82715A, 0x332C26, .72f);
    private static final Palette GIANT_SKELETON_EPITHET = new Palette(0xB9AF98, 0xEEE1CB, 0xA2C7CC, 0x73655D, 0x332C26, .31f);
    private static Intro intro;

    private ClientDragonIntro() {}

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.TITLE_TEXT.id(), "dragon_intro", ClientDragonIntro::render);
    }

    static void receive(DragonIntroPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (packet.startedAt() < 0) {
            if (intro != null && intro.dragon.equals(packet.dragon())) intro = null;
            return;
        }
        if (client.level == null || client.level.dimension() != Level.END) return;
        if (intro != null && intro.level.get() == client.level && intro.dragon.equals(packet.dragon())) return;
        intro = new Intro(new WeakReference<>(client.level), packet.dragon(), packet.startedAt(),
            line(client.font, Component.translatable("entity.minecraft.ender_dragon"), false),
            line(client.font, Component.translatable("mite_reforged.dragon_ritual.epithet"), true), DRAGON_NAME, DRAGON_EPITHET);
    }

    static void receiveWither(WitherIntroPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().location().equals(packet.dimension())) return;
        if (intro != null && intro.level.get() == client.level && intro.startedAt > packet.startedAt()) return;
        intro = new Intro(new WeakReference<>(client.level), packet.encounter(), packet.startedAt(),
            line(client.font, Component.translatable("entity.minecraft.wither"), false),
            line(client.font, Component.translatable("mite_reforged.wither_ritual.epithet"), true), WITHER_NAME, WITHER_EPITHET);
    }

    static void receiveGroundSea(GroundSeaBossIntroPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().location().equals(packet.dimension())) return;
        if (packet.kind() == GroundSeaBossIntroPacket.Kind.CRABULON) {
            showCrabulon(client, packet.encounter(), packet.startedAt());
            return;
        }
        if (intro != null && intro.level.get() == client.level && intro.startedAt > packet.startedAt()) return;
        if (packet.kind() == GroundSeaBossIntroPacket.Kind.MYTHRIL_KNIGHT) {
            var name = Component.translatable("entity.mite_reforged.mythril_knight");
            var epithet = Component.translatable("mite_reforged.mythril_knight.epithet");
            intro = new Intro(new WeakReference<>(client.level), packet.encounter(), packet.startedAt(),
                line(client.font, name, false), line(client.font, epithet, true), MYTHRIL_NAME, MYTHRIL_EPITHET);
            return;
        }
        if (packet.kind() == GroundSeaBossIntroPacket.Kind.REDSTONE_GOLEM) {
            intro = new Intro(new WeakReference<>(client.level), packet.encounter(), packet.startedAt(),
                line(client.font, Component.translatable("entity.mite_reforged.redstone_iron_golem"), false),
                line(client.font, Component.translatable("mite_reforged.redstone_iron_golem.epithet"), true),
                REDSTONE_NAME, REDSTONE_EPITHET);
            return;
        }
        if (packet.kind() == GroundSeaBossIntroPacket.Kind.GIANT_SKELETON) {
            intro = new Intro(new WeakReference<>(client.level), packet.encounter(), packet.startedAt(),
                line(client.font, Component.translatable("entity.mite_reforged.underworld_giant_skeleton"), false),
                line(client.font, Component.translatable("mite_reforged.underworld_giant_skeleton.epithet"), true),
                GIANT_SKELETON_NAME, GIANT_SKELETON_EPITHET);
            return;
        }
        boolean sea = packet.kind() == GroundSeaBossIntroPacket.Kind.ELDER;
        var name = Component.translatable(sea ? "entity.minecraft.elder_guardian" : "entity.minecraft.giant");
        var epithet = Component.translatable(sea ? "mite_reforged.elder_ritual.epithet" : "mite_reforged.giant_ritual.epithet");
        intro = new Intro(new WeakReference<>(client.level), packet.encounter(), packet.startedAt(),
            line(client.font, name, false), line(client.font, epithet, true), sea ? ELDER_NAME : GIANT_NAME,
            sea ? ELDER_EPITHET : GIANT_EPITHET);
    }

    static void receiveKingSlime(SourceKingSlime boss) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || boss.level() != client.level
                || !boss.withinIntroRange(client.player.position())) return;
        long started = boss.introStarted();
        if (started < 0 || client.level.getGameTime() - started >= FADE_END) return;
        if (intro != null && intro.level.get() == client.level
                && (intro.dragon.equals(boss.getUUID()) || intro.startedAt > started)) return;
        intro = new Intro(new WeakReference<>(client.level), boss.getUUID(), started,
            line(client.font, Component.translatable("entity.mite_reforged.king_slime"), false),
            line(client.font, Component.translatable("mite_reforged.king_slime.epithet"), true), SLIME_NAME, SLIME_EPITHET);
    }

    static void receiveCrabulon(SourceCrabulon boss) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || boss.level() != client.level
                || !boss.withinArena(client.player.position())) return;
        if (boss.actionPose() == CrabulonCombat.Action.INTRO) showCrabulon(client, boss.getUUID(), boss.entranceAge());
    }

    private static void showCrabulon(Minecraft client, UUID encounter, long elapsed) {
        if (client.level == null || client.player == null || elapsed < 0 || elapsed >= FADE_END) return;
        if (!CRABULON_INTROS_SEEN.computeIfAbsent(client.level, level -> new HashSet<>()).add(encounter)) return;
        long localStart = client.gui.getGuiTicks() - elapsed;
        intro = new Intro(new WeakReference<>(client.level), encounter, localStart,
            line(client.font, Component.translatable("entity.mite_reforged.crabulon"), false),
            line(client.font, Component.translatable("mite_reforged.crabulon.epithet"), true), CRAB_NAME, CRAB_EPITHET);
    }

    static void receiveQueenBee(SourceQueenBee boss) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || boss.level() != client.level
                || !boss.withinArena(client.player.position())) return;
        long started = boss.introStarted();
        if (started < 0 || client.level.getGameTime() - started >= FADE_END) return;
        if (intro != null && intro.level.get() == client.level
                && (intro.dragon.equals(boss.getUUID()) || intro.startedAt > started)) return;
        intro = new Intro(new WeakReference<>(client.level), boss.getUUID(), started,
            line(client.font, Component.translatable("entity.mite_reforged.queen_bee"), false),
            line(client.font, Component.translatable("mite_reforged.queen_bee.epithet"), true), QUEEN_NAME, QUEEN_EPITHET);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partial, int width, int height) {
        if (intro == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || intro.level.get() != client.level) {
            intro = null;
            return;
        }
        long now = intro.palette == CRAB_NAME ? gui.getGuiTicks() : client.level.getGameTime();
        float age = now - intro.startedAt + partial;
        if (age >= FADE_END) {
            intro = null;
            return;
        }
        if (age <= 0 || client.options.hideGui || client.player == null) return;

        gui.setupOverlayRenderState(true, false);
        float fade = 1 - ease((age - FADE_START) / (FADE_END - FADE_START));
        float originalScale = Mth.clamp(height / 114f, .85f, 4.6f);
        float nameScale = fit(intro.name, originalScale * 2, width, .82f);
        float epithetScale = fit(intro.epithet, Mth.clamp(originalScale * .52f, .85f, 4.6f) * 2, width, .75f);
        float lineGap = (client.font.lineHeight + 1.4f) * nameScale;
        float groupHeight = lineGap + client.font.lineHeight * epithetScale;
        float nameY = height * .335f - groupHeight * .5f;
        drawLine(graphics, client.font, intro.name, width, nameY, nameScale, age, fade, false, intro.palette);
        if (age > EPITHET_START) {
            drawLine(graphics, client.font, intro.epithet, width, nameY + lineGap, epithetScale, age, fade, true, intro.epithetPalette);
        }
        graphics.flush();
    }

    private static float fit(Line line, float desired, int width, float fraction) {
        return Math.min(desired, width * fraction / Math.max(line.width + 2, 1));
    }

    private static void drawLine(GuiGraphics graphics, Font font, Line line, int width, float y, float scale,
                                 float age, float fade, boolean epithet, Palette palette) {
        float revealAge = age - (epithet ? EPITHET_START : 0);
        float reveal = Mth.clamp(revealAge / (epithet ? 10 : 12), 0, 1);
        int visible = Mth.ceil(reveal * line.glyphs.size());
        int alpha = (int) (255 * ease(revealAge / 7) * fade);
        // Vanilla treats colors with alpha below four as opaque.
        if (alpha < 4 || visible == 0) return;

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(width * .5f, y, 0);
        pose.scale(scale, scale, 1);
        var matrix = pose.last().pose();
        var buffers = graphics.bufferSource();
        int ghostAlpha = (int) (alpha * .22f * (1 - .55f * ease(revealAge / 38)));
        for (int i = 0; i < visible; i++) {
            Glyph glyph = line.glyphs.get(i);
            float x = glyph.offset - line.width * .5f;
            float across = glyph.offset / Math.max(line.width, 1);
            int color = color(age, across, palette);
            if (ghostAlpha >= 4) {
                float spread = .55f + .60f * ease(revealAge / 43);
                int ghostColor = argb(ghostAlpha, palette.ghost);
                font.drawInBatch(glyph.sequence, x - spread, .45f, ghostColor, false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
                font.drawInBatch(glyph.sequence, x + spread, -.45f, ghostColor, false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            font.drawInBatch8xOutline(glyph.sequence, x, 0, argb(alpha, color),
                argb(Math.max(4, alpha * 3 / 4), palette.outline), matrix, buffers, LightTexture.FULL_BRIGHT);
        }
        pose.popPose();
    }

    private static Line line(Font font, Component text, boolean epithet) {
        List<Glyph> glyphs = new ArrayList<>();
        float[] width = {0};
        int[] previous = {-1};
        text.getVisualOrderText().accept((index, style, codePoint) -> {
            FormattedCharSequence sequence = sink -> sink.accept(0, style.withColor((TextColor) null), codePoint);
            if (previous[0] >= 0) width[0] += spacing(previous[0], codePoint, epithet);
            glyphs.add(new Glyph(sequence, width[0]));
            width[0] += font.getSplitter().stringWidth(sequence);
            previous[0] = codePoint;
            return true;
        });
        return new Line(List.copyOf(glyphs), width[0]);
    }

    private static float spacing(int left, int right, boolean epithet) {
        if (Character.isSpaceChar(left) || Character.isSpaceChar(right)
                || Character.getType(right) == Character.NON_SPACING_MARK) return 0;
        if (wide(left) && wide(right)) return epithet ? .92f : .58f;
        return epithet ? .58f : .26f;
    }

    private static boolean wide(int codePoint) {
        return switch (Character.UnicodeScript.of(codePoint)) {
            case HAN, HIRAGANA, KATAKANA, HANGUL -> true;
            default -> false;
        };
    }

    private static int color(float age, float across, Palette palette) {
        float wave = .5f + .5f * Mth.sin(age * .12f - across * 5f);
        int body = blend(palette.shadow, palette.light, wave);
        float gleam = Math.max(0, 1 - Math.abs(across - (age - 14) / 34) * 3.8f);
        return blend(body, palette.gleam, gleam * gleam * palette.gleamStrength);
    }

    private static int blend(int a, int b, float t) {
        int red = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
        int green = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
        int blue = (int) Mth.lerp(t, a & 255, b & 255);
        return red << 16 | green << 8 | blue;
    }

    private static int argb(int alpha, int rgb) {
        return alpha << 24 | rgb;
    }

    private static float ease(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private record Glyph(FormattedCharSequence sequence, float offset) {}
    private record Line(List<Glyph> glyphs, float width) {}
    private record Palette(int shadow, int light, int gleam, int ghost, int outline, float gleamStrength) {}
    private record Intro(WeakReference<ClientLevel> level, UUID dragon, long startedAt, Line name, Line epithet,
                         Palette palette, Palette epithetPalette) {}
}
