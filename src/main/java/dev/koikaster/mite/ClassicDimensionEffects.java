package dev.koikaster.mite;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
/** Latest visual specification: old128 cloud plane; native height when disabled. */
public final class ClassicDimensionEffects extends DimensionSpecialEffects {
 private final DimensionSpecialEffects.OverworldEffects delegate=new DimensionSpecialEffects.OverworldEffects();
 public ClassicDimensionEffects(){super(DimensionSpecialEffects.OverworldEffects.CLOUD_LEVEL,true,SkyType.NORMAL,false,false);}
 private static boolean shortDistance(){return dev.koikaster.mite.visual.ClassicAirFog.distanceIndex(net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance())>=2;}
 @Override public float getCloudHeight(){return dev.koikaster.mite.visual.ClassicVisuals.sky()?(shortDistance()?Float.NaN:128):delegate.getCloudHeight();}
 @Override public boolean renderSky(net.minecraft.client.multiplayer.ClientLevel level,int ticks,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.Camera camera,org.joml.Matrix4f projection,boolean foggy,Runnable setupFog){
  // Both original bfe renderers skip the sky pass for Short and Tiny distances.
  return dev.koikaster.mite.visual.ClassicVisuals.sky()&&shortDistance();
 }
 public Vec3 getBrightnessDependentFogColor(Vec3 color,float sun){return delegate.getBrightnessDependentFogColor(color,sun);}
 public boolean isFoggyAt(int x,int z){return false;}
}
