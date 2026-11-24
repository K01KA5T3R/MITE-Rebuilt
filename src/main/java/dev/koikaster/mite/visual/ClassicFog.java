package dev.koikaster.mite.visual;
import dev.koikaster.mite.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client fog only. Air colors use the original sky/distance blend.
 * Water uses the original EXP curve in the client fog include. The native distance
 * settings remain a safe fallback for packs that replace the include.
 * Blindness, darkness, powder snow, lava, spectators and unowned dimensions retain native effects.
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicFog {
 private static net.minecraft.client.multiplayer.ClientLevel fogLevel;
 private static float fogSky;
 private static long fogFrame;
 @SubscribeEvent public static void airColor(ViewportEvent.ComputeFogColor e){
  var mc=Minecraft.getInstance();if(!airViewer(e.getCamera())||e.getRenderer().getClass()!=GameRenderer.class)return;
  if(mc.level.dimension()==WorldEvents.UNDERWORLD){float gray=LegacyLighting.underworldFogGray(mc.level.getDayTime());e.setRed(gray);e.setGreen(gray);e.setBlue(gray);}
  else if(mc.level.dimension()==net.minecraft.world.level.Level.OVERWORLD){
   float partial=(float)e.getPartialTick(),angle=mc.level.getTimeOfDay(partial);
   float daylight=Mth.clamp(Mth.cos(angle*((float)Math.PI*2))*2+.5f,0,1);
   var sky=mc.level.getSkyColor(e.getCamera().getPosition(),partial);
   float direction=Mth.sin(mc.level.getSunAngle(partial))>0?-1:1;
   float facing=Math.max(0,e.getCamera().getLookVector().x()*direction);
   var color=ClassicAirFog.color(new float[]{.7529412f*(daylight*.94f+.06f),.84705883f*(daylight*.94f+.06f),daylight*.91f+.09f},
    new float[]{(float)sky.x,(float)sky.y,(float)sky.z},mc.options.getEffectiveRenderDistance(),mc.level.effects().getSunriseColor(angle,partial),facing,mc.level.getRainLevel(partial),mc.level.getThunderLevel(partial));
   float height=Mth.clamp(((float)e.getCamera().getPosition().y-mc.level.getMinBuildHeight())*mc.level.getLevelData().getClearColorScale(),0,1);height*=height;
   float dark=e.getRenderer().getDarkenWorldAmount(partial);
   color[0]*=height*(1-.3f*dark);color[1]*=height*(1-.4f*dark);color[2]*=height*(1-.4f*dark);
   if(e.getCamera().getEntity() instanceof LivingEntity living&&living.hasEffect(MobEffects.NIGHT_VISION)){
    float vision=GameRenderer.getNightVisionScale(living,partial),maximum=Math.max(color[0],Math.max(color[1],color[2]));
    if(maximum>0)for(int i=0;i<3;i++)color[i]=Mth.lerp(vision,color[i],color[i]/maximum);
   }
   e.setRed(color[0]);e.setGreen(color[1]);e.setBlue(color[2]);
  }
 }
 @SubscribeEvent public static void airDistance(ViewportEvent.RenderFog event){
  var mc=Minecraft.getInstance();if(!airViewer(event.getCamera())||event.getRenderer().getClass()!=GameRenderer.class||event.getType()!=FogType.NONE||event.getMode()!=net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN)return;
  float distance=event.getRenderer().getRenderDistance();if(!Float.isFinite(distance)||distance<=0)return;
  int sky=mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,event.getCamera().getBlockPosition());
  long now=System.nanoTime();if(fogLevel!=mc.level){fogLevel=mc.level;fogSky=0;fogFrame=now;}
  double height=event.getCamera().getPosition().y-mc.level.getMinBuildHeight();
  fogSky=LegacyLighting.approachFogSky(fogSky,sky,Math.min(1,(now-fogFrame)/1_000_000_000f),height<0);fogFrame=now;
  boolean overworld=mc.level.dimension()==net.minecraft.world.level.Level.OVERWORLD;
  float adjusted=overworld&&ClientVisualSettings.voidFog()?ClassicFogEnvironment.distance(distance,height,fogSky):distance;
  int dimension=mc.level.dimension()==WorldEvents.UNDERWORLD?-2:mc.level.dimension()==net.minecraft.world.level.Level.NETHER?-1:mc.level.dimension()==net.minecraft.world.level.Level.END?1:0;
  float[] range=ClassicFogRanges.terrain(adjusted,dimension,event.getCamera().getBlockPosition().getY());
  event.setNearPlaneDistance(range[0]);event.setFarPlaneDistance(range[1]);event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);event.setCanceled(true);
 }

 private static boolean airViewer(net.minecraft.client.Camera camera){
  var mc=Minecraft.getInstance();return ClassicVisualState.ready&&ClientVisualSettings.classicFog()&&mc.level!=null&&mc.player!=null&&knownDimension(mc.level.dimension())&&camera.getClass()==net.minecraft.client.Camera.class&&camera.getEntity()==mc.player&&!mc.player.isSpectator()&&!protectedVision(mc.player)&&camera.getFluidInCamera()==FogType.NONE;
 }

 static boolean protectedVision(Object entity){return entity instanceof LivingEntity living&&(living.hasEffect(MobEffects.BLINDNESS)||living.hasEffect(MobEffects.DARKNESS));}
 @SubscribeEvent public static void waterColor(ViewportEvent.ComputeFogColor event){
  var mc=Minecraft.getInstance();var camera=event.getCamera();var entity=camera.getEntity();
  if(!ClassicVisuals.water()||mc.level==null||camera.getFluidInCamera()!=FogType.WATER||entity==null||entity.isSpectator()||protectedVision(entity))return;
  if(!knownDimension(mc.level.dimension()))return;
  int respiration=entity instanceof LivingEntity living?EnchantmentHelper.getRespiration(living):0;
  float r=.02f+respiration*.2f,g=r,b=.2f+respiration*.2f;
  // Preserve modern void darkening and the current boss/world darkening amount.
  float height=Mth.clamp(((float)camera.getPosition().y-mc.level.getMinBuildHeight())*mc.level.getLevelData().getClearColorScale(),0,1);height*=height;
  float dark=event.getRenderer().getDarkenWorldAmount((float)event.getPartialTick());
  r*=height*(1-.3f*dark);g*=height*(1-.4f*dark);b*=height*(1-.4f*dark);
  if(entity instanceof LivingEntity living&&living.hasEffect(MobEffects.NIGHT_VISION)){
   float vision=GameRenderer.getNightVisionScale(living,(float)event.getPartialTick()),maximum=Math.max(r,Math.max(g,b));
   if(maximum>0){float scale=1/maximum;r=Mth.lerp(vision,r,r*scale);g=Mth.lerp(vision,g,g*scale);b=Mth.lerp(vision,b,b*scale);}
  }
  event.setRed(r);event.setGreen(g);event.setBlue(b);
 }
 @SubscribeEvent public static void waterDistance(ViewportEvent.RenderFog event){
  var mc=Minecraft.getInstance();var entity=event.getCamera().getEntity();
  if(!ClassicVisuals.water()||mc.level==null||event.getType()!=FogType.WATER||entity==null||entity.isSpectator()||protectedVision(entity)||!knownDimension(mc.level.dimension()))return;
  int respiration=entity instanceof LivingEntity living?EnchantmentHelper.getRespiration(living):0;
  boolean breathing=entity instanceof LivingEntity living&&living.hasEffect(MobEffects.WATER_BREATHING);
  float density=breathing?.05f:Math.max(.01f,.1f-respiration*.03f);
  float far=Math.min(event.getRenderer().getRenderDistance(),(float)(-Math.log(ClassicWaterFogShader.available()?.0001:.05)/density));
  event.setNearPlaneDistance(0);event.setFarPlaneDistance(far);event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);event.setCanceled(true);
 }
 static boolean knownDimension(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key){return key==net.minecraft.world.level.Level.OVERWORLD||key==net.minecraft.world.level.Level.NETHER||key==net.minecraft.world.level.Level.END||key==WorldEvents.UNDERWORLD;}
 @SubscribeEvent public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){if(event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel old){ClassicFogEnvironment.unload(old);if(fogLevel==old){fogLevel=null;fogSky=0;fogFrame=0;}}}
 @SubscribeEvent public static void chunkLoaded(net.minecraftforge.event.level.ChunkEvent.Load event){if(event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel current)ClassicFogEnvironment.chunkLoaded(current,event.getChunk().getPos());}
 private ClassicFog(){}
}
