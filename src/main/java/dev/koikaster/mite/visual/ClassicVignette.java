package dev.koikaster.mite.visual;

import dev.koikaster.mite.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Original vignette cadence and unattenuated light sample; native border warning stays intact. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicVignette {
 private static boolean active;
 private static float brightness=1;
 @SubscribeEvent public static void render(RenderGuiOverlayEvent.Pre event){
  if(!event.getOverlay().id().equals(VanillaGuiOverlay.VIGNETTE.id()))return;
  var mc=Minecraft.getInstance();
  if(!ClassicVisuals.lighting()||mc.level==null||mc.player==null||mc.getCameraEntity()==null||!(mc.gui instanceof net.minecraftforge.client.gui.overlay.ForgeGui gui)){active=false;return;}
  var dimension=mc.level.dimension();
  if(dimension!=Level.OVERWORLD&&dimension!=Level.NETHER&&dimension!=Level.END&&dimension!=WorldEvents.UNDERWORLD){active=false;return;}
  boolean cursed=OriginalCurseEffects.id(mc.player)>0;
  if(mc.options.graphicsMode().get()==GraphicsStatus.FAST&&!cursed){active=false;return;}
  if(!active){brightness=mc.gui.vignetteBrightness;active=true;}
  var pos=BlockPos.containing(mc.player.getX(),mc.player.getY()+mc.player.getBbHeight()*.66,mc.player.getZ());
  int light=Math.max(mc.level.getBrightness(LightLayer.SKY,pos),mc.level.getBrightness(LightLayer.BLOCK,pos));
  float ambient=dimension==Level.NETHER?.1f:0;
  brightness=ClassicVignetteCurve.step(brightness,cursed?0:LegacyLighting.brightness(light,ambient));
  float nativeBrightness=mc.gui.vignetteBrightness;
  try{
   gui.setupOverlayRenderState(true,false);
   mc.gui.vignetteBrightness=dimension==WorldEvents.UNDERWORLD?0:brightness;
   mc.gui.renderVignette(event.getGuiGraphics(),mc.getCameraEntity());
   event.setCanceled(true);
   if(dimension==WorldEvents.UNDERWORLD)brightness=1;
  }finally{mc.gui.vignetteBrightness=nativeBrightness;}
 }
 private ClassicVignette(){}
}
