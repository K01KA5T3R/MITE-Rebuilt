package dev.koikaster.mite.visual;
import com.mojang.blaze3d.vertex.PoseStack;import com.mojang.math.Axis;import dev.koikaster.mite.*;import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.entity.ItemRenderer;import net.minecraft.client.renderer.texture.OverlayTexture;import net.minecraft.core.Direction;import net.minecraft.util.RandomSource;import net.minecraft.world.entity.HumanoidArm;import net.minecraft.world.item.UseAnim;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.RenderHandEvent;import net.minecraftforge.eventbus.api.*;import net.minecraftforge.fml.common.Mod;
/** The torch's model follows the bow string; retain the source first-person pose for its custom mesh. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class TorchBowHand953 {
 private static boolean failed;
 @SubscribeEvent(priority=EventPriority.NORMAL) public static void render(RenderHandEvent e){
  if(failed||!ClassicVisuals.firstPerson()||!(e.getItemStack().getItem() instanceof TorchBow))return;
  var mc=Minecraft.getInstance();var player=mc.player;var stack=e.getItemStack();
  if(player==null||player.isScoping()||!player.isUsingItem()||player.getUseItem()!=stack||player.getUseItemRemainingTicks()<=0||player.getUsedItemHand()!=e.getHand())return;
  var arm=e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();int side=arm==HumanoidArm.RIGHT?1:-1;
  var pose=new PoseStack();pose.last().pose().set(e.getPoseStack().last().pose());pose.last().normal().set(e.getPoseStack().last().normal());pose.pushPose();
  try{
   ClassicFirstPersonAnimations.transform(pose,side,e.getSwingProgress(),e.getEquipProgress(),UseAnim.BOW,true,player.getUseItemRemainingTicks(),stack.getUseDuration(),e.getPartialTick());
   pose.translate(0,-.3f,0);pose.scale(1.5f,1.5f,1.5f);pose.mulPose(Axis.YP.rotationDegrees(side*50));pose.mulPose(Axis.ZP.rotationDegrees(side*335));pose.translate(side*-.9375f,-.0625f,0);
   var model=mc.getItemRenderer().getModel(stack,mc.level,player,player.getId());int count=0;
   for(var pass:model.getRenderPasses(stack,true))for(var type:pass.getRenderTypes(stack,true)){
    var out=ItemRenderer.getFoilBufferDirect(e.getMultiBufferSource(),type,true,stack.hasFoil());
    for(int face=-1;face<6;face++)for(var q:pass.getQuads(null,face<0?null:Direction.values()[face],RandomSource.create(42))){
     var data=q.getVertices();int stride=data.length/4;var n=q.getDirection().getNormal();
     for(int i=0;i<4;i++){
      int vertex=side>0?3-i:i,at=vertex*stride;float x=Float.intBitsToFloat(data[at]),y=Float.intBitsToFloat(data[at+1]),z=Float.intBitsToFloat(data[at+2]);
      int normal=stride>7?data[at+7]:0;float nx=normal==0?n.getX():(byte)(normal&255)/127f,ny=normal==0?n.getY():(byte)(normal>>8&255)/127f,nz=normal==0?n.getZ():(byte)(normal>>16&255)/127f;
      out.vertex(pose.last().pose(),side*(1-x),y,z-8.5f/16).color(1f,1f,1f,1f).uv(Float.intBitsToFloat(data[at+4]),Float.intBitsToFloat(data[at+5])).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(e.getPackedLight()).normal(pose.last().normal(),-side*nx,ny,nz).endVertex();
     }count++;
    }
   }
   if(count>0)e.setCanceled(true);
  }catch(RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().error("Torch bow hand model unavailable",error);}finally{pose.popPose();}
 }
 private TorchBowHand953(){}
}
