package dev.koikaster.mite;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** A real 64x32 classic UV layout; R195 skins are kept unmodified. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClassicMobModels {
 private static final java.util.Map<String,Boolean> LEGACY_SKINS=new java.util.concurrent.ConcurrentHashMap<>();
 @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->LEGACY_SKINS.clear());}
 public static final ModelLayerLocation LEGACY_ZOMBIE=new ModelLayerLocation(Content.id("classic_zombie"),"main");
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(LEGACY_ZOMBIE,()->{
  var mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0);var root=mesh.getRoot();
  root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,12,4),PartPose.offset(5,2,0));
  root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(1.9f,12,0));
  return LayerDefinition.create(mesh,64,32);
 });}
 public static boolean usesLegacySkin(String name){return LEGACY_SKINS.computeIfAbsent(name,ClassicMobModels::inspectLegacySkin);}
 private static boolean inspectLegacySkin(String name){
  var resource=net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(Content.id("textures/entity/"+name+".png"));
  if(resource.isEmpty())return false;
  try(var stream=resource.get().open();var image=com.mojang.blaze3d.platform.NativeImage.read(stream)){return image.getWidth()==64&&image.getHeight()==32;}
  catch(java.io.IOException|RuntimeException error){com.mojang.logging.LogUtils.getLogger().warn("Cannot inspect entity skin dimensions: "+name,error);return false;}
 }
 private static final class SourceModel extends HumanoidModel<Mobs.Undead> {
  private boolean tactical;
  SourceModel(ModelPart root){super(root);}
  @Override public void setupAnim(Mobs.Undead entity,float swing,float amount,float age,float yaw,float pitch){
   super.setupAnim(entity,swing,amount,age,yaw,pitch);
   tactical=entity.isTacticalWight();
   AnimationUtils.animateZombieArms(leftArm,rightArm,entity.isAggressive(),attackTime,age);
  }
  @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
      com.mojang.blaze3d.vertex.VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha){
   super.renderToBuffer(pose,vertices,light,overlay,tactical?red*.8f:red,tactical?green*.93f:green,blue,alpha);
  }
 }
 public static HumanoidMobRenderer<Mobs.Undead,HumanoidModel<Mobs.Undead>> renderer(EntityRendererProvider.Context context){
  return new HumanoidMobRenderer<Mobs.Undead,HumanoidModel<Mobs.Undead>>(context,new SourceModel(context.bakeLayer(LEGACY_ZOMBIE)),.5f){
   private final HumanoidModel<Mobs.Undead> classic=model,modern=new SourceModel(context.bakeLayer(ModelLayers.ZOMBIE));
   {addLayer(glow(this,"revenant"));addLayer(glow(this,"earth_elemental"));addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),context.getModelManager()));}
   @Override public void render(Mobs.Undead entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,int light){model=usesLegacySkin(Mobs.name(entity))?classic:modern;super.render(entity,yaw,partial,pose,buffer,light);}
   public ResourceLocation getTextureLocation(Mobs.Undead entity){return Content.id("textures/entity/"+Mobs.name(entity)+".png");}
  };
 }
 public static <T extends net.minecraft.world.entity.LivingEntity,M extends EntityModel<T>> EyesLayer<T,M> glow(net.minecraft.client.renderer.entity.RenderLayerParent<T,M> parent,String name){
  return glow(parent,name,null);
 }
 public static <T extends net.minecraft.world.entity.LivingEntity,M extends EntityModel<T>> EyesLayer<T,M> glow(net.minecraft.client.renderer.entity.RenderLayerParent<T,M> parent,String name,ResourceLocation fallback){
  var texture=Content.id("textures/entity/"+name+"_glow.png");var type=net.minecraft.client.renderer.RenderType.eyes(texture);
  return new EyesLayer<T,M>(parent){
   public net.minecraft.client.renderer.RenderType renderType(){return hasMask()?type:net.minecraft.client.renderer.RenderType.eyes(fallback);}
   private boolean hasMask(){return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();}
   @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,int light,T entity,float swing,float swingAmount,float partial,float age,float yaw,float pitch){
    if(!Mobs.name(entity).equals(name)||(!hasMask()&&fallback==null))return;
    super.render(pose,buffer,light,entity,swing,swingAmount,partial,age,yaw,pitch);
   }
  };
 }
 private ClassicMobModels(){}
}
