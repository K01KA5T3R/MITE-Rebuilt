package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
/** RenderInvisibleStalker binds the original Wight texture at
 * opacity0.05; no new PNG and no full entity invisibility flag. */
final class SourceInvisibleStalkerRenderer extends HumanoidMobRenderer<SourceInvisibleStalker,HumanoidModel<SourceInvisibleStalker>> {
 private static final class Model extends HumanoidModel<SourceInvisibleStalker>{
  Model(ModelPart root){super(root);}
  @Override public void setupAnim(SourceInvisibleStalker actor,float swing,float amount,float age,float yaw,float pitch){super.setupAnim(actor,swing,amount,age,yaw,pitch);float a=Mth.sin(attackTime*(float)Math.PI),b=Mth.sin((1-(1-attackTime)*(1-attackTime))*(float)Math.PI);leftArm.zRot=rightArm.zRot=0;rightArm.yRot=-(.1f-a*.6f);leftArm.yRot=.1f-a*.6f;leftArm.xRot=rightArm.xRot=-1.5707964f-(a*1.2f-b*.4f);rightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;leftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;rightArm.xRot+=Mth.sin(age*.067f)*.05f;leftArm.xRot-=Mth.sin(age*.067f)*.05f;}
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float alpha){super.renderToBuffer(pose,buffer,light,overlay,r,g,b,alpha*.05f);}
 }
 SourceInvisibleStalkerRenderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(ClassicMobModels.LEGACY_ZOMBIE)),.5f);}
 @Override public ResourceLocation getTextureLocation(SourceInvisibleStalker actor){return Content.id("textures/entity/wight.png");}
 @Override protected RenderType getRenderType(SourceInvisibleStalker actor,boolean visible,boolean translucent,boolean glowing){return RenderType.entityTranslucent(getTextureLocation(actor));}
}
