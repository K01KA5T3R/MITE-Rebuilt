package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.WitherSkeleton;
/** Native skeleton UV layout, source type1 scale; existing original texture. */
final class SourceWitherSkeletonRenderer extends HumanoidMobRenderer<WitherSkeleton,SkeletonModel<WitherSkeleton>> {
 SourceWitherSkeletonRenderer(EntityRendererProvider.Context context){super(context,new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),context.getModelManager()));}
 @Override public ResourceLocation getTextureLocation(WitherSkeleton mob){return new ResourceLocation("minecraft","textures/entity/skeleton/wither_skeleton.png");}
 @Override protected void scale(WitherSkeleton mob,PoseStack pose,float partial){pose.scale(1.2f,1.2f,1.2f);}
}
