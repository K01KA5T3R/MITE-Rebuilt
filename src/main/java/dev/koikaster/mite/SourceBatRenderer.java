package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;
/** Existing native Bat geometry with unchanged supplied source PNG bytes. */
final class SourceBatRenderer extends BatRenderer {
 SourceBatRenderer(EntityRendererProvider.Context context,String texture){super(context);this.texture=texture;if(!texture.equals("bat")){var mask=Content.id("textures/entity/"+texture+"_glow.png");addLayer(new EyesLayer<>(this){@Override public RenderType renderType(){return RenderType.entityCutoutNoCull(mask);}});}}
 private final String texture;
 @Override public ResourceLocation getTextureLocation(Bat bat){return texture.equals("bat")?new ResourceLocation("minecraft","textures/entity/bat.png"):Content.id("textures/entity/"+texture+".png");}
 @Override protected void scale(Bat bat,PoseStack pose,float partial){super.scale(bat,pose,partial);if(bat instanceof SourceGiantVampireBat)pose.scale(2,2,2);}
}
