package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
/** Original bgl registers EntityWeb -> bgx(thrownWeb): one flat .5-scale camera-facing quad. */
public final class SourceWebRenderer extends EntityRenderer<SourceWebProjectile> {
 private static final ResourceLocation TEXTURE=new ResourceLocation(MiteReforged.ID,"textures/item/source_thrown_web.png");
 public SourceWebRenderer(EntityRendererProvider.Context context){super(context);}
 @Override public void render(SourceWebProjectile entity,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
  poses.pushPose();poses.scale(.5f,.5f,.5f);poses.mulPose(entityRenderDispatcher.cameraOrientation());
  var pose=poses.last();var consumer=buffers.getBuffer(RenderType.entityCutout(TEXTURE));
  vertex(consumer,pose,-.5f,-.25f,0,1,light);vertex(consumer,pose,.5f,-.25f,1,1,light);vertex(consumer,pose,.5f,.75f,1,0,light);vertex(consumer,pose,-.5f,.75f,0,0,light);
  poses.popPose();super.render(entity,yaw,partial,poses,buffers,light);
 }
 private static void vertex(VertexConsumer buffer,PoseStack.Pose pose,float x,float y,float u,float v,int light){buffer.vertex(pose.pose(),x,y,0).color(255,255,255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,1,0).endVertex();}
 @Override public ResourceLocation getTextureLocation(SourceWebProjectile entity){return TEXTURE;}
}
