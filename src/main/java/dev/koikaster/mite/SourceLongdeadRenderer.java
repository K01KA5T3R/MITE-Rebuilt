package dev.koikaster.mite;

import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Native64x32 skeleton UVs bind the untouched original longdead skins. */
final class SourceLongdeadRenderer extends HumanoidMobRenderer<SourceLongdead,SkeletonModel<SourceLongdead>> {
 SourceLongdeadRenderer(EntityRendererProvider.Context context){
  super(context,new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)),.5f);
  addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),context.getModelManager()));
  addLayer(ClassicMobModels.glow(this,"longdead_guardian"));
 }
 public ResourceLocation getTextureLocation(SourceLongdead mob){return Content.id("textures/entity/"+(mob.guardian()?"longdead_guardian":"longdead")+".png");}
}
