package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.koikaster.mite.MiteReforged;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.model.BakedModelWrapper;

/** Original bfj held-item profile and bfr standard-form cube rotation. Native
 * first-person context keeps translucent shader targets correct; custom models
 * and non-cubes retain the native renderer. Empty-hand parameters are already
 * identical to the original and need no replacement.
 */
public final class ClassicBlockHandRenderer {
 private static boolean failed;
 private static final Map<BakedModel,Boolean> CUBES=new WeakHashMap<>();
 public static void reset(){failed=false;}
 public static void render(RenderHandEvent event){
  if(failed)return;var mc=Minecraft.getInstance();var player=mc.player;var stack=event.getItemStack();if(player==null)return;
  var id=BuiltInRegistries.ITEM.getKey(stack.getItem());if(!id.getNamespace().equals("minecraft")&&!id.getNamespace().equals(MiteReforged.ID))return;
  if(IClientItemExtensions.of(stack)!=IClientItemExtensions.DEFAULT||stack.getUseAnimation()!=UseAnim.NONE)return;
  var model=mc.getItemRenderer().getModel(stack,mc.level,player,player.getId());if(model.isCustomRenderer()||!model.isGui3d())return;
  try{if(!CUBES.computeIfAbsent(model,ClassicBlockHandRenderer::cube))return;}
  catch(RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().warn("Classic cube model unavailable; retaining native rendering",error);return;}
  var arm=event.getHand()==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();int side=arm==HumanoidArm.RIGHT?1:-1;var pose=new PoseStack();pose.last().pose().set(event.getPoseStack().last().pose());pose.last().normal().set(event.getPoseStack().last().normal());pose.pushPose();
  try{
   boolean using=player.isUsingItem()&&player.getUsedItemHand()==event.getHand();
   ClassicFirstPersonAnimations.transform(pose,side,event.getSwingProgress(),event.getEquipProgress(),UseAnim.NONE,using,player.getUseItemRemainingTicks(),stack.getUseDuration(),event.getPartialTick());
   // bfr.a(Block,int,float), standard-form branch; ItemRenderer supplies the -0.5 center.
   pose.mulPose(Axis.YP.rotationDegrees(side*90));
   var context=side>0?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
   mc.getItemRenderer().render(stack,context,side<0,pose,event.getMultiBufferSource(),event.getPackedLight(),OverlayTexture.NO_OVERLAY,new Untransformed(model));
   event.setCanceled(true);
  }catch(RuntimeException error){failed=true;com.mojang.logging.LogUtils.getLogger().error("Classic cube-hand renderer unavailable; retaining native rendering",error);}
  finally{pose.popPose();}
 }
 private static boolean cube(BakedModel model){
  var faces=EnumSet.noneOf(Direction.class);var random=RandomSource.create(42);
  for(int face=-1;face<6;face++){
   random.setSeed(42);var direction=face<0?null:Direction.values()[face];
   for(var quad:model.getQuads(null,direction,random)){
    var data=quad.getVertices();int step=data.length/4,mask=0;var normal=quad.getDirection();
    for(int i=0;i<4;i++){
     float x=Float.intBitsToFloat(data[i*step]),y=Float.intBitsToFloat(data[i*step+1]),z=Float.intBitsToFloat(data[i*step+2]);
     if(Math.min(Math.abs(x),Math.abs(x-1))>.00001f||Math.min(Math.abs(y),Math.abs(y-1))>.00001f||Math.min(Math.abs(z),Math.abs(z-1))>.00001f)return false;
     float fixed=switch(normal.getAxis()){case X->x;case Y->y;case Z->z;};if(Math.abs(fixed-(normal.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0))>.00001f)return false;
     float u=normal.getAxis()==Direction.Axis.X?z:x,v=normal.getAxis()==Direction.Axis.Y?z:y;mask|=1<<((u>.5f?1:0)+(v>.5f?2:0));
    }
    if(mask!=15)return false;faces.add(normal);
   }
  }
  return faces.size()==6;
 }
 private static final class Untransformed extends BakedModelWrapper<BakedModel>{
  Untransformed(BakedModel model){super(model);}
  @Override public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left){return this;}
 }
 private ClassicBlockHandRenderer(){}
}
