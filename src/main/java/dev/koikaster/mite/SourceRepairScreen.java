package dev.koikaster.mite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.*;

/** Native anvil PNG and editable name; no level-cost/config UI. */
@OnlyIn(Dist.CLIENT)
public final class SourceRepairScreen extends AbstractContainerScreen<SourceRepairMenu> implements ContainerListener {
 private static final ResourceLocation TEXTURE=new ResourceLocation("minecraft","textures/gui/container/anvil.png");private EditBox name;private boolean receiving;
 public SourceRepairScreen(SourceRepairMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
 protected void init(){super.init();titleLabelX=60;name=new EditBox(font,leftPos+62,topPos+24,103,12,Component.translatable("container.repair"));name.setCanLoseFocus(true);name.setTextColor(-1);name.setTextColorUneditable(-1);name.setBordered(false);name.setMaxLength(40);name.setResponder(text->{if(!receiving&&menu.rename(text))Network.repairName(menu.containerId,text);});addRenderableWidget(name);menu.addSlotListener(this);slotChanged(menu,0,menu.inputs.getItem(0));}
 public void removed(){super.removed();menu.removeSlotListener(this);}
 protected void containerTick(){super.containerTick();name.tick();}
 public void resize(net.minecraft.client.Minecraft mc,int width,int height){String value=name.getValue();super.resize(mc,width,height);name.setValue(value);}
 protected void renderBg(GuiGraphics graphics,float partial,int x,int y){graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);graphics.blit(TEXTURE,leftPos+59,topPos+20,0,imageHeight+(ClassicRepair.canRename(menu.inputs.getItem(0))?0:16),110,16);if((!menu.inputs.getItem(0).isEmpty()||!menu.inputs.getItem(1).isEmpty())&&menu.output.isEmpty())graphics.blit(TEXTURE,leftPos+99,topPos+45,imageWidth,0,28,21);}
 public void render(GuiGraphics graphics,int x,int y,float partial){renderBackground(graphics);super.render(graphics,x,y,partial);renderTooltip(graphics,x,y);}
 public boolean keyPressed(int key,int scan,int mods){return key==256?super.keyPressed(key,scan,mods):name.keyPressed(key,scan,mods)||name.canConsumeInput()||super.keyPressed(key,scan,mods);}
 public boolean mouseClicked(double x,double y,int button){boolean was=name.isFocused();boolean result=super.mouseClicked(x,y,button);var first=menu.inputs.getItem(0);if(name.isFocused()&&!was&&!first.hasCustomHoverName()&&name.getValue().equals(first.getHoverName().getString()))name.setValue("");return result;}
 public boolean charTyped(char value,int modifiers){return value!='"'&&super.charTyped(value,modifiers);}
 public void slotChanged(AbstractContainerMenu menu,int slot,ItemStack stack){if(slot!=0||name==null)return;receiving=true;try{name.setValue(stack.isEmpty()?"":stack.getHoverName().getString());name.setEditable(ClassicRepair.canRename(stack));}finally{receiving=false;}}
 public void dataChanged(AbstractContainerMenu menu,int index,int value){}
}
