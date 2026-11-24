package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
/** Original RenderEarthElemental/RenderFireElemental/RenderShadow with their ModelInvisibleStalker arm pose. */
public final class SourceStalkerRenderer extends HumanoidMobRenderer<Mobs.Undead,HumanoidModel<Mobs.Undead>> {
 private static final class Model extends HumanoidModel<Mobs.Undead>{
  Model(ModelPart root){super(root);}
  public void setupAnim(Mobs.Undead actor,float swing,float amount,float age,float yaw,float pitch){super.setupAnim(actor,swing,amount,age,yaw,pitch);float a=Mth.sin(attackTime*(float)Math.PI),b=Mth.sin((1-(1-attackTime)*(1-attackTime))*(float)Math.PI);leftArm.zRot=rightArm.zRot=0;rightArm.yRot=-(.1f-a*.6f);leftArm.yRot=.1f-a*.6f;leftArm.xRot=rightArm.xRot=-1.5707964f-(a*1.2f-b*.4f);rightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;leftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;rightArm.xRot+=Mth.sin(age*.067f)*.05f;leftArm.xRot-=Mth.sin(age*.067f)*.05f;}
 }
 public SourceStalkerRenderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(ClassicMobModels.LEGACY_ZOMBIE)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE_OUTER_ARMOR)),context.getModelManager()));addLayer(new EyesLayer<Mobs.Undead,HumanoidModel<Mobs.Undead>>(this){public RenderType renderType(){return RenderType.eyes(Content.id("textures/entity/earth_elemental/earth_elemental_glow.png"));}public void render(PoseStack pose,MultiBufferSource buffers,int light,Mobs.Undead actor,float swing,float amount,float partial,float age,float yaw,float pitch){if(!actor.earthBody())return;var mask=Content.id("textures/entity/earth_elemental/earth_elemental"+(magmaTexture(actor.originalEarth().type())?"_magma":"")+"_glow.png");getParentModel().renderToBuffer(pose,buffers.getBuffer(RenderType.eyes(mask)),15728640,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);}});}
 public static float opacity(Mobs.Undead actor){return Mobs.name(actor).equals("fire_elemental")?0:1;}
 protected RenderType getRenderType(Mobs.Undead actor,boolean visible,boolean translucent,boolean glowing){return opacity(actor)==0?null:super.getRenderType(actor,visible,translucent,glowing);}
 public static ResourceLocation texture(Mobs.Undead actor){if(!actor.earthBody())return Content.id("textures/entity/"+Mobs.name(actor)+".png");String role=switch(actor.originalEarth().type()){case 1->"stone/earth_elemental_stone";case 257->"stone/earth_elemental_stone_magma";case 49->"obsidian/earth_elemental_obsidian";case 305->"obsidian/earth_elemental_obsidian_magma";case 87->"netherrack/earth_elemental_netherrack";case 343->"netherrack/earth_elemental_netherrack_magma";case 121->"end_stone/earth_elemental_end_stone";case 82->"clay/earth_elemental_clay";case 172->"clay/earth_elemental_clay_hardened";default->"end_stone/earth_elemental_end_stone_magma";};return Content.id("textures/entity/earth_elemental/"+role+".png");}
 public static boolean magmaTexture(int kind){return switch(kind){case 1,49,87,121,82,172->false;default->true;};}
 public ResourceLocation getTextureLocation(Mobs.Undead actor){return texture(actor);}
 protected int getBlockLightLevel(Mobs.Undead actor,BlockPos pos){int light=super.getBlockLightLevel(actor,pos);return actor.earthBody()&&actor.originalEarth().magma()?Math.max(5,light):light;}
 protected int getSkyLightLevel(Mobs.Undead actor,BlockPos pos){int light=super.getSkyLightLevel(actor,pos);return actor.earthBody()&&actor.originalEarth().magma()?Math.max(5,light):light;}
}
