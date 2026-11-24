package dev.koikaster.mite;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 axp inventory status boxes. Original PNGs stay intact; no food storage bar is added. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class DietInventoryDisplay {
 private static final ResourceLocation BACKGROUND=Content.id("textures/gui/r195_status.png"),ICONS=Content.id("textures/gui/r195_icons.png");
 @SubscribeEvent public static void render(ScreenEvent.Render.Post event){
  if(!(event.getScreen() instanceof InventoryScreen screen)||screen.getRecipeBookComponent().isVisible())return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||screen.getGuiLeft()<36)return;
  var diet=Diet.read(mc.player);int curseId=OriginalCurseEffects.id(mc.player);if(!diet.malnourished()&&diet.resistance==0&&curseId==0)return;
  boolean wide=screen.getGuiLeft()>=128;int x=screen.getGuiLeft()-(wide?128:36),y=screen.getGuiTop();
  if(diet.malnourished()){
   var title=Component.translatable("effect.mite_reforged.malnourished");
   var detail=Component.translatable(mc.gui.getGuiTicks()/100%2==0?"effect.mite_reforged.malnourished_healing":"effect.mite_reforged.malnourished_hunger");
   box(event.getGuiGraphics(),x,y,wide,18,false,0,title,detail);
   if(hover(event,x,y,wide)){
    var lines=new ArrayList<Component>();lines.add(title);if(diet.protein==0)lines.add(Component.translatable("effect.mite_reforged.missing_protein"));if(diet.phytonutrients==0)lines.add(Component.translatable("effect.mite_reforged.missing_phytonutrients"));
    lines.add(Component.translatable("effect.mite_reforged.malnourished_healing"));lines.add(Component.translatable("effect.mite_reforged.malnourished_hunger"));event.getGuiGraphics().renderComponentTooltip(mc.font,lines,event.getMouseX(),event.getMouseY());
   }y+=33;
  }
  if(diet.resistance>0){
   var title=Component.translatable("effect.mite_reforged.insulin_resistance");
   var time=Component.literal(String.format(java.util.Locale.ROOT,"%d:%02d",diet.insulin/1200,diet.insulin/20%60));
   box(event.getGuiGraphics(),x,y,wide,54,true,diet.resistance,title,time);
   if(hover(event,x,y,wide))event.getGuiGraphics().renderComponentTooltip(mc.font,java.util.List.of(title,Component.translatable("message.mite_reforged.insulin_"+diet.resistance),Component.translatable("effect.mite_reforged.insulin_description_"+diet.resistance)),event.getMouseX(),event.getMouseY());y+=33;
  }
  var curse=OriginalCurses.definition(curseId);if(curse!=null){boolean known=PlayerState.data(mc.player).getBoolean("CurseKnown");var detail=known?Component.translatable("curse.mite_reforged."+curse.key()+".name").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE):Component.translatable("curse.mite_reforged.unknown");box(event.getGuiGraphics(),x,y,wide,0,false,0,Component.translatable("effect.mite_reforged.cursed"),detail);if(known&&hover(event,x,y,wide)){var lines=new ArrayList<Component>();if(!wide)lines.add(detail);String text=Component.translatable("curse.mite_reforged."+curse.key()+".desc").getString();for(String line:text.split("\\\\?\\|"))lines.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));event.getGuiGraphics().renderComponentTooltip(mc.font,lines,event.getMouseX(),event.getMouseY());}}
 }
 private static boolean hover(ScreenEvent.Render.Post event,int x,int y,boolean wide){return event.getMouseX()>=x&&event.getMouseX()<x+(wide?123:32)&&event.getMouseY()>=y&&event.getMouseY()<y+32;}
 private static void box(GuiGraphics graphics,int x,int y,boolean wide,int u,boolean sugar,int severity,Component title,Component detail){
  if(wide)graphics.blit(BACKGROUND,x,y,0,166,140,32);
  else {graphics.blit(BACKGROUND,x,y,0,166,28,32);graphics.blit(BACKGROUND,x+28,y,119,166,4,32);}
  if(sugar){graphics.blit(new ResourceLocation("minecraft","textures/item/sugar.png"),x+7,y+8,0,0,16,16,16,16);graphics.setColor(1,severity==1?1:severity==2?2/3f:1/3f,severity==2?0:1/3f,1);}
  graphics.blit(ICONS,x+6,y+7,u,198,18,18);graphics.setColor(1,1,1,1);
  if(wide){var font=Minecraft.getInstance().font;graphics.drawString(font,title,x+27,y+7,0xffffff,false);graphics.drawString(font,detail,x+27,y+17,0x7f7f7f,false);}
 }
 private DietInventoryDisplay(){}
}
