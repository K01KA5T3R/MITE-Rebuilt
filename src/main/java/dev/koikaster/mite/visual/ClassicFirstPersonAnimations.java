package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.koikaster.mite.MiteReforged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** Vanilla 1.6.4 bfj.a(float), normal flat-item branch and its 1/16 extrusion origin.
 * Standard cubes have their own source transform; generated block icons use
 * this same flat path. Custom models and new use actions stay native.
 * All timing comes from the existing renderer/player; this class writes only its pose stack.
 */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicFirstPersonAnimations {
 // 1.20.1 item/handheld in baked model units.
 private static final Vector3f RIGHT_ROTATION = new Vector3f(0, -90, 25);
 private static final Vector3f LEFT_ROTATION = new Vector3f(0, 90, -25);
 private static final Vector3f HANDHELD_TRANSLATION = new Vector3f(1.13f, 3.2f, 1.13f).mul(1f / 16f);
 private static final Vector3f HANDHELD_SCALE = new Vector3f(.68f);
 private static final Vector3f NO_EXTRA_ROTATION = new Vector3f();
 private static final float DISPLAY_TOLERANCE = .000001f;
 private static boolean failed;
 public static void resetVisualFailure(){failed=false;ClassicBlockHandRenderer.reset();ClassicMapHandRenderer.reset();}
 @SubscribeEvent(priority=EventPriority.LOW)
 public static void render(RenderHandEvent event){
  if(failed||!ClassicVisuals.firstPerson())return;
  var mc=Minecraft.getInstance();var player=mc.player;var stack=event.getItemStack();
  if(player==null||player.isScoping()||player.isAutoSpinAttack())return;
  if(stack.isEmpty())return;
  if(stack.getItem() instanceof BlockItem){ClassicBlockHandRenderer.render(event);if(event.isCanceled())return;}
  var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
  if(!id.getNamespace().equals("minecraft")&&!id.getNamespace().equals(MiteReforged.ID))return;
  if(stack.is(Items.FILLED_MAP)||stack.is(Items.CROSSBOW)||stack.is(Items.TRIDENT)||stack.is(Items.SPYGLASS))return;
  if(net.minecraftforge.client.extensions.common.IClientItemExtensions.of(stack)!=net.minecraftforge.client.extensions.common.IClientItemExtensions.DEFAULT)return;
  var hand=event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
  var display=hand==HumanoidArm.RIGHT?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
  boolean originalSword=stack.getItem() instanceof SwordItem && isOriginalSword(id);
  int modelSeed=originalSword?player.getId()+display.ordinal():player.getId();
  var model=mc.getItemRenderer().getModel(stack,mc.level,player,modelSeed);
  if(model.isGui3d()||model.isCustomRenderer())return;
  if(originalSword){
   // The native renderer applies custom hand transforms. The classic geometry bypasses them.
   if(model.getClass()!=SimpleBakedModel.class)return;
   var transform=model.getTransforms().getTransform(display);
   var rotation=hand==HumanoidArm.RIGHT?RIGHT_ROTATION:LEFT_ROTATION;
   if(!transform.rotation.equals(rotation,DISPLAY_TOLERANCE)
       ||!transform.translation.equals(HANDHELD_TRANSLATION,DISPLAY_TOLERANCE)
       ||!transform.scale.equals(HANDHELD_SCALE,DISPLAY_TOLERANCE)
       ||!transform.rightRotation.equals(NO_EXTRA_ROTATION,DISPLAY_TOLERANCE))return;
  }
  var passes=ClassicFlatItemRenderer.prepare(model,stack);if(passes==null)return;
  boolean using=player.isUsingItem()&&player.getUseItemRemainingTicks()>0&&player.getUsedItemHand()==event.getHand();
  var action=using?stack.getUseAnimation():UseAnim.NONE;
  if(action!=UseAnim.NONE&&action!=UseAnim.EAT&&action!=UseAnim.DRINK&&action!=UseAnim.BLOCK&&action!=UseAnim.BOW)return;
  int side=hand==HumanoidArm.RIGHT?1:-1;var pose=new PoseStack();
  pose.last().pose().set(event.getPoseStack().last().pose());pose.last().normal().set(event.getPoseStack().last().normal());
  pose.pushPose();
  try{
   if(id.getNamespace().equals(MiteReforged.ID))frameBossItem(pose,side,id.getPath(),action);
   transform(pose,side,event.getSwingProgress(),event.getEquipProgress(),action,using,player.getUseItemRemainingTicks(),stack.getUseDuration(),event.getPartialTick());
   if(stack.getItem() instanceof FishingRodItem)pose.mulPose(Axis.YP.rotationDegrees(180)); // original xv.o_()
   // bfj.a(EntityLiving,ItemStack,int): flat icon extrusion, in original coordinates.
   pose.translate(0,-.3f,0);pose.scale(1.5f,1.5f,1.5f);
   pose.mulPose(Axis.YP.rotationDegrees(side*50));pose.mulPose(Axis.ZP.rotationDegrees(side*335));
   pose.translate(side*-.9375f,-.0625f,0);
   // Literal original mesh, including half-texel side UVs and outward normals.
   // Mirror the completed original geometry for modern left-hand support.
   ClassicFlatItemRenderer.render(passes,stack,pose,event.getMultiBufferSource(),event.getPackedLight(),OverlayTexture.NO_OVERLAY,side<0);
   event.setCanceled(true);
  }catch(RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().error("Classic hand renderer unavailable; retaining native rendering",error);}
  finally{pose.popPose();}
 }
 private static boolean isOriginalSword(ResourceLocation id){
  if(id.getNamespace().equals("minecraft")){
   return id.getPath().equals("iron_sword")||id.getPath().equals("golden_sword");
  }
  return switch(id.getPath()){
   case "iron_sword","gold_sword","copper_sword","silver_sword","mithril_sword",
        "adamantium_sword","rusted_iron_sword","ancient_metal_sword" -> true;
   default -> false;
  };
 }
 public static void transform(PoseStack pose,int side,float swing,float equip,UseAnim action,boolean using,int remaining,int duration,float partial){
  if(using&&(action==UseAnim.EAT||action==UseAnim.DRINK)){
   float left=remaining-partial+1,progress=1-left/Math.max(1,duration);
   float power=1-progress;power=power*power*power;power=power*power*power;power=power*power*power;float eased=1-power;
   pose.translate(0,Math.abs(Mth.cos(left/4*(float)Math.PI)*.1f)*(progress>.2f?1:0),0);
   pose.translate(side*eased*.6f,-eased*.5f,0);
   pose.mulPose(Axis.YP.rotationDegrees(side*eased*90));pose.mulPose(Axis.XP.rotationDegrees(eased*10));pose.mulPose(Axis.ZP.rotationDegrees(side*eased*30));
  }else if(!using){
   pose.translate(side*-Mth.sin(Mth.sqrt(swing)*(float)Math.PI)*.4f,Mth.sin(Mth.sqrt(swing)*(float)Math.PI*2)*.2f,-Mth.sin(swing*(float)Math.PI)*.2f);
  }
  pose.translate(side*.56f,-.52f-equip*.6f,-.72f);pose.mulPose(Axis.YP.rotationDegrees(side*45));
  float squared=Mth.sin(swing*swing*(float)Math.PI),root=Mth.sin(Mth.sqrt(swing)*(float)Math.PI);
  pose.mulPose(Axis.YP.rotationDegrees(side*-squared*20));pose.mulPose(Axis.ZP.rotationDegrees(side*-root*20));pose.mulPose(Axis.XP.rotationDegrees(-root*80));
  pose.scale(.4f,.4f,.4f);
  if(using&&action==UseAnim.BLOCK){
   pose.translate(side*-.5f,.2f,0);pose.mulPose(Axis.YP.rotationDegrees(side*30));pose.mulPose(Axis.XP.rotationDegrees(-80));pose.mulPose(Axis.YP.rotationDegrees(side*60));
  }else if(using&&action==UseAnim.BOW){
   pose.mulPose(Axis.ZP.rotationDegrees(side*-18));pose.mulPose(Axis.YP.rotationDegrees(side*-12));pose.mulPose(Axis.XP.rotationDegrees(-8));pose.translate(side*-.9f,.2f,0);
   float elapsed=duration-(remaining-partial+1),pull=elapsed/20;pull=Math.min(1,(pull*pull+pull*2)/3);
   if(pull>.1f)pose.translate(0,Mth.sin((elapsed-.1f)*1.3f)*.01f*(pull-.1f),0);
   pose.translate(0,0,pull*.1f);pose.mulPose(Axis.ZP.rotationDegrees(side*-335));pose.mulPose(Axis.YP.rotationDegrees(side*-50));
   pose.translate(0,.5f,0);pose.scale(1,1,1+pull*.2f);pose.translate(0,-.5f,0);pose.mulPose(Axis.YP.rotationDegrees(side*50));pose.mulPose(Axis.ZP.rotationDegrees(side*335));
  }
 }
 private static void frameBossItem(PoseStack pose,int side,String item,UseAnim action){
  switch(item){
   case "stinger_boomerang" -> {pose.translate(-side*.034f,.063f,0);pose.scale(.76f,.76f,1);}
   case "fungal_spear","fungal_pickaxe" -> {pose.translate(-side*.025f,.036f,0);pose.scale(.86f,.86f,1);}
   case "high_energy_ender_eye","giant_call","monument_call","slime_crown","slime_cushion","fungal_ingot" -> {
    if(action==UseAnim.NONE)frameCompactItem(pose,side,.60f);
   }
   case "fungal_sprout","honey_bait","slime_hook","ninja_hood" -> {
    if(action==UseAnim.NONE)frameCompactItem(pose,side,.76f);
   }
   default -> { }
  }
 }
 private static void frameCompactItem(PoseStack pose,int side,float scale){
  // Keep the compact icon at the hand anchor instead of scaling the hand toward the camera center.
  pose.translate(side*(.49f-.56f*scale),-.47f+.52f*scale,0);
  pose.scale(scale,scale,1);
 }
 private ClassicFirstPersonAnimations(){}
}
