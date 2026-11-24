package dev.koikaster.mite;

import java.util.*;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Native controls and book model. Display only acknowledged server quotes. */
public final class AdvancedEnchantScreen extends AbstractContainerScreen<AdvancedEnchantMenu> {
 private static final ResourceLocation GUI=Content.id("textures/gui/source_enchanting_table.png"),BOOK=new ResourceLocation("minecraft","textures/entity/enchanting_table_book.png");
 private static final int ROWS=5,LIST_X=78,LIST_Y=18,ROW_HEIGHT=17,ROW_WIDTH=184,TRACK_X=264,TRACK_WIDTH=10,TRACK_HEIGHT=85;
 private final List<EntryButton> entries=new ArrayList<>();
 private int scroll,pendingRevision=-1;private long pendingSession;private double dragOffset;private boolean dragging;
 private ItemStack observedInput=ItemStack.EMPTY;private BookModel book;private Button minus,plus,confirm;
 public AdvancedEnchantScreen(AdvancedEnchantMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=284;imageHeight=236;titleLabelX=8;inventoryLabelX=61;inventoryLabelY=143;}
 public static void accept(AdvancedEnchantNetwork.Snapshot snapshot){
  var mc=Minecraft.getInstance();if(mc.player==null||!(mc.player.containerMenu instanceof AdvancedEnchantMenu menu)||menu.containerId!=snapshot.menu())return;
  if(menu.view!=null&&(menu.view.session()!=snapshot.session()||snapshot.revision()<=menu.view.revision()))return;
  menu.view=snapshot;if(mc.screen instanceof AdvancedEnchantScreen screen&&screen.menu==menu)screen.refresh();
 }
 @Override protected void init(){
  super.init();entries.clear();dragging=false;book=new BookModel(minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
  for(int i=0;i<ROWS;i++)entries.add(addRenderableWidget(new EntryButton(i)));
  minus=addRenderableWidget(Button.builder(Component.literal("-"),b->changeRank(-1)).bounds(leftPos+78,topPos+107,20,20).build());
  plus=addRenderableWidget(Button.builder(Component.literal("+"),b->changeRank(1)).bounds(leftPos+158,topPos+107,20,20).build());
  confirm=addRenderableWidget(Button.builder(Component.translatable("gui.mite_reforged.advanced.confirm"),b->{var v=menu.view;if(v!=null)request(v.selected(),v.rank(),true);}).bounds(leftPos+196,topPos+107,78,20).build());refresh();
 }
 private AdvancedEnchanting.Row selected(){var v=menu.view;return v==null||v.selected()==null?null:v.rows().stream().filter(row->row.id().equals(v.selected())).findFirst().orElse(null);}
 private AdvancedEnchanting.Row row(int visible){var v=menu.view;int index=visible+scroll;return v==null||index<0||index>=v.rows().size()?null:v.rows().get(index);}
 private boolean waiting(){var v=menu.view;if(pendingRevision>=0&&v!=null&&(v.session()!=pendingSession||v.revision()>pendingRevision))pendingRevision=-1;return pendingRevision>=0;}
 private void request(ResourceLocation id,int rank,boolean confirm){var v=menu.view;if(v==null||id==null||waiting())return;pendingSession=v.session();pendingRevision=v.revision();AdvancedEnchantNetwork.request(v,id,rank,confirm);refresh();}
 private void select(int visible){var row=row(visible);if(row!=null&&row.reason()==AdvancedEnchanting.Reason.READY)request(row.id(),row.minimum(),false);}
 private void changeRank(int delta){var v=menu.view;var row=selected();if(v!=null&&row!=null&&row.reason()==AdvancedEnchanting.Reason.READY){int rank=v.rank()+delta;if(rank>=row.minimum()&&rank<=row.maximum())request(row.id(),rank,false);}}
 private int maximumScroll(){return menu.view==null?0:Math.max(0,menu.view.rows().size()-ROWS);}
 private void setScroll(int value){scroll=Math.max(0,Math.min(maximumScroll(),value));refreshEntries();}
 private int thumbHeight(){int size=menu.view==null?0:menu.view.rows().size();return size<=ROWS?TRACK_HEIGHT:Math.max(15,TRACK_HEIGHT*ROWS/size);}
 private int thumbTop(){int maximum=maximumScroll();return LIST_Y+(maximum==0?0:(TRACK_HEIGHT-thumbHeight())*scroll/maximum);}
 private void dragTo(double y){int travel=TRACK_HEIGHT-thumbHeight();if(travel>0)setScroll((int)Math.round((y-topPos-LIST_Y-dragOffset)*maximumScroll()/travel));}
 private static Component reason(AdvancedEnchanting.Reason reason){return Component.translatable("gui.mite_reforged.advanced.reason."+reason.name().toLowerCase(Locale.ROOT));}
 private static Component enchantmentName(AdvancedEnchanting.Row row){var enchantment=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(row.id());return enchantment==null?Component.literal(row.id().toString()):Component.translatable(enchantment.getDescriptionId());}
 private static String range(AdvancedEnchanting.Row row){return row.reason()==AdvancedEnchanting.Reason.MAXIMUM?Integer.toString(row.current()):row.minimum()==row.maximum()?Integer.toString(row.maximum()):row.minimum()+"-"+row.maximum();}
 private void refreshEntries(){boolean pending=waiting();for(var entry:entries){var row=row(entry.index);entry.visible=row!=null;entry.active=!pending&&row!=null&&row.reason()==AdvancedEnchanting.Reason.READY;if(row==null){entry.setTooltip(null);continue;}var name=enchantmentName(row);entry.setMessage(name.copy().append(" "+range(row)));var tooltip=name.copy();if(row.reason()!=AdvancedEnchanting.Reason.READY)tooltip.append("\n").append(reason(row.reason()));entry.setTooltip(Tooltip.create(tooltip));}}
 private void refresh(){
  if(minus==null)return;var input=menu.input.getItem(0);if(!ItemStack.matches(observedInput,input)){observedInput=input.copy();scroll=0;dragging=false;}scroll=Math.min(scroll,maximumScroll());var v=menu.view;var row=selected();boolean pending=waiting();
  minus.active=!pending&&row!=null&&row.reason()==AdvancedEnchanting.Reason.READY&&v.rank()>row.minimum();plus.active=!pending&&row!=null&&row.reason()==AdvancedEnchanting.Reason.READY&&v.rank()<row.maximum();confirm.active=!pending&&row!=null&&row.reason()==AdvancedEnchanting.Reason.READY&&v.quote().reason()==AdvancedEnchanting.Reason.READY;confirm.setTooltip(v!=null&&v.quote().reason()!=AdvancedEnchanting.Reason.READY&&v.quote().reason()!=AdvancedEnchanting.Reason.EMPTY&&v.quote().reason()!=AdvancedEnchanting.Reason.INVALID?Tooltip.create(reason(v.quote().reason())):null);refreshEntries();
 }
 @Override protected void containerTick(){super.containerTick();refresh();}
 @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&maximumScroll()>0&&isHovering(TRACK_X,LIST_Y,TRACK_WIDTH,TRACK_HEIGHT,x,y)){dragging=true;double relative=y-topPos;dragOffset=relative>=thumbTop()&&relative<thumbTop()+thumbHeight()?relative-thumbTop():thumbHeight()/2d;dragTo(y);return true;}return super.mouseClicked(x,y,button);}
 @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&dragging){dragTo(y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
 @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&dragging){dragging=false;return true;}return super.mouseReleased(x,y,button);}
 @Override public boolean mouseScrolled(double x,double y,double amount){if(amount!=0&&maximumScroll()>0&&isHovering(LIST_X,LIST_Y,TRACK_X+TRACK_WIDTH-LIST_X,TRACK_HEIGHT,x,y)){setScroll(scroll-(int)Math.signum(amount));return true;}return super.mouseScrolled(x,y,amount);}
 @Override public boolean keyPressed(int key,int scan,int modifiers){if(getFocused() instanceof EntryButton){if(key==266||key==267){setScroll(scroll+(key==266?-ROWS:ROWS));return true;}if(key==268||key==269){setScroll(key==268?0:maximumScroll());return true;}}return super.keyPressed(key,scan,modifiers);}
 @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
  g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff000000);g.fill(leftPos+1,topPos+1,leftPos+imageWidth-1,topPos+imageHeight-1,0xff555555);g.fill(leftPos+1,topPos+1,leftPos+imageWidth-2,topPos+imageHeight-2,0xffffffff);g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xffc6c6c6);
  g.blit(GUI,leftPos+24,topPos+69,24,46,18,18);g.blit(GUI,leftPos+60,topPos+153,7,83,162,54);g.blit(GUI,leftPos+60,topPos+211,7,141,162,18);renderBook(g,partial);
  if(maximumScroll()>0){g.fill(leftPos+TRACK_X,topPos+LIST_Y,leftPos+TRACK_X+TRACK_WIDTH,topPos+LIST_Y+TRACK_HEIGHT,0xff373737);int y=topPos+thumbTop(),height=thumbHeight();g.fill(leftPos+TRACK_X+1,y,leftPos+TRACK_X+TRACK_WIDTH-1,y+height,0xffeeeeee);g.fill(leftPos+TRACK_X+2,y+1,leftPos+TRACK_X+TRACK_WIDTH-1,y+height,0xff555555);g.fill(leftPos+TRACK_X+2,y+1,leftPos+TRACK_X+TRACK_WIDTH-2,y+height-1,0xffb0b0b0);}
  var v=menu.view;if(v==null)return;var row=selected();if(row!=null)g.drawCenteredString(font,Component.translatable("gui.mite_reforged.advanced.rank",v.rank()),leftPos+128,topPos+113,0xffffff);
  g.drawString(font,Component.translatable("gui.mite_reforged.advanced.shelves",v.shelves()),leftPos+8,topPos+96,0x404040,false);g.drawString(font,Component.translatable("gui.mite_reforged.advanced.tier",v.maximumTier()),leftPos+8,topPos+108,0x404040,false);
  var cause=v.quote().reason();var cost=row==null?null:Component.translatable("gui.mite_reforged.advanced.cost",v.quote().levels());
  if(cause!=AdvancedEnchanting.Reason.READY&&cause!=AdvancedEnchanting.Reason.EMPTY&&cause!=AdvancedEnchanting.Reason.INVALID){var text=reason(cause);int width=imageWidth-16-(cost==null?0:font.width(cost)+8);g.drawString(font,font.plainSubstrByWidth(text.getString(),width),leftPos+8,topPos+132,0x703a30,false);}
  if(cost!=null)g.drawString(font,cost,leftPos+imageWidth-10-font.width(cost),topPos+132,cause==AdvancedEnchanting.Reason.READY?0x315f22:0x703a30,false);
 }
 private void renderBook(GuiGraphics g,float partial){float open=menu.input.isEmpty()?0:1,tick=minecraft.player==null?0:minecraft.player.tickCount+partial;g.enableScissor(leftPos+6,topPos+18,leftPos+72,topPos+65);Lighting.setupForEntityInInventory();g.pose().pushPose();g.pose().translate(leftPos+33,topPos+31,100);g.pose().scale(-35,35,35);g.pose().mulPose(Axis.XP.rotationDegrees(25));g.pose().mulPose(Axis.YP.rotationDegrees(-90-(1-open)*90));g.pose().mulPose(Axis.XP.rotationDegrees(180));book.setupAnim(tick,.1f,.9f,open);book.renderToBuffer(g.pose(),g.bufferSource().getBuffer(book.renderType(BOOK)),0xf000f0,OverlayTexture.NO_OVERLAY,1,1,1,1);g.flush();g.pose().popPose();Lighting.setupFor3DItems();g.disableScissor();}
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);refresh();super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
 private final class EntryButton extends Button {
  final int index;
  EntryButton(int index){super(leftPos+LIST_X,topPos+LIST_Y+index*ROW_HEIGHT,ROW_WIDTH,ROW_HEIGHT-1,Component.empty(),button->select(index),DEFAULT_NARRATION);this.index=index;}
  @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partial){
   var row=row(index);if(row==null)return;boolean selected=row.id().equals(menu.view.selected());
   int textureY=!active?185:selected||isHoveredOrFocused()?204:166;
   g.blitNineSliced(GUI,getX(),getY(),getWidth(),getHeight(),2,108,19,0,textureY);renderString(g,font,0xffffff);
  }
  @Override public void renderString(GuiGraphics g,Font font,int color){var row=row(index);if(row==null)return;boolean selected=row.id().equals(menu.view.selected());String range=range(row);int right=getX()+getWidth()-5,tint=!active?0xaaa393:selected||isHoveredOrFocused()?0xffff80:0x392d1b;g.drawString(font,font.plainSubstrByWidth(enchantmentName(row).getString(),getWidth()-14-font.width(range)),getX()+5,getY()+3,tint,false);g.drawString(font,range,right-font.width(range),getY()+3,tint,false);}
 }
}
