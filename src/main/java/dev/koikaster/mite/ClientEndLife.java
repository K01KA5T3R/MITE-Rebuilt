package dev.koikaster.mite;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** New End ecology uses original skin geometry and PNGs. Body-only GPU tint
 * leaves the original pig saddle, leash and other independent layers alone. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientEndLife {
 public static final ResourceLocation PIG=new ResourceLocation("minecraft:textures/entity/pig/pig.png"),COW=new ResourceLocation("minecraft:textures/entity/cow/cow.png"),EYES=new ResourceLocation("minecraft:textures/entity/enderman/enderman_eyes.png");
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerEntityRenderer(EndLifeContent.PIG.get(),ctx->new PigRenderer(ctx){@Override public void render(Pig pig,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){super.render(pig,yaw,partial,pose,tint(buffers,PIG,.64f,.45f,.95f),light);}});
  e.registerEntityRenderer(EndLifeContent.COW.get(),ctx->new CowRenderer(ctx){@Override public void render(Cow cow,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){super.render(cow,yaw,partial,pose,tint(buffers,COW,.70f,.45f,1),light);}});
  for(var type:java.util.List.of(EndLifeContent.PROWLER,EndLifeContent.SENTINEL))e.registerEntityRenderer(type.get(),ctx->new EndermanRenderer(ctx){
   @Override protected void scale(EnderMan mob,PoseStack pose,float partial){if(mob.getType()==EndLifeContent.SENTINEL.get())pose.scale(1.15f,1.1f,1.15f);}
   @Override public void render(EnderMan mob,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){super.render(mob,yaw,partial,pose,mob.getType()==EndLifeContent.SENTINEL.get()?type->type==RenderType.eyes(EYES)?new Tint(buffers.getBuffer(type),.35f,.9f,1):buffers.getBuffer(type):buffers,light);}
  });
 }
 static MultiBufferSource tint(MultiBufferSource source,ResourceLocation texture,float red,float green,float blue){return type->{var vertices=source.getBuffer(type);return type==RenderType.entityCutoutNoCull(texture)||type==RenderType.itemEntityTranslucentCull(texture)?new Tint(vertices,red,green,blue):vertices;};}
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){e.register((stack,layer)->0xffbca2f3,EndLifeContent.RAW_PORK.get(),EndLifeContent.COOKED_PORK.get(),EndLifeContent.RAW_BEEF.get(),EndLifeContent.COOKED_BEEF.get());}
 record Tint(VertexConsumer target,float red,float green,float blue) implements VertexConsumer {
  public VertexConsumer vertex(double x,double y,double z){target.vertex(x,y,z);return this;}
  public VertexConsumer color(int r,int g,int b,int a){target.color(Math.round(r*red),Math.round(g*green),Math.round(b*blue),a);return this;}
  public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
  public VertexConsumer overlayCoords(int a,int b){target.overlayCoords(a,b);return this;}
  public VertexConsumer uv2(int a,int b){target.uv2(a,b);return this;}
  public VertexConsumer normal(float x,float y,float z){target.normal(x,y,z);return this;}
  public void endVertex(){target.endVertex();}
  public void defaultColor(int r,int g,int b,int a){target.defaultColor(Math.round(r*red),Math.round(g*green),Math.round(b*blue),a);}
  public void unsetDefaultColor(){target.unsetDefaultColor();}
 }
 @Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
 public static final class Tooltips {
  @SubscribeEvent public static void tooltip(ItemTooltipEvent e){append(e,SourceTooltipLayout1447.extended(e,Screen.hasShiftDown()));}
  static void append(ItemTooltipEvent e,boolean shift){if(shift&&e.getItemStack().getItem() instanceof EndLifeContent.Meat meat)e.getToolTip().add(Component.translatable("tooltip.mite_reforged.end_meat_effect",Component.translatable(meat.effect.getDescriptionId()),meat.ticks/20).withStyle(net.minecraft.ChatFormatting.BLUE));}
 }
 private ClientEndLife(){}
}
