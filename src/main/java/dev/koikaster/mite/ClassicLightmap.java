package dev.koikaster.mite;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Writes the complete 16x16 lightmap before terrain, entity and hand rendering. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicLightmap {
 private static java.lang.reflect.Field pixelsField,textureField;
 private static boolean failed;
 private static float flicker,flickerNoise;
 private static final net.minecraft.util.RandomSource VISUAL_RANDOM=net.minecraft.util.RandomSource.create();
 public static void resetVisualFailure(){failed=false;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(event.phase==TickEvent.Phase.END){var mc=Minecraft.getInstance();if(mc.level!=null&&ClientVisualSettings.classicLightmap()){
   // R195 bfe.f: the .1 factor belongs to the lightmap consumer, not this recurrence.
   var random=VISUAL_RANDOM;flickerNoise=(float)((double)flickerNoise+(random.nextDouble()-random.nextDouble())*random.nextDouble()*random.nextDouble());flickerNoise=(float)((double)flickerNoise*.9);
   flicker+=(flickerNoise-flicker)*1.0f;
  }}
 }
 @SubscribeEvent public static void render(RenderLevelStageEvent event){
  if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY||failed||!ClientVisualSettings.classicLightmap())return;
  var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.player.hasEffect(MobEffects.DARKNESS))return;
  try{
   if(pixelsField==null){
    for(var field:LightTexture.class.getDeclaredFields()){
     if(field.getType()==NativeImage.class){pixelsField=field;field.setAccessible(true);}
     else if(field.getType()==DynamicTexture.class){textureField=field;field.setAccessible(true);}
    }
    if(pixelsField==null||textureField==null)throw new IllegalStateException("Unsupported LightTexture layout");
   }
   var lightmap=mc.gameRenderer.lightTexture();var pixels=(NativeImage)pixelsField.get(lightmap);var texture=(DynamicTexture)textureField.get(lightmap);
   if(pixels.getWidth()!=16||pixels.getHeight()!=16)throw new IllegalStateException("Unsupported lightmap size");
   float partial=event.getPartialTick(),vision=0;
   int dimension=mc.level.dimension()==Level.NETHER?-1:mc.level.dimension()==WorldEvents.UNDERWORLD?-2:mc.level.dimension()==Level.END?1:0;
   float sun=LegacyLighting.sunBrightness(mc.level.getDayTime(),1,mc.level.getRainLevel(1),mc.level.getThunderLevel(1),dimension);
   var effect=mc.player.getEffect(MobEffects.NIGHT_VISION);
   if(effect!=null){int duration=effect.getDuration();vision=LegacyLighting.visionScale(duration,partial);}
   boolean overworld=mc.level.dimension()==Level.OVERWORLD;boolean end=mc.level.dimension()==Level.END;
   float ambient=mc.level.dimension()==Level.NETHER?.1f:0;
   boolean moons=Settings.MOONS;
   var moon=moons&&overworld?SourceMoonCycle.kind(mc.level.getDayTime()):Rules.Moon.NORMAL;
   float rain=mc.level.getRainLevel(partial),lightning=mc.level.getSkyFlashTime()>0?(float)Math.pow(rain,4):0;
   var conditions=new LegacyLighting.Sky(overworld?LegacyLighting.moonAscension(mc.level.getDayTime()):0,LegacyLighting.moonBrightness(mc.level.getDayTime(),rain,moon),lightning,moon,overworld);
   for(int sky=0;sky<16;sky++)for(int block=0;block<16;block++)pixels.setPixelRGBA(block,sky,LegacyLighting.sample(sky,block,sun,ambient,flicker,(float)ClientVisualSettings.gamma(),vision,mc.gameRenderer.getDarkenWorldAmount(partial),end,conditions).abgr());
   int active=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE),bound=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
   try{texture.upload();}finally{com.mojang.blaze3d.platform.GlStateManager._activeTexture(active);com.mojang.blaze3d.platform.GlStateManager._bindTexture(bound);}
  }catch(ReflectiveOperationException|RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().error("Classic lightmap unavailable; retaining vanilla rendering",error);}
 }
 private ClassicLightmap(){}
}
