package dev.koikaster.mite;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.*;

/** Original one-slot PNG and XP-point labels; native book rendering API. No previewed enchantment clues. */
@OnlyIn(Dist.CLIENT)
public final class SourceEnchantScreen extends AbstractContainerScreen<SourceEnchantMenu> {
 private static final ResourceLocation GUI=Content.id("textures/gui/source_enchanting_table.png"),BOOK=new ResourceLocation("minecraft","textures/entity/enchanting_table_book.png");private BookModel book;private float open,oldOpen,flip,oldFlip,target,velocity;private ItemStack previous=ItemStack.EMPTY;private final RandomSource random=RandomSource.create();
 public SourceEnchantScreen(SourceEnchantMenu menu,Inventory inventory,Component title){super(menu,inventory,title);titleLabelX=12;}
 protected void init(){super.init();book=new BookModel(minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));}
 protected void containerTick(){super.containerTick();var item=menu.getSlot(0).getItem();if(!ItemStack.matches(item,previous)){previous=item.copy();do{target+=random.nextInt(4)-random.nextInt(4);}while(flip<=target+1&&flip>=target-1);}oldFlip=flip;oldOpen=open;open=Mth.clamp(open+(menu.levels[0]>0||menu.levels[1]>0||menu.levels[2]>0?.2f:-.2f),0,1);float move=Mth.clamp((target-flip)*.4f,-.2f,.2f);velocity+=(move-velocity)*.9f;flip+=velocity;}
 public boolean mouseClicked(double x,double y,int button){if(!SourceSkillWorld1820.enchanting(minecraft.player))return super.mouseClicked(x,y,button);for(int i=0;i<3;i++)if(isHovering(60,14+19*i,108,19,x,y)&&menu.canAfford(minecraft.player,i)){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i);return true;}return super.mouseClicked(x,y,button);}
 protected void renderBg(GuiGraphics g,float partial,int x,int y){g.blit(GUI,leftPos,topPos,0,0,imageWidth,imageHeight);renderBook(g,partial);EnchantmentNames.getInstance().initSeed(menu.glyphSeed());for(int i=0;i<3;i++){int xp=menu.cost(i),bx=leftPos+60,by=topPos+14+19*i;if(xp<=0){g.blit(GUI,bx,by,0,185,108,19);continue;}boolean afford=menu.canAfford(minecraft.player,i),hover=x>=bx&&y>=by&&x<bx+108&&y<by+19;g.blit(GUI,bx,by,0,afford?(hover?204:166):185,108,19);String text=Integer.toString(xp);int width=104-font.width(text);var words=EnchantmentNames.getInstance().getRandomName(font,width);g.drawWordWrap(font,words,bx+2,by+2,width,afford?(hover?0xffff80:6839882):3421237);g.drawString(font,text,bx+104-font.width(text),by+9,afford?8453920:4226832,false);}}
 private void renderBook(GuiGraphics g,float partial){float opened=Mth.lerp(partial,oldOpen,open),turn=Mth.lerp(partial,oldFlip,flip);Lighting.setupForEntityInInventory();g.pose().pushPose();g.pose().translate(leftPos+33,topPos+31,100);g.pose().scale(-40,40,40);g.pose().mulPose(Axis.XP.rotationDegrees(25));g.pose().translate((1-opened)*.2f,(1-opened)*.1f,(1-opened)*.25f);g.pose().mulPose(Axis.YP.rotationDegrees(-(1-opened)*90-90));g.pose().mulPose(Axis.XP.rotationDegrees(180));float a=Mth.clamp(Mth.frac(turn+.25f)*1.6f-.3f,0,1),b=Mth.clamp(Mth.frac(turn+.75f)*1.6f-.3f,0,1);book.setupAnim(0,a,b,opened);book.renderToBuffer(g.pose(),g.bufferSource().getBuffer(book.renderType(BOOK)),0xf000f0,OverlayTexture.NO_OVERLAY,1,1,1,1);g.flush();g.pose().popPose();Lighting.setupFor3DItems();}
 public void render(GuiGraphics g,int x,int y,float partial){renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);for(int i=0;i<3;i++)if(menu.cost(i)>0&&isHovering(60,14+19*i,108,19,x,y))g.renderTooltip(font,menu.canAfford(minecraft.player,i)&&!SourceSkillWorld1820.enchanting(minecraft.player)?Component.translatable("gui.mite_reforged.requires_skill",Component.translatable("skill.enchanting.profession")).withStyle(net.minecraft.ChatFormatting.GRAY):Component.translatable("gui.mite_reforged.enchant_cost",menu.cost(i)),x,y);}
}
