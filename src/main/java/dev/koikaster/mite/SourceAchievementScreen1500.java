package dev.koikaster.mite;

import java.util.*;
import java.util.function.IntPredicate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 awq manual graph. Reading this view never grants an achievement. */
public final class SourceAchievementScreen1500 extends Screen {
 static final ResourceLocation FRAME=Content.id("textures/gui/source_achievement_background.png");
 static final ResourceLocation MUSHROOM=Content.id("item/source_achievement_mushroom5");
 static final int MIN_X=SourceAchievementCatalog561.NODES.stream().mapToInt(n->n.x()).min().orElseThrow()*24-112;
 static final int MIN_Y=SourceAchievementCatalog561.NODES.stream().mapToInt(n->n.y()).min().orElseThrow()*24-112;
 static final int MAX_X=SourceAchievementCatalog561.NODES.stream().mapToInt(n->n.x()).max().orElseThrow()*24-77;
 static final int MAX_Y=SourceAchievementCatalog561.NODES.stream().mapToInt(n->n.y()).max().orElseThrow()*24-77;
 private static final int R196_MAX_Y=SourceAchievementCatalog561.nodes(true).stream().mapToInt(n->n.y()).max().orElseThrow()*24-77;
 private double oldX=-130,oldY=-70,x=-130,y=-70,targetX=-130,targetY=-70;
 private int mode;private boolean dragging;private double lastMouseX,lastMouseY;private Button cycle;
 public SourceAchievementScreen1500(){super(Component.translatable("gui.mite_reforged.achievements"));}
 @Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static final class Models {
  @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){e.register(MUSHROOM);}
 }
 @Override protected void init(){
  mode=0;
  addRenderableWidget(new ClassicTitleButton(width/2+24,height/2+74,80,Component.translatable("gui.done"),b->onClose(),false));
  cycle=addRenderableWidget(new ClassicTitleButton(width/2-104,height/2+74,80,Component.empty(),b->{mode=1-mode;updateMode();},false));
  updateMode();if(minecraft.player!=null)SourceAchievementNetwork570.request(false);
 }
 private boolean otherPlayers(){String name=minecraft.getUser().getName();return ClientSourceAchievements570.world().values().stream().anyMatch(r->!r.username().equals(name));}
 private void updateMode(){if(cycle==null)return;cycle.visible=otherPlayers();cycle.active=true;if(!cycle.visible)mode=0;cycle.setMessage(Component.translatable(mode==0?"gui.mite_reforged.achievements.world":"gui.mite_reforged.achievements.player"));}
 private boolean unlocked(int id){return mode==0?ClientSourceAchievements570.own().contains(id):ClientSourceAchievements570.world().containsKey(id);}
 private IntPredicate unlocked(){return this::unlocked;}
 private boolean r196(){return minecraft.level!=null&&WorldContentMode.r196(minecraft.level);}
 private boolean extras(){return minecraft.level==null||WorldContentMode.rebuilt(minecraft.level);}
 private int maximumY(){return r196()?R196_MAX_Y:MAX_Y;}
 private SourceAchievementCatalog561.Node node(int id){return SourceAchievementCatalog561.node(id,r196());}
 private int left(){return (width-256)/2;}private int top(){return (height-202)/2;}
 @Override public void tick(){oldX=x;oldY=y;double dx=targetX-x,dy=targetY-y;if(dx*dx+dy*dy<4){x=targetX;y=targetY;}else{x+=dx*.85;y+=dy*.85;}}
 @Override public boolean mouseClicked(double mx,double my,int button){if(button==0&&mx>=left()+8&&mx<left()+232&&my>=top()+17&&my<top()+172){dragging=true;lastMouseX=mx;lastMouseY=my;return true;}return super.mouseClicked(mx,my,button);}
 @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(button==0&&mx>=left()+8&&mx<left()+232&&my>=top()+17&&my<top()+172){if(dragging){x-=mx-lastMouseX;y-=my-lastMouseY;targetX=Mth.clamp(x,MIN_X,MAX_X-1);targetY=Mth.clamp(y,MIN_Y,maximumY()-1);oldX=x;oldY=y;}dragging=true;lastMouseX=mx;lastMouseY=my;return true;}return super.mouseDragged(mx,my,button,dx,dy);}
 @Override public boolean mouseReleased(double mx,double my,int button){if(button==0)dragging=false;return super.mouseReleased(mx,my,button);}
 @Override public boolean keyPressed(int key,int scan,int mods){if(minecraft.options.keyInventory.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,mods);}
 @Override public void onClose(){minecraft.setScreen(null);}
 private static int edgeColor(boolean done,boolean available,boolean bright){return done?-9408400:available?(bright?-16728064:-16744448):0xff000000;}
 private void line(GuiGraphics g,SourceAchievementCatalog561.Node child,SourceAchievementCatalog561.Node parent,int ox,int oy,int vx,int vy,int color){
  int ax=child.x()*24-ox+11+vx,ay=child.y()*24-oy+11+vy,bx=parent.x()*24-ox+11+vx,by=parent.y()*24-oy+11+vy;
  if(child.flipped()){int swap=ax;ax=bx;bx=swap;swap=ay;ay=by;by=swap;}
  g.hLine(ax,bx,ay,color);g.vLine(bx,ay,by,color);
 }
 private void second(GuiGraphics g,SourceAchievementCatalog561.Node child,int ox,int oy,int vx,int vy,boolean bright){
  if(child.secondParent()<0)return;var parent=node(child.secondParent());boolean done=unlocked(child.id()),available=SourceAchievementRules561.canShow(child.id(),unlocked(),r196());
  if(!unlocked(child.secondParent())||!SourceAchievementRules561.allParents(child.secondParent(),true,unlocked(),r196())){if(!unlocked(parent.id()))done=false;available=false;}
  line(g,child,parent,ox,oy,vx,vy,edgeColor(done,available,bright));
 }
 private void edges(GuiGraphics g,int ox,int oy,int vx,int vy,boolean bright){
  for(int pass=0;pass<2;pass++)for(var child:SourceAchievementPolicy567.nodes(r196(),extras())){
   if(child.parent()>=0){var parent=node(child.parent());boolean done=unlocked(child.id()),available=SourceAchievementRules561.canShow(child.id(),unlocked(),r196());
    if(!SourceAchievementRules561.allParents(child.id(),false,unlocked(),r196())){if(!unlocked(parent.id()))done=false;available=false;}
    if(done&&pass==0||!done&&pass==1)continue;
    if(child.secondParent()>=0&&unlocked(parent.id()))second(g,child,ox,oy,vx,vy,bright);
    line(g,child,parent,ox,oy,vx,vy,edgeColor(done,available,bright));
   }
   if(child.secondParent()>=0&&!unlocked(child.parent()))second(g,child,ox,oy,vx,vy,bright);
  }
 }
 static Block tile(Random random,int column,int row){random.setSeed(1234L+column);random.nextInt();int v=random.nextInt(1+row)+row/2;if(v>37||row==35)return Blocks.BEDROCK;if(v==22)return random.nextInt(2)==0?Blocks.DIAMOND_ORE:Blocks.REDSTONE_ORE;if(v==10)return Blocks.IRON_ORE;if(v==8)return Blocks.COAL_ORE;if(v>4)return Blocks.STONE;if(v>0)return Blocks.DIRT;return Blocks.SAND;}
 static ItemStack icon(SourceAchievementCatalog561.Node n){if(n.name().equals("potion"))return SourceLegacyPotions1406.stack(0);var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(n.icon()));return item==null?ItemStack.EMPTY:new ItemStack(item);}
 private void item(GuiGraphics g,SourceAchievementCatalog561.Node node,int sx,int sy){
  ItemStack stack=icon(node);if(!node.name().equals("supersizeMe")){g.renderItem(stack,sx,sy);return;}
  var model=minecraft.getModelManager().getModel(MUSHROOM);var pose=g.pose();pose.pushPose();pose.translate(sx+8,sy+8,150);pose.mulPoseMatrix(new org.joml.Matrix4f().scaling(1,-1,1));pose.scale(16,16,16);boolean flat=!model.usesBlockLight();if(flat)Lighting.setupForFlatItems();minecraft.getItemRenderer().render(stack,ItemDisplayContext.GUI,false,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY,model);g.flush();if(flat)Lighting.setupFor3DItems();pose.popPose();
 }
 private Component nodeTitle(SourceAchievementCatalog561.Node n){
  String key = "achievement.mite_reforged.source." + n.name();
  if (r196() && n.name().equals("buildChainMail")) key += ".r196";
  return Component.translatable(key);
 }
 private Component description(SourceAchievementCatalog561.Node n){
  String suffix = r196() && n.name().equals("buildWorkBench") ? ".r196.desc" : ".desc";
  return Component.translatable("achievement.mite_reforged.source." + n.name() + suffix,
      minecraft.options.keyInventory.getTranslatedKeyMessage());
 }
 private Component taken(SourceAchievementCatalog561.Node n){var first=ClientSourceAchievements570.world().get(n.id());return mode==1&&first!=null&&!first.username().isEmpty()?Component.translatable("gui.mite_reforged.achievements.taken_by",first.username()):Component.translatable("gui.mite_reforged.achievements.taken");}
 private void tooltip(GuiGraphics g,SourceAchievementCatalog561.Node n,int mx,int my){
  boolean show=SourceAchievementRules561.canShow(n.id(),unlocked(),r196())||unlocked(n.id());Component title=nodeTitle(n),body=show?description(n):Component.translatable("gui.mite_reforged.achievements.see",nodeTitle(node(SourceAchievementRules561.referTo(n.id(),unlocked(),r196()))));
  int tw=Math.max(font.width(title),show?n.tooltipWidth():120);if(show&&unlocked(n.id())&&mode==1)tw=Math.max(tw,font.width(taken(n)));var wrapped=font.split(body,tw);int textHeight=wrapped.size()*font.lineHeight;int extra=show&&unlocked(n.id())?12:0;int sx=mx+12,sy=my-4;
  g.pose().pushPose();g.pose().translate(0,0,400);g.fill(sx-3,sy-3,sx+tw+3,sy+textHeight+extra+15,0xc0000000);g.drawString(font,title,sx,sy,show?(n.challenge()?0xffff80:0xffffff):(n.challenge()?0x808000:0x808080),false);int y=sy+12;for(var line:wrapped){g.drawString(font,line,sx,y,show?0xa0a0a0:0x705050,false);y+=font.lineHeight;}if(show&&unlocked(n.id()))g.drawString(font,taken(n),sx,sy+textHeight+extra+4,0x9090ff,false);g.pose().popPose();
 }
 @Override public void render(GuiGraphics g,int mx,int my,float partial){
  updateMode();renderBackground(g);int ox=Mth.clamp(Mth.floor(oldX+(x-oldX)*partial),MIN_X,MAX_X-1),oy=Mth.clamp(Mth.floor(oldY+(y-oldY)*partial),MIN_Y,maximumY()-1),vx=left()+16,vy=top()+17;boolean bright=Math.sin((double)(Util.getMillis()%600)/600*Math.PI*2)>.6;
  g.enableScissor(vx,vy,vx+224,vy+155);int col=(ox+288)>>4,row=(oy+288)>>4,rx=(ox+288)%16,ry=(oy+288)%16;var random=new Random();
  for(int yy=0;yy*16-ry<155;yy++){float shade=.6f-(row+yy)/25f*.3f;g.setColor(shade,shade,shade,1);for(int xx=0;xx*16-rx<224;xx++){var block=tile(random,col+xx,row+yy);var sprite=minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(block.defaultBlockState());g.blit(vx+xx*16-rx,vy+yy*16-ry,0,16,16,sprite);}}
  g.setColor(1,1,1,1);edges(g,ox,oy,vx,vy,bright);SourceAchievementCatalog561.Node hover=null;
  for(var n:SourceAchievementPolicy567.nodes(r196(),extras())){int nx=n.x()*24-ox,ny=n.y()*24-oy;if(nx< -24||ny< -24||nx>224||ny>155)continue;boolean done=unlocked(n.id()),available=SourceAchievementRules561.canShow(n.id(),unlocked(),r196());float shade=done?1:available?(bright?.8f:.6f):.3f;g.setColor(shade,shade,shade,1);g.blit(FRAME,vx+nx-2,vy+ny-2,n.challenge()?26:0,202,26,26);g.flush();if(!done&&!available)g.setColor(.1f,.1f,.1f,1);item(g,n,vx+nx+3,vy+ny+3);g.flush();g.setColor(1,1,1,1);if(mx>=vx&&my>=vy&&mx<vx+224&&my<vy+155&&mx>=vx+nx&&mx<=vx+nx+22&&my>=vy+ny&&my<=vy+ny+22)hover=n;}
  g.flush();g.disableScissor();g.setColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();g.blit(FRAME,left(),top(),0,0,256,202);g.drawString(font,title,left()+15,top()+5,0x404040,false);super.render(g,mx,my,partial);if(hover!=null)tooltip(g,hover,mx,my);
 }
}
