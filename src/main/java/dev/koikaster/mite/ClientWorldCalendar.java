package dev.koikaster.mite;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Permanent date/clock HUD. The human removed the32-day countdown. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientWorldCalendar {
 private static java.util.List<Component> lastLines=java.util.List.of();
 private static long renderedFrames;
 static java.util.List<Component> lastLines(){return lastLines;}
 static long renderedFrames(){return renderedFrames;}
 @SubscribeEvent public static void draw(RenderGuiEvent.Post event){
  var mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.options.hideGui){lastLines=java.util.List.of();return;}
  long ticks=mc.level.getDayTime(),day=SourceMoonCycle.dayOfWorld(ticks);
  int minute=(int)(Math.floorMod(ticks,24000L)*1440/24000+360)%1440;
  var time=Component.translatable("hud.mite_reforged.calendar",day,String.format(Locale.ROOT,"%02d:%02d",minute/60,minute%60));
  int right=mc.getWindow().getGuiScaledWidth()-6;var graphics=event.getGuiGraphics();
  graphics.drawString(mc.font,time,right-mc.font.width(time),6,0xffeeeeee,true);
  lastLines=java.util.List.of(time);renderedFrames++;
 }
 private ClientWorldCalendar(){}
}
