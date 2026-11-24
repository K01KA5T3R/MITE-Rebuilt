package dev.koikaster.mite.visual;

import net.minecraft.client.renderer.texture.TextureAtlas;

/** Original block/item atlas sampling. Keep allocated mip levels and user options intact. */
public final class ClassicTextureSampling {
    public static boolean mipmap(boolean nativeMipmap, Object texture) {
        if (!nativeMipmap || !ClassicVisualState.ready || !(texture instanceof TextureAtlas atlas)
                || !TextureAtlas.LOCATION_BLOCKS.equals(atlas.location())) return nativeMipmap;
        return !ClassicVisuals.textures();
    }

    private ClassicTextureSampling() {}
}
