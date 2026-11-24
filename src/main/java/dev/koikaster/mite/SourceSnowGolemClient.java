package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Literal bcc boxes/UV/animation and bho head pose; the original64x64 sprite
 * is a copied resource, with no procedural/AI repaint. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SourceSnowGolemClient {
 public static final ResourceLocation TEXTURE=Content.id("textures/entity/source_snow_golem.png");
 static final ResourceLocation PUMPKIN=Content.id("item/source_snow_pumpkin"); private static final ModelLayerLocation LAYER=new ModelLayerLocation(Content.id("source_snow_golem"),"main");
 static LayerDefinition mesh(){var mesh=new MeshDefinition();var r=mesh.getRoot();var shrink=new CubeDeformation(-.5f);
  r.addOrReplaceChild("middle",CubeListBuilder.create().texOffs(0,16).addBox(-5,-10,-5,10,10,10,shrink),PartPose.offset(0,13,0));
  r.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,36).addBox(-6,-12,-6,12,12,12,shrink),PartPose.offset(0,24,0));
  r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8,shrink),PartPose.offset(0,4,0));
  r.addOrReplaceChild("right",CubeListBuilder.create().texOffs(32,0).addBox(-1,0,-1,12,2,2,shrink),PartPose.offset(0,6,0));
  r.addOrReplaceChild("left",CubeListBuilder.create().texOffs(32,0).addBox(-1,0,-1,12,2,2,shrink),PartPose.offset(0,6,0));return LayerDefinition.create(mesh,64,64);
 }
 public static final class Model extends EntityModel<SnowGolem> {
  final ModelPart root,middle,base,head,right,left;
  Model(ModelPart root){this.root=root;middle=root.getChild("middle");base=root.getChild("base");head=root.getChild("head");right=root.getChild("right");left=root.getChild("left");}
  @Override public void setupAnim(SnowGolem actor,float swing,float amount,float age,float yaw,float pitch){head.yRot=yaw/57.295776f;head.xRot=pitch/57.295776f;middle.yRot=yaw/57.295776f*.25f;float sin=SourceDragonTrig.sin(middle.yRot),cos=SourceDragonTrig.cos(middle.yRot);right.zRot=1;left.zRot=-1;right.yRot=middle.yRot;left.yRot=(float)Math.PI+middle.yRot;right.x=cos*5;right.z=-sin*5;left.x=-cos*5;left.z=sin*5;}
  @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,float red,float green,float blue,float alpha){root.render(pose,out,light,overlay,red,green,blue,alpha);}
 }
 public static final class Renderer extends MobRenderer<SnowGolem,Model> {
  Renderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(LAYER)),.5f);addLayer(new Pumpkin(this,context));}
  @Override public ResourceLocation getTextureLocation(SnowGolem actor){return TEXTURE;}
 }
 private static final class Pumpkin extends RenderLayer<SnowGolem,Model> {
  final EntityRendererProvider.Context context;Pumpkin(Renderer renderer,EntityRendererProvider.Context context){super(renderer);this.context=context;}
  @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,SnowGolem actor,float swing,float amount,float partial,float age,float yaw,float pitch){pose.pushPose();getParentModel().head.translateAndRotate(pose);pose.translate(0,-.34375f,0);pose.mulPose(Axis.YP.rotationDegrees(180)); /* bho90 + bfr render-type0 90; NONE keeps modern camera transforms out. */pose.scale(.625f,-.625f,.625f);context.getItemRenderer().render(new ItemStack(Blocks.CARVED_PUMPKIN),ItemDisplayContext.NONE,false,pose,SourceSnowHeadCull.buffers(buffers),light,OverlayTexture.NO_OVERLAY,net.minecraft.client.Minecraft.getInstance().getModelManager().getModel(PUMPKIN));pose.popPose();}
 }
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LAYER,SourceSnowGolemClient::mesh);}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(EntityType.SNOW_GOLEM,Renderer::new);}
 @SubscribeEvent public static void models(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event){event.register(PUMPKIN);} private SourceSnowGolemClient(){}
}
