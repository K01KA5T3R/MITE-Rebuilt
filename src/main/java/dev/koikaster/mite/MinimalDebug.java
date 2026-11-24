package dev.koikaster.mite;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** The human-selected minimal information is a permanent HUD. Native F3 and
 * hardware/chunk/target diagnostics remain suppressed. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class MinimalDebug {
 private static List<String> lastRendered=List.of();
 private static long renderedFrames,suppressedGraphs;
 static List<String> lastRendered(){return lastRendered;}
 static long renderedFrames(){return renderedFrames;}
 static long suppressedGraphs(){return suppressedGraphs;}
 private static void chartsOff(){var options=Minecraft.getInstance().options;options.renderDebugCharts=false;options.renderFpsChart=false;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.START)chartsOff();}
 @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.START)chartsOff();}
 static List<String> lines(Minecraft mc){
  if(mc.player==null||mc.level==null)return List.of();
  var lines=new ArrayList<String>();
  String version=ModList.get().getModContainerById(MiteReforged.ID).orElseThrow().getModInfo().getVersion().toString();
  lines.add("MITE "+version+" / Minecraft 1.20.1");
  lines.add(Component.translatable("gui.mite_reforged.debug_fps",mc.getFps()).getString());
  boolean reduced=mc.player.isReducedDebugInfo()||mc.options.reducedDebugInfo().get();
  if(!reduced){var p=mc.player.blockPosition();lines.add("XYZ: "+p.getX()+" / "+p.getY()+" / "+p.getZ());}
  lines.add(Component.translatable("gui.mite_reforged.debug_facing",Component.translatable("gui.mite_reforged.direction_"+mc.player.getDirection().getName())).getString());
  if(!reduced){var key=mc.level.getBiome(mc.player.blockPosition()).unwrapKey();if(key.isPresent()){var id=key.get().location();String translation="biome."+id.getNamespace()+"."+id.getPath().replace('/','.');var name=net.minecraft.client.resources.language.I18n.exists(translation)?Component.translatable(translation):Component.literal(id.toString());lines.add(Component.translatable("gui.mite_reforged.debug_biome",name).getString());}}
  return List.copyOf(lines);
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void overlay(RenderGuiOverlayEvent.Pre event){
  if(event.getOverlay().id().equals(VanillaGuiOverlay.FPS_GRAPH.id())){event.setCanceled(true);suppressedGraphs++;return;}
  if(!event.getOverlay().id().equals(VanillaGuiOverlay.DEBUG_TEXT.id()))return;
  event.setCanceled(true);chartsOff();
 }
 @SubscribeEvent public static void draw(RenderGuiEvent.Post event){
  var mc=Minecraft.getInstance();
  if(mc.options.hideGui||mc.player==null||mc.level==null){lastRendered=List.of();return;}
  var lines=lines(mc);var graphics=event.getGuiGraphics();
  for(int i=0;i<lines.size();i++){int y=2+i*9;String line=lines.get(i);graphics.fill(1,y-1,mc.font.width(line)+3,y+8,0x90000000);graphics.drawString(mc.font,line,2,y,0xe0e0e0,false);}
  lastRendered=lines;renderedFrames++;
 }
 private MinimalDebug(){}
}
