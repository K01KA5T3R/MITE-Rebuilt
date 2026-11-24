package dev.koikaster.mite;
/** Visual-only accessors. Existing gameplay settings are deliberately separate. */
public final class ClientVisualSettings {
 public static boolean classicLightmap(){return dev.koikaster.mite.visual.ClassicVisuals.lighting();}
 public static boolean classicFog(){return dev.koikaster.mite.visual.ClassicVisuals.fog();}
 public static boolean voidFog(){return classicFog();}
 public static boolean classicTextures(){return dev.koikaster.mite.visual.ClassicVisuals.textures();}
 public static double gamma(){return net.minecraft.client.Minecraft.getInstance().options.gamma().get();}
 private ClientVisualSettings(){}
}
