package dev.koikaster.mite;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Builds the front sprites from the active resource pack so classic and modern art both fade. */
public final class ClientFurnaceEmbers implements SpriteSource {
    private static final ClientFurnaceEmbers INSTANCE = new ClientFurnaceEmbers();
    private static final SpriteSourceType TYPE = new SpriteSourceType(Codec.unit(INSTANCE));
    private static final ResourceLocation BLOCKS = new ResourceLocation("minecraft", "blocks");

    private ClientFurnaceEmbers() {}

    public static List<SpriteSource> append(List<SpriteSource> sources, Object rawAtlas) {
        if (!BLOCKS.equals(rawAtlas)) return sources;
        var combined = new ArrayList<>(sources);
        combined.add(INSTANCE);
        return combined;
    }

    @Override
    public SpriteSourceType type() { return TYPE; }

    @Override
    public void run(ResourceManager resources, Output output) {
        add(resources, output, "minecraft", "furnace", "_front");
        add(resources, output, "minecraft", "blast_furnace", "_front");
        add(resources, output, "minecraft", "smoker", "_front");
        add(resources, output, "mite_reforged", "clay_oven", "_front_off");
        add(resources, output, "mite_reforged", "hardened_clay_oven", "_front_off");
        add(resources, output, "mite_reforged", "sandstone_oven", "_front_off");
        add(resources, output, "mite_reforged", "stone_furnace", "_front_off");
        add(resources, output, "mite_reforged", "netherrack_furnace", "_front_off");
        add(resources, output, "mite_reforged", "obsidian_furnace", "_front_off");
    }

    private static void add(ResourceManager resources, Output output, String namespace, String block, String offSuffix) {
        var front = "block/" + block;
        var on = texture(resources, namespace, front + "_front_on");
        if (on == null) return;
        var off = texture(resources, namespace, front + offSuffix);
        for (int stage = 1; stage <= FurnaceEmbers.LAST_STAGE; stage++) {
            var id = new ResourceLocation(namespace, front + "_embers_" + stage);
            int weight = stage;
            output.add(id, () -> blend(id, on, off, weight));
        }
    }

    private static Resource texture(ResourceManager resources, String namespace, String path) {
        var id = new ResourceLocation(namespace, path);
        return resources.getResource(TEXTURE_ID_CONVERTER.idToFile(id)).orElse(null);
    }

    private static SpriteContents blend(ResourceLocation id, Resource on, Resource off, int stage) {
        var burning = SpriteLoader.loadSprite(id, on);
        if (burning == null || off == null) return burning;
        try (var cold = SpriteLoader.loadSprite(id, off)) {
            if (cold == null) return burning;
            NativeImage bright = burning.getOriginalImage();
            NativeImage dark = cold.getOriginalImage();
            for (int y = 0; y < bright.getHeight(); y++) {
                int darkY = y % burning.height() * cold.height() / burning.height();
                for (int x = 0; x < bright.getWidth(); x++) {
                    int darkX = x % burning.width() * cold.width() / burning.width();
                    int full = bright.getPixelRGBA(x, y);
                    int base = dark.getPixelRGBA(darkX, darkY);
                    bright.setPixelRGBA(x, y, interpolate(full, base, stage));
                }
            }
        } catch (RuntimeException error) {
            burning.close();
            throw error;
        }
        return burning;
    }

    private static int interpolate(int full, int base, int stage) {
        int color = 0;
        for (int channel = 0; channel < 32; channel += 8) {
            int on = (full >>> channel) & 255;
            int off = (base >>> channel) & 255;
            int value = (on * (4 - stage) + off * stage + 2) / 4;
            color |= value << channel;
        }
        return color;
    }
}

