package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.koikaster.mite.MiteReforged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Literal R195 bfj.a(float) two-handed map branch. Map contents and player
 * arm rendering retain native providers; modern offhand/custom skins fall back.
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicMapHandRenderer {
 private static final RenderType BACKGROUND=RenderType.text(new ResourceLocation("minecraft","textures/map/map_background.png"));
 private static boolean failed;
 public static void reset(){failed=false;}
 public static boolean supportsSkin(ResourceLocation skin,String model){
  return "default".equals(model)&&(skin.equals(ClassicPlayerAppearance.TEXTURE)||(skin.getNamespace().equals("minecraft")&&(skin.getPath().startsWith("textures/entity/player/")||skin.getPath().equals("textures/entity/steve.png"))));
 }
 @SubscribeEvent public static void render(RenderHandEvent event){
  if(failed||!ClassicVisuals.firstPerson()||event.getHand()!=InteractionHand.MAIN_HAND||!event.getItemStack().is(Items.FILLED_MAP))return;
  var mc=Minecraft.getInstance();var player=mc.player;
  if(player==null||player.isScoping()||player.isAutoSpinAttack()||!player.getOffhandItem().isEmpty()||!supportsSkin(player.getSkinTextureLocation(),player.getModelName()))return;
  var renderer=mc.getEntityRenderDispatcher().getRenderer(player);if(renderer.getClass()!=PlayerRenderer.class)return;
  var pose=new PoseStack();pose.last().pose().set(event.getPoseStack().last().pose());pose.last().normal().set(event.getPoseStack().last().normal());
  try{
   base(pose,event.getSwingProgress(),event.getEquipProgress(),event.getInterpolatedPitch());
   if(!player.isInvisible())for(int side:new int[]{-1,1}){pose.pushPose();arm(pose,side);((PlayerRenderer)renderer).renderRightHand(pose,event.getMultiBufferSource(),event.getPackedLight(),player);pose.popPose();}
   paper(pose,event.getSwingProgress());
   var vertices=event.getMultiBufferSource().getBuffer(BACKGROUND);var matrix=pose.last().pose();
   vertices.vertex(matrix,-7,135,0).color(255,255,255,255).uv(0,1).uv2(event.getPackedLight()).endVertex();
   vertices.vertex(matrix,135,135,0).color(255,255,255,255).uv(1,1).uv2(event.getPackedLight()).endVertex();
   vertices.vertex(matrix,135,-7,0).color(255,255,255,255).uv(1,0).uv2(event.getPackedLight()).endVertex();
   vertices.vertex(matrix,-7,-7,0).color(255,255,255,255).uv(0,0).uv2(event.getPackedLight()).endVertex();
   var id=MapItem.getMapId(event.getItemStack());var data=MapItem.getSavedData(id,mc.level);
   if(id!=null&&data!=null)mc.gameRenderer.getMapRenderer().render(pose,event.getMultiBufferSource(),id,data,false,event.getPackedLight());
   event.setCanceled(true);
  }catch(RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().warn("Classic map rendering unavailable; retaining native rendering",error);}
 }
 public static void base(PoseStack pose,float swing,float equip,float pitch){
  float root=Mth.sqrt(swing),wave=Mth.sin(root*(float)Math.PI);
  pose.translate(-wave*.4f,Mth.sin(root*(float)Math.PI*2)*.2f,-Mth.sin(swing*(float)Math.PI)*.2f);
  float tilt=1-pitch/45+.1f;tilt=Mth.clamp(tilt,0,1);tilt=-Mth.cos(tilt*(float)Math.PI)*.5f+.5f;
  pose.translate(0,-equip*1.2f-tilt*.5f+.04f,-.9f*.8f);pose.mulPose(Axis.YP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(tilt*-85));
 }
 public static void arm(PoseStack pose,int side){
  pose.translate(0,-.6f,1.1f*side);pose.mulPose(Axis.XP.rotationDegrees(-45*side));pose.mulPose(Axis.ZP.rotationDegrees(-90));pose.mulPose(Axis.ZP.rotationDegrees(59));pose.mulPose(Axis.YP.rotationDegrees(-65*side));
 }
 public static void paper(PoseStack pose,float swing){
  float squared=Mth.sin(swing*swing*(float)Math.PI),root=Mth.sin(Mth.sqrt(swing)*(float)Math.PI);
  pose.mulPose(Axis.YP.rotationDegrees(-squared*20));pose.mulPose(Axis.ZP.rotationDegrees(-root*20));pose.mulPose(Axis.XP.rotationDegrees(-root*80));pose.scale(.38f,.38f,.38f);pose.mulPose(Axis.YP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(180));pose.translate(-1,-1,0);pose.scale(.015625f,.015625f,.015625f);
 }
 private ClassicMapHandRenderer(){}
}
