package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Skeleton;
/** Native64x32 skeleton UV layout; untouched R195 Lord skin and eye mask. */
final class SourceSkeletonRenderer extends HumanoidMobRenderer<Skeleton,SkeletonModel<Skeleton>> {
 SourceSkeletonRenderer(EntityRendererProvider.Context context){super(context,new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),context.getModelManager()));addLayer(ClassicMobModels.glow(this,"bone_lord"));addLayer(ClassicMobModels.glow(this,"ancient_bone_lord"));}
 public ResourceLocation getTextureLocation(Skeleton mob){if(mob instanceof SourceBoneLord)return Content.id("textures/entity/bone_lord.png");return new ResourceLocation("minecraft","textures/entity/skeleton/"+(mob instanceof SourceSkeleton source&&source.skeletonType()==1?"wither_skeleton":"skeleton")+".png");}
 @Override protected void scale(Skeleton mob,PoseStack pose,float partial){if(mob instanceof SourceSkeleton source&&source.skeletonType()==1)pose.scale(1.2f,1.2f,1.2f);}
}
