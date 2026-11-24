package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.blockentity.*;import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.ItemStack;import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.*;import net.minecraftforge.event.TickEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientMountedCrossbow1949 {
 private static final ResourceLocation RIG=new ResourceLocation(MiteReforged.ID,"block/mounted_crossbow_rig");
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(MountedCrossbow1949.BLOCK_ENTITY.get(),Renderer::new);event.registerEntityRenderer(MountedCrossbow1949.SEAT.get(),NoopRenderer::new);}
 @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(RIG);}
 public static final class Renderer implements BlockEntityRenderer<MountedCrossbow1949.MountEntity> {
  Renderer(BlockEntityRendererProvider.Context context){}
  public void render(MountedCrossbow1949.MountEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
   var level=entity.getLevel();if(level==null)return;var seat=MountedCrossbow1949.seat(level,entity.getBlockPos());float yaw=entity.getBlockState().getValue(MountedCrossbow1949.MountBlock.FACING).toYRot(),pitch=0;
   if(seat!=null){var player=seat.getFirstPassenger();yaw=player==null?seat.getYRot():player.getYRot();pitch=net.minecraft.util.Mth.clamp(player==null?seat.getXRot():player.getXRot(),-60,60);}
   pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-yaw));pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.translate(-.5,0,-.5);
   var minecraft=Minecraft.getInstance();var model=minecraft.getModelManager().getModel(RIG);minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.solid()),entity.getBlockState(),model,1,1,1,light,overlay);pose.popPose();
  }
 }
 @Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
 public static final class Input {
  private static int previous=-1,heartbeat;private static boolean held;
  @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var minecraft=Minecraft.getInstance();var seat=minecraft.player!=null&&minecraft.player.getVehicle() instanceof MountedCrossbow1949.Seat s?s:null;
   int current=seat==null?-1:seat.getId();boolean pressed=seat!=null&&minecraft.screen==null&&minecraft.options.keyUse.isDown();
   if(previous!=-1&&current!=previous&&held&&minecraft.getConnection()!=null)MountedCrossbowTransport1949.send(previous,false);
   if(current!=-1&&(current!=previous||pressed!=held||pressed&&++heartbeat>=4)){MountedCrossbowTransport1949.send(current,pressed);heartbeat=0;}
   previous=current;held=pressed;
  }
  @SubscribeEvent public static void action(InputEvent.InteractionKeyMappingTriggered event){var minecraft=Minecraft.getInstance();if(minecraft.player!=null&&minecraft.player.getVehicle() instanceof MountedCrossbow1949.Seat){event.setCanceled(true);event.setSwingHand(false);}}
  @SubscribeEvent public static void hand(RenderHandEvent event){var minecraft=Minecraft.getInstance();if(minecraft.player!=null&&minecraft.player.getVehicle() instanceof MountedCrossbow1949.Seat)event.setCanceled(true);}
 }
 private ClientMountedCrossbow1949(){}
}
