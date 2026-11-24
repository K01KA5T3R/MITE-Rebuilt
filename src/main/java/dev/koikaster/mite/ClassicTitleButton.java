package dev.koikaster.mite;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Old aut button geometry and avq language icon, without changing other screens. */
final class ClassicTitleButton extends Button {
 private static final ResourceLocation WIDGETS=ClassicTitleScreen.asset("widgets.png");
 private final boolean language;
 ClassicTitleButton(int x,int y,int width,Component label,OnPress press,boolean language){super(x,y,width,20,label,press,DEFAULT_NARRATION);this.language=language;}
 @Override public void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial){
  RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();graphics.setColor(1,1,1,1);
  if(language)graphics.blit(WIDGETS,getX(),getY(),0,isHoveredOrFocused()?126:106,20,20);
  else{
   int row=46+(active?(isHoveredOrFocused()?2:1):0)*20,left=width/2;
   graphics.blit(WIDGETS,getX(),getY(),0,row,left,20);
   graphics.blit(WIDGETS,getX()+left,getY(),200-(width-left),row,width-left,20);
   graphics.drawCenteredString(Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+6,!active?0xa0a0a0:isHoveredOrFocused()?0xffffa0:0xe0e0e0);
  }
 }
}
