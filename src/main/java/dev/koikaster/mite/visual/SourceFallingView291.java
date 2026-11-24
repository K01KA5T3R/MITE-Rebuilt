package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.koikaster.mite.SourceFallingTransport258;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;

/** bgo's centre/block-ID visibility and bfr's falling-cube pass: one centre
 * light sample, fixed six-face colours, no modern ambient occlusion. */
@OnlyIn(Dist.CLIENT)
public final class SourceFallingView291 {
 private static final double OFFSET=(double)(.98f/2f);
 public static BlockPos center(FallingBlockEntity body){return BlockPos.containing(body.getX(),body.getY()+OFFSET,body.getZ());}
 public static float shade(Direction face){return switch(face){case DOWN->.5f;case UP->1f;case NORTH,SOUTH->.8f;case WEST,EAST->.6f;};}
 public static boolean render(FallingBlockEntity body,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int nativeLight){
  var state=body.getBlockState();if(!SourceFallingTransport258.eligible(state))return false;
  var level=body.level();var pos=center(body);
  if(level.getBlockState(pos).is(state.getBlock())||state.getRenderShape()!=RenderShape.MODEL)return true;
  var dispatcher=Minecraft.getInstance().getBlockRenderer();BakedModel model=dispatcher.getBlockModel(state);long seed=state.getSeed(body.getStartPos());
  int light=LevelRenderer.getLightColor(level,pos);var random=RandomSource.create(seed);
  poses.pushPose();
  try{
   // R195 vertices start at centre-0.5; native entity position is centre-0.49.
   poses.translate(-.5,OFFSET-.5,-.5);
   for(var layer:model.getRenderTypes(state,RandomSource.create(seed),ModelData.EMPTY)){
    var vertices=buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(layer));
    for(var face:Direction.values()){random.setSeed(seed);for(var quad:model.getQuads(state,face,random,ModelData.EMPTY,layer))emit(poses,vertices,quad,light);}
    random.setSeed(seed);for(var quad:model.getQuads(state,null,random,ModelData.EMPTY,layer))emit(poses,vertices,quad,light);
   }
  }finally{poses.popPose();}
  return true;
 }
 private static void emit(PoseStack poses,com.mojang.blaze3d.vertex.VertexConsumer vertices,BakedQuad quad,int light){float value=shade(quad.getDirection());vertices.putBulkData(poses.last(),quad,new float[]{value,value,value,value},1,1,1,new int[]{light,light,light,light},OverlayTexture.NO_OVERLAY,false);}
 private SourceFallingView291(){}
}
