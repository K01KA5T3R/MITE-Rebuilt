package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientEvents {
 @SubscribeEvent public static void inventoryProgress(net.minecraftforge.client.event.ScreenEvent.Render.Post event){
  if(!(event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen screen))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isCreative())return;
  int progress=InventoryCrafting.progress(mc.player),total=InventoryCrafting.total(mc.player);if(total<=1&&progress==0)return;
  int x=screen.getGuiLeft()+134,y=screen.getGuiTop()+29;var graphics=event.getGuiGraphics();
  SourceCraftProgress.draw(graphics,x,y,progress,total,false);
  if(event.getMouseX()>=x&&event.getMouseX()<x+17&&event.getMouseY()>=y&&event.getMouseY()<y+14)
   graphics.renderTooltip(mc.font,net.minecraft.network.chat.Component.translatable(!SurvivalFood.hasFoodEnergy(mc.player)?"gui.mite_reforged.craft_hungry":progress==0?"gui.mite_reforged.craft_start":"gui.mite_reforged.craft_progress",Math.max(0,total-progress)/20f),event.getMouseX(),event.getMouseY());
 }
 @SubscribeEvent public static void hunger(RenderGuiOverlayEvent.Pre e){
  if(!e.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isCreative()||mc.player.isSpectator())return;
  var gui=(net.minecraftforge.client.gui.overlay.ForgeGui)mc.gui;
  if(!gui.shouldDrawSurvivalElements()||mc.player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity)return;
  e.setCanceled(true);int cap=PlayerState.foodCap(mc.player),food=mc.player.getFoodData().getFoodLevel();
  int x=mc.getWindow().getGuiScaledWidth()/2+91,y=mc.getWindow().getGuiScaledHeight()-gui.rightHeight;gui.rightHeight+=10;
  gui.setupOverlayRenderState(true,false);
  var g=e.getGuiGraphics();var tex=new ResourceLocation("minecraft:textures/gui/icons.png");
  // R195 avj.a(int,int): one visible nutrition row; satiation only controls its hungry jitter.
  boolean hunger=mc.player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);
  var random=net.minecraft.util.RandomSource.create(gui.getGuiTicks()*312871L);
  for(int i=0;i<10;i++){
   int dy=y;if(mc.player.getFoodData().getSaturationLevel()==0&&gui.getGuiTicks()%(food*3+1)==0)dy+=random.nextInt(3)-1;
   int dx=x-i*8-9;
   if(i<cap/2)g.blit(tex,dx,dy,hunger?133:16,27,9,9);
   if(i*2+1<food)g.blit(tex,dx,dy,hunger?88:52,27,9,9);
   else if(i*2+1==food)g.blit(tex,dx,dy,hunger?97:61,27,9,9);
  }
 }
 @SubscribeEvent public static void negativeExperience(RenderGuiOverlayEvent.Pre event){
  if(!event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()))return;
  var mc=Minecraft.getInstance();
  if(mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||mc.options.hideGui)return;
  int level=PlayerState.level(mc.player);
  if(level>=0||mc.player.jumpableVehicle()!=null)return;
  event.setCanceled(true);
  ((net.minecraftforge.client.gui.overlay.ForgeGui)mc.gui).setupOverlayRenderState(true,false);
  var g=event.getGuiGraphics();
  int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
  int start=width/2-91,bar=height-29;
  var icons=new ResourceLocation("minecraft:textures/gui/icons.png");
  g.blit(icons,start,bar,0,64,182,5);
  int progress=(int)(Rules.levelProgress(PlayerState.data(mc.player).getInt("Xp"),level)*183);
  if(progress>0)g.blit(icons,start,bar,0,69,progress,5);
  String label=Integer.toString(level);
  int x=(width-mc.font.width(label))/2,y=height-35;
  g.drawString(mc.font,label,x+1,y,0,false);
  g.drawString(mc.font,label,x-1,y,0,false);
  g.drawString(mc.font,label,x,y+1,0,false);
  g.drawString(mc.font,label,x,y-1,0,false);
  g.drawString(mc.font,label,x,y,0xFF1313,false);
 }
 private ClientEvents(){}
}
