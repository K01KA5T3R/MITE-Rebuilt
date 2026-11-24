package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Literal bch geometry/UV/animation and bhu scale. V060 removes profession
 * and biome layers; retained default source profession0 appearance is farmer. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SourceVillagerClient {
 public static final ResourceLocation TEXTURE=Content.id("textures/entity/source_villager.png");
 private static final ModelLayerLocation LAYER=new ModelLayerLocation(Content.id("source_villager"),"main");
 static LayerDefinition mesh(){
  var mesh=new MeshDefinition();var root=mesh.getRoot();
  var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-10,-4,8,10,8),PartPose.ZERO);
  head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(24,0).addBox(-1,-1,-6,2,4,2),PartPose.offset(0,-2,0));
  root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,20).addBox(-4,0,-3,8,12,6).texOffs(0,38).addBox(-4,0,-3,8,18,6,new CubeDeformation(.5f)),PartPose.ZERO);
  root.addOrReplaceChild("arms",CubeListBuilder.create().texOffs(44,22).addBox(-8,-2,-2,4,8,4).texOffs(44,22).addBox(4,-2,-2,4,8,4).texOffs(40,38).addBox(-4,2,-2,8,4,4),PartPose.offset(0,2,0));
  root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,22).addBox(-2,0,-2,4,12,4),PartPose.offset(-2,12,0));
  root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,22).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(2,12,0));
  return LayerDefinition.create(mesh,64,64);
 }
 public static final class Model extends EntityModel<Villager> {
  final ModelPart root,head,arms,right,left;
  Model(ModelPart root){this.root=root;head=root.getChild("head");arms=root.getChild("arms");right=root.getChild("right_leg");left=root.getChild("left_leg");}
  @Override public void setupAnim(Villager actor,float swing,float amount,float age,float yaw,float pitch){head.yRot=yaw/57.295776f;head.xRot=pitch/57.295776f;arms.y=3;arms.z=-1;arms.xRot=-.75f;right.xRot=Mth.cos(swing*.6662f)*1.4f*amount*.5f;left.xRot=Mth.cos(swing*.6662f+(float)Math.PI)*1.4f*amount*.5f;right.yRot=left.yRot=0;}
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,float red,float green,float blue,float alpha){root.render(pose,consumer,light,overlay,red,green,blue,alpha);}
 }
 public static final class Renderer extends MobRenderer<Villager,Model> {
  public Renderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(LAYER)),.5f);}
  @Override protected void scale(Villager actor,PoseStack pose,float partial){float scale=.9375f;if(actor.isBaby()){scale*=.5;shadowRadius=.25f;}else shadowRadius=.5f;pose.scale(scale,scale,scale);}
  @Override public ResourceLocation getTextureLocation(Villager actor){return TEXTURE;}
 }
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LAYER,SourceVillagerClient::mesh);}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(EntityType.VILLAGER,Renderer::new);}
 private SourceVillagerClient(){}
}
