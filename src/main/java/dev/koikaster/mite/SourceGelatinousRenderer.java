package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
/** Source bhe two-pass slime model and RenderGelatinousCube squish scaling. */
public final class SourceGelatinousRenderer extends MobRenderer<SourceGelatinousCube,SlimeModel<SourceGelatinousCube>> {
 public SourceGelatinousRenderer(EntityRendererProvider.Context context){super(context,new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)),.25f);var outer=new SlimeModel<SourceGelatinousCube>(context.bakeLayer(ModelLayers.SLIME_OUTER));addLayer(new RenderLayer<>(this){public void render(PoseStack pose,MultiBufferSource buffers,int light,SourceGelatinousCube mob,float swing,float amount,float partial,float age,float yaw,float pitch){if(mob.isInvisible())return;getParentModel().copyPropertiesTo(outer);outer.prepareMobModel(mob,swing,amount,partial);outer.setupAnim(mob,swing,amount,age,yaw,pitch);outer.renderToBuffer(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(mob))),light,LivingEntityRenderer.getOverlayCoords(mob,0),1,1,1,1);}});}
 @Override public ResourceLocation getTextureLocation(SourceGelatinousCube mob){return Content.id("textures/entity/"+Mobs.name(mob)+".png");}
 @Override protected void scale(SourceGelatinousCube mob,PoseStack pose,float partial){float size=mob.sourceSize(),squish=(mob.previousSquishFactor+(mob.squishFactor-mob.previousSquishFactor)*partial)/(size*.5f+1),inverse=1/(squish+1);pose.scale(inverse*size,size/inverse,inverse*size);}
}
