package dev.koikaster.mite;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bhz/bcm: original64x64 pigman skin, mirrored old limb UVs, raised zombie
 * arms even while neutral. No Piglin ears/head/nose/model is retained. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SourcePigmanClient {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Content.id("source_pigman"),"main");
 private static final ResourceLocation ITE_PIGMAN=Content.id("textures/entity/ite_zombie_pigman_lord.png");
 public static final ResourceLocation TEXTURE=Content.id("textures/entity/zombie_pigman.png");
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LAYER,()->{
  var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();
  root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,12,4),PartPose.offset(5,2,0));
  root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(1.9f,12,0));
  return LayerDefinition.create(mesh,64,64);
 });}
 public static final class Model extends HumanoidModel<ZombifiedPiglin> {
  public Model(ModelPart root){super(root);}
  @Override public void setupAnim(ZombifiedPiglin actor,float swing,float amount,float age,float yaw,float pitch){
   super.setupAnim(actor,swing,amount,age,yaw,pitch);
   float first=Mth.sin(attackTime*(float)Math.PI),second=Mth.sin((1-(1-attackTime)*(1-attackTime))*(float)Math.PI);
   rightArm.zRot=0;leftArm.zRot=0;rightArm.yRot=-(.1f-first*.6f);leftArm.yRot=.1f-first*.6f;rightArm.xRot=-1.5707964f;leftArm.xRot=-1.5707964f;
   rightArm.xRot-=first*1.2f-second*.4f;leftArm.xRot-=first*1.2f-second*.4f;
   rightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;leftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;rightArm.xRot+=Mth.sin(age*.067f)*.05f;leftArm.xRot-=Mth.sin(age*.067f)*.05f;
  }
 }
 public static final class Renderer extends HumanoidMobRenderer<ZombifiedPiglin,Model> {
  public Renderer(EntityRendererProvider.Context context){super(context,new Model(context.bakeLayer(LAYER)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<ZombifiedPiglin>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<ZombifiedPiglin>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),context.getModelManager()));}
  @Override public ResourceLocation getTextureLocation(ZombifiedPiglin actor){return IteEliteSpawns.isElite(actor)?ITE_PIGMAN:TEXTURE;}
 }
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(EntityType.ZOMBIFIED_PIGLIN,Renderer::new);}
 private SourcePigmanClient(){}
}
