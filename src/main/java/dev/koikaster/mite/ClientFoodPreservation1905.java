package dev.koikaster.mite;
import com.mojang.blaze3d.vertex.PoseStack;import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.blockentity.*;import net.minecraft.client.renderer.entity.ItemRenderer;import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;import net.minecraftforge.client.event.*;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientFoodPreservation1905 {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(FoodPreservation1905.ENTITY.get(),RackRenderer::new);}
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){event.register((stack,layer)->0xffd0b59a,FoodPreservation1905.SMOKED.keySet().stream().map(k->FoodPreservation1905.DRIED.get(k).get()).toArray(net.minecraft.world.item.Item[]::new));event.register((stack,layer)->0xffbca080,FoodPreservation1905.SMOKED.values().stream().map(v->v.get()).toArray(net.minecraft.world.item.Item[]::new));}
 public static final class RackRenderer implements BlockEntityRenderer<FoodPreservation1905.RackEntity> {
  private final ItemRenderer items;RackRenderer(BlockEntityRendererProvider.Context context){items=context.getItemRenderer();}
  public void render(FoodPreservation1905.RackEntity rack,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){for(int i=0;i<4;i++){var stack=rack.getItem(i);if(stack.isEmpty())continue;pose.pushPose();pose.translate((i&1)==0?.27:.73,.77,i<2?.27:.73);pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.38f,.38f,.38f);items.renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,rack.getLevel(),i);pose.popPose();}}
 }
 private ClientFoodPreservation1905(){}
}
