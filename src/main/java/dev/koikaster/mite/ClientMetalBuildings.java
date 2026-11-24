package dev.koikaster.mite;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Original1.6.4 bbd/biv UV geometry and unchanged seven R195 strongbox textures. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientMetalBuildings {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(SourceMetalBuildings.BOX_ENTITY.get(),Chest::new);}
 @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{SourceMetalBuildings.DOORS.values().forEach(b->ItemBlockRenderTypes.setRenderLayer(b.get(),RenderType.cutout()));SourceMetalBuildings.BARS.values().forEach(b->ItemBlockRenderTypes.setRenderLayer(b.get(),RenderType.cutout()));});}
 public static final IClientItemExtensions ITEM=new IClientItemExtensions(){private BlockEntityWithoutLevelRenderer renderer;@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=Minecraft.getInstance();renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){@Override public void renderByItem(ItemStack item,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var block=((BlockItem)item.getItem()).getBlock();var box=new SourceMetalBuildings.BoxEntity(net.minecraft.core.BlockPos.ZERO,block.defaultBlockState());Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(box,pose,buffers,light,overlay);}};}return renderer;}};
 public static final class Chest implements BlockEntityRenderer<SourceMetalBuildings.BoxEntity> {
  final ModelPart lid,latch,base;
  public Chest(BlockEntityRendererProvider.Context context){var mesh=new MeshDefinition();var root=mesh.getRoot();root.addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14),PartPose.offset(1,7,15));root.addOrReplaceChild("latch",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-15,2,4,1),PartPose.offset(8,7,15));root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,19).addBox(0,0,0,14,10,14),PartPose.offset(1,6,1));var model=LayerDefinition.create(mesh,64,64).bakeRoot();lid=model.getChild("lid");latch=model.getChild("latch");base=model.getChild("base");}
  @Override public void render(SourceMetalBuildings.BoxEntity box,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(0,1,1);pose.scale(1,-1,-1);pose.translate(.5,.5,.5);var facing=box.getBlockState().getValue(ChestBlock.FACING);pose.mulPose(Axis.YP.rotationDegrees(switch(facing){case NORTH->180;case WEST->90;case EAST->-90;default->0;}));pose.translate(-.5,-.5,-.5);float openness=1-box.getOpenNess(partial);openness=1-openness*openness*openness;lid.xRot=latch.xRot=-openness*(float)Math.PI/2;var material=((SourceMetalBuildings.Strongbox)box.getBlockState().getBlock()).material;var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(Content.id("textures/entity/strongbox/"+material+"_single.png")));lid.render(pose,vertices,light,overlay);latch.render(pose,vertices,light,overlay);base.render(pose,vertices,light,overlay);pose.popPose();}
 }
 private ClientMetalBuildings(){}
}
