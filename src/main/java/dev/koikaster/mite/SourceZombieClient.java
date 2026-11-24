package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bhz/bcm/bci, including the old villager head/armor geometry and64x64 UVs. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SourceZombieClient {
 private static final ResourceLocation ITE_ZOMBIE=Content.id("textures/entity/ite_zombie_lord.png");
 public static final ResourceLocation NORMAL=Content.id("textures/entity/source_zombie.png"),VILLAGER=Content.id("textures/entity/source_zombie_villager.png");
 private static ModelLayerLocation layer(boolean villager,int armor){return new ModelLayerLocation(Content.id("source_zombie_"+(villager?"villager":"normal")+"_"+armor),"main");}
 private static LayerDefinition mesh(boolean villager,int armor){
  float inflate=armor==1?.5f:armor==2?1:0;var d=new CubeDeformation(inflate);var mesh=HumanoidModel.createMesh(d,0);var root=mesh.getRoot();
  root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,12,4,d),PartPose.offset(5,2,0));root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,0,-2,4,12,4,d),PartPose.offset(1.9f,12,0));
  if(villager){var head=armor==0?CubeListBuilder.create().texOffs(0,32).addBox(-4,-10,-4,8,10,8,d).texOffs(24,32).addBox(-1,-3,-6,2,4,2,d):CubeListBuilder.create().texOffs(0,0).addBox(-4,-10,-4,8,6,8,d);root.addOrReplaceChild("head",head,PartPose.ZERO);}
  return LayerDefinition.create(mesh,64,armor==0?64:32);
 }
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event){for(boolean v:new boolean[]{false,true})for(int armor=0;armor<=2;armor++){boolean role=v;int part=armor;event.registerLayerDefinition(layer(v,armor),()->mesh(role,part));}}
 public static final class Model extends HumanoidModel<Zombie> {
  public Model(ModelPart root){super(root);}
  @Override public void setupAnim(Zombie actor,float swing,float amount,float age,float yaw,float pitch){super.setupAnim(actor,swing,amount,age,yaw,pitch);float first=Mth.sin(attackTime*(float)Math.PI),second=Mth.sin((1-(1-attackTime)*(1-attackTime))*(float)Math.PI);rightArm.zRot=0;leftArm.zRot=0;rightArm.yRot=-(.1f-first*.6f);leftArm.yRot=.1f-first*.6f;rightArm.xRot=-1.5707964f;leftArm.xRot=-1.5707964f;rightArm.xRot-=first*1.2f-second*.4f;leftArm.xRot-=first*1.2f-second*.4f;rightArm.zRot+=Mth.cos(age*.09f)*.05f+.05f;leftArm.zRot-=Mth.cos(age*.09f)*.05f+.05f;rightArm.xRot+=Mth.sin(age*.067f)*.05f;leftArm.xRot-=Mth.sin(age*.067f)*.05f;}
 }
 static boolean villager(Zombie actor){return actor instanceof SourceZombieOwner owner?owner.sourceVillager():actor instanceof net.minecraft.world.entity.monster.ZombieVillager;}
 public static final class Armor extends HumanoidArmorLayer<Zombie,Model,Model> {
  private final boolean villager;Armor(Renderer renderer,EntityRendererProvider.Context c,boolean role){super(renderer,new Model(c.bakeLayer(layer(role,1))),new Model(c.bakeLayer(layer(role,2))),c.getModelManager());villager=role;}
  @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,Zombie actor,float swing,float amount,float partial,float age,float yaw,float pitch){if(villager(actor)==villager)super.render(pose,buffer,light,actor,swing,amount,partial,age,yaw,pitch);}
 }
 public static final class Renderer extends HumanoidMobRenderer<Zombie,Model> {
  private final Model normal,villager;
  public Renderer(EntityRendererProvider.Context c){super(c,new Model(c.bakeLayer(layer(false,0))),.5f);normal=model;villager=new Model(c.bakeLayer(layer(true,0)));addLayer(new Armor(this,c,false));addLayer(new Armor(this,c,true));}
  @Override public void render(Zombie actor,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){model=villager(actor)?villager:normal;super.render(actor,yaw,partial,pose,buffer,light);}
  @Override protected void setupRotations(Zombie actor,PoseStack pose,float age,float yaw,float partial){if(actor instanceof SourceZombieOwner owner&&owner.sourceConverting())yaw+=Mth.cos(actor.tickCount*3.25f)*(float)Math.PI*.25f;super.setupRotations(actor,pose,age,yaw,partial);}
  @Override public ResourceLocation getTextureLocation(Zombie actor){return IteEliteSpawns.isElite(actor)?ITE_ZOMBIE:villager(actor)?VILLAGER:NORMAL;}
 }
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(EntityType.ZOMBIE,Renderer::new);event.registerEntityRenderer(EntityType.ZOMBIE_VILLAGER,Renderer::new);}
 private SourceZombieClient(){}
}
