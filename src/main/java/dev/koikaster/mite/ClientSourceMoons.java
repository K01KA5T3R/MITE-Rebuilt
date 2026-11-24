package dev.koikaster.mite;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** bfl moon tint and unchanged source raster halos, on the native sky pose. */
public final class ClientSourceMoons {
 private static final ResourceLocation MOON=Content.id("textures/environment/moon_phases.png"),HALO=Content.id("textures/environment/halo_256.png"),RING=Content.id("textures/environment/ring_128.png");
 static long moonFrames,haloFrames,ringFrames;static SourceMoonCycle.Tint lastTint=new SourceMoonCycle.Tint(1,1,1);static int lastPhase;static float lastHaloAlpha,lastRingAlpha;
 private static boolean overworld(){var level=Minecraft.getInstance().level;return level!=null&&level.dimension()==Level.OVERWORLD;}
 private static boolean blood(){var level=Minecraft.getInstance().level;return overworld()&&SourceMoonCycle.blood(level.getDayTime(),false);}
 private static float rain(float partial){var mc=Minecraft.getInstance();if(!mc.level.getBiome(mc.gameRenderer.getMainCamera().getBlockPosition()).value().hasPrecipitation()&&!blood())return 0;return mc.level.getRainLevel(partial);}
 public static boolean hasPrecipitation(boolean original){return original||blood();}
 public static Biome.Precipitation precipitation(Biome.Precipitation original){return original==Biome.Precipitation.NONE&&blood()?Biome.Precipitation.RAIN:original;}
 public static void beforeMoon(float partial){
  if(!overworld())return;var mc=Minecraft.getInstance();lastTint=SourceMoonCycle.tint(mc.level.getDayTime());lastPhase=SourceMoonCycle.phase(mc.level.getDayTime());
  RenderSystem.setShaderTexture(0,MOON);RenderSystem.setShaderColor(lastTint.red(),lastTint.green(),lastTint.blue(),1-rain(partial));moonFrames++;
 }
 private static void quad(PoseStack pose,ResourceLocation texture,float size,float red,float green,float blue,float alpha){
  if(alpha<=0)return;RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,texture);RenderSystem.setShaderColor(red,green,blue,alpha);
  var builder=Tesselator.getInstance().getBuilder();var matrix=pose.last().pose();builder.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);
  builder.vertex(matrix,-size,-100,size).uv(0,0).endVertex();builder.vertex(matrix,size,-100,size).uv(1,0).endVertex();builder.vertex(matrix,size,-100,-size).uv(1,1).endVertex();builder.vertex(matrix,-size,-100,-size).uv(0,1).endVertex();BufferUploader.drawWithShader(builder.end());
 }
 public static void afterMoon(Object rawPose,float partial){
  if(!overworld())return;var pose=(PoseStack)rawPose;var level=Minecraft.getInstance().level;long time=level.getDayTime();float rain=rain(partial),angle=level.getTimeOfDay(partial);lastHaloAlpha=lastRingAlpha=0;
  if(SourceMoonCycle.blood(time,false)||SourceMoonCycle.blue(time,false)){
   lastHaloAlpha=SourceMoonCycle.haloAlpha(angle,rain);if(SourceMoonCycle.blood(time,false))quad(pose,HALO,160,.6f,.2f,.1f,lastHaloAlpha);else quad(pose,HALO,160,.33f,.37f,.8f,lastHaloAlpha);haloFrames++;
  }else if(SourceMoonCycle.moonDog(time,false)){lastRingAlpha=SourceMoonCycle.ringAlpha(angle,rain);quad(pose,RING,80,.4975f,.6275f,.85f,lastRingAlpha);ringFrames++;}
  RenderSystem.setShaderColor(1,1,1,1-rain);
 }
 private ClientSourceMoons(){}
}
