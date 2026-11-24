package dev.koikaster.mite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Ghost recipe cells are deliberately separate from the real input and output. */
public final class MithrilAutoWorkbenchScreen extends AbstractContainerScreen<MithrilAutoWorkbench.Menu> {
 private Button deposit,withdraw,rune;
 public MithrilAutoWorkbenchScreen(MithrilAutoWorkbench.Menu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=340;imageHeight=218;inventoryLabelX=89;inventoryLabelY=125;}
 protected void init(){super.init();titleLabelX=(imageWidth-font.width(title))/2;
  deposit=addRenderableWidget(Button.builder(Component.translatable("gui.mite_reforged.auto.deposit"),b->press(0)).bounds(leftPos+8,topPos+103,96,20).build());
  withdraw=addRenderableWidget(Button.builder(Component.translatable("gui.mite_reforged.auto.withdraw"),b->press(1)).bounds(leftPos+108,topPos+103,76,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.mite_reforged.auto.clear"),b->press(2)).bounds(leftPos+188,topPos+103,68,20).build());
  rune=addRenderableWidget(Button.builder(Component.translatable("gui.mite_reforged.auto.rune"),b->press(3)).bounds(leftPos+260,topPos+103,72,20).build());
 }
 protected void containerTick(){super.containerTick();deposit.active=menu.credit()<MithrilAutoWorkbench.CREDIT_CAP;withdraw.active=menu.credit()>0;rune.active=menu.preview.getItem(0).getItem() instanceof SourceRunestoneItem;}
 private void press(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
  g.fill(leftPos-1,topPos-1,leftPos+imageWidth+1,topPos+imageHeight+1,0xff202020);g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffc6c6c6);
  for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff373737);g.fill(x,y,x+17,y+17,0xffeeeeee);g.fill(x,y,x+16,y+16,slot.index<9?0xff7f949d:0xff8b8b8b);}
  g.drawString(font,Component.translatable("gui.mite_reforged.auto.pattern"),leftPos+8,topPos+20,0xff404040,false);g.drawString(font,Component.translatable("gui.mite_reforged.auto.input"),leftPos+80,topPos+20,0xff404040,false);g.drawString(font,Component.translatable("gui.mite_reforged.auto.output"),leftPos+172,topPos+20,0xff404040,false);
  g.fill(leftPos+140,topPos+55,leftPos+164,topPos+63,0xff555555);int fill=(int)Math.min(24,24L*menu.progress()/menu.total());g.fill(leftPos+140,topPos+55,leftPos+140+fill,topPos+63,0xffdfdfdf);
  g.drawString(font,Component.translatable("gui.mite_reforged.auto.credit",menu.credit()),leftPos+8,topPos+89,0xff404040,false);g.drawString(font,Component.translatable("gui.mite_reforged.auto.state."+menu.status()),leftPos+172,topPos+89,0xff404040,false);
 }
 public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
