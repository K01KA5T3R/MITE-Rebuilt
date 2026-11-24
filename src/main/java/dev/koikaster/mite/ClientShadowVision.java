package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** avj vision darkens the scene before the HUD, without a fabricated texture or modern darkness effect. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientShadowVision {
 public static float alpha(float dim,float partial){float value=dim-partial*.01f;return value>.01f?Math.min(value,.9f):0;}
 @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event){event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(),"source_shadow_vision",(gui,graphics,partial,width,height)->{var player=Minecraft.getInstance().player;if(player==null)return;float a=alpha(((SourceShadowVision)player).mite$visionDimming(),partial);if(a<=0)return;gui.setupOverlayRenderState(true,false);graphics.fill(0,0,width,height,((int)(a*255)<<24));});}
 private ClientShadowVision(){}
}
