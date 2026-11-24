package dev.koikaster.mite;
import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;import net.minecraft.world.entity.player.Inventory;import net.minecraft.network.chat.Component;import net.minecraft.resources.ResourceLocation;
/** axh's original texture coordinates and two brewing animations. */
public final class SourceBrewingScreen1412 extends AbstractContainerScreen<SourceBrewingMenu1412>{
 private static final ResourceLocation TEXTURE=Content.id("textures/gui/container/source_brewing_stand1412.png");
 public SourceBrewingScreen1412(SourceBrewingMenu1412 menu,Inventory inventory,Component title){super(menu,inventory,title);}
 protected void init(){super.init();titleLabelX=(imageWidth-font.width(title))/2;inventoryLabelX=7;inventoryLabelY=imageHeight-96+3;}
 protected void renderLabels(GuiGraphics graphics,int x,int y){graphics.drawString(font,title,titleLabelX,6,0x282828,false);graphics.drawString(font,playerInventoryTitle,7,imageHeight-96+3,0x282828,false);}
 protected void renderBg(GuiGraphics graphics,float partial,int x,int y){graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);int remaining=menu.getBrewingTicks();if(remaining>0){int height=(int)(28f*(1f-(float)remaining/400f));if(height>0)graphics.blit(TEXTURE,leftPos+97,topPos+16,176,0,9,height);height=new int[]{29,24,20,16,11,6,0}[remaining/2%7];if(height>0)graphics.blit(TEXTURE,leftPos+65,topPos+14+29-height,185,29-height,12,height);}}
 public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
}
