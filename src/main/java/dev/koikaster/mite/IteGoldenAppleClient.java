package dev.koikaster.mite;

import net.minecraft.client.Minecraft;

/** Client-safe duration helper for ITE golden apple eating animation timing. */
public final class IteGoldenAppleClient {
    public static int duration(Object stack, int original) {
        return IteGoldenApples.duration(stack, original, Minecraft.getInstance().player);
    }

    private IteGoldenAppleClient() {}
}
