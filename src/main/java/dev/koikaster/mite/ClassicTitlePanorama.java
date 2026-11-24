package dev.koikaster.mite;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** blt.b/c/a: 256px120degree panorama,64source samples,eight3tap passes. */
final class ClassicTitlePanorama implements AutoCloseable {
 private static final ResourceLocation[] FACES=new ResourceLocation[6];
 private static final CubeMap FALLBACK=new CubeMap(ClassicTitleScreen.asset("panorama"));
 static{for(int i=0;i<6;i++)FACES[i]=ClassicTitleScreen.asset("panorama_"+i+".png");}
 private RenderTarget first,second;private boolean unavailable;
 private void prepare(){
  if(first!=null)return;
  first=new TextureTarget(256,256,true,Minecraft.ON_OSX);second=new TextureTarget(256,256,false,Minecraft.ON_OSX);
  first.setFilterMode(9729);second.setFilterMode(9729);
 }
 void render(GuiGraphics graphics,int width,int height,float ticks){
  graphics.flush();if(unavailable){FALLBACK.render(Minecraft.getInstance(),Mth.sin(ticks/400f)*25+20,-ticks*.1f,1);return;}
  try{prepare();}catch(RuntimeException failure){unavailable=true;close();com.mojang.logging.LogUtils.getLogger().error("Classic title framebuffer unavailable",failure);FALLBACK.render(Minecraft.getInstance(),Mth.sin(ticks/400f)*25+20,-ticks*.1f,1);return;}
  var model=RenderSystem.getModelViewStack();RenderSystem.backupProjectionMatrix();model.pushPose();
  try{
   model.setIdentity();first.setClearColor(0,0,0,1);first.clear(Minecraft.ON_OSX);second.setClearColor(0,0,0,1);second.clear(Minecraft.ON_OSX);first.bindWrite(true);
   RenderSystem.setProjectionMatrix(new Matrix4f().setPerspective((float)Math.toRadians(120),1,.05f,10),VertexSorting.DISTANCE_TO_ORIGIN);
   model.mulPose(Axis.XP.rotationDegrees(180));RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
   RenderSystem.setShaderColor(1,1,1,1);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
   for(int sample=0;sample<64;sample++){
    model.pushPose();model.translate(((sample%8)/8f-.5f)/64f,((sample/8)/8f-.5f)/64f,0);
    model.mulPose(Axis.XP.rotationDegrees(Mth.sin(ticks/400f)*25+20));model.mulPose(Axis.YP.rotationDegrees(-ticks*.1f));
    for(int face=0;face<6;face++){
     model.pushPose();if(face==1)model.mulPose(Axis.YP.rotationDegrees(90));if(face==2)model.mulPose(Axis.YP.rotationDegrees(180));if(face==3)model.mulPose(Axis.YP.rotationDegrees(-90));if(face==4)model.mulPose(Axis.XP.rotationDegrees(90));if(face==5)model.mulPose(Axis.XP.rotationDegrees(-90));
     RenderSystem.applyModelViewMatrix();RenderSystem.setShaderTexture(0,FACES[face]);var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);int alpha=255/(sample+1);
     vertex(b,-1,-1,1,0,0,alpha);vertex(b,1,-1,1,1,0,alpha);vertex(b,1,1,1,1,1,alpha);vertex(b,-1,1,1,0,1,alpha);Tesselator.getInstance().end();model.popPose();
    }
    model.popPose();RenderSystem.colorMask(true,true,true,false);
   }
   model.setIdentity();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,256,256,0,-1,1),VertexSorting.ORTHOGRAPHIC_Z);
   RenderTarget source=first,dest=second;
   for(int pass=0;pass<8;pass++){
    dest.bindWrite(true);RenderSystem.setShaderTexture(0,source.getColorTextureId());
    for(int tap=0;tap<3;tap++){
     float u=(tap-1)/256f;var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);int alpha=(int)(255f/(tap+1));
     vertex(b,256,256,0,u,0,alpha);vertex(b,256,0,0,1+u,0,alpha);vertex(b,0,0,0,1+u,1,alpha);vertex(b,0,256,0,u,1,alpha);Tesselator.getInstance().end();
    }
    var swap=source;source=dest;dest=swap;
   }
   RenderSystem.colorMask(true,true,true,true);RenderSystem.restoreProjectionMatrix();
   // Modern RenderTarget texture origin differs from legacy framebuffer copies.
   // Adapt that origin here, rather than rotating the six source faces or UI.
   model.popPose();model.pushPose();RenderSystem.applyModelViewMatrix();Minecraft.getInstance().getMainRenderTarget().bindWrite(true);RenderSystem.setShaderTexture(0,source.getColorTextureId());
   float factor=120f/Math.max(width,height),u=height*factor/256,v=width*factor/256;
   var matrix=graphics.pose().last().pose();var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
   guiVertex(b,matrix,0,height,.5f-v,.5f-u);guiVertex(b,matrix,width,height,.5f+v,.5f-u);guiVertex(b,matrix,width,0,.5f+v,.5f+u);guiVertex(b,matrix,0,0,.5f-v,.5f+u);Tesselator.getInstance().end();
  }finally{
   RenderSystem.colorMask(true,true,true,true);RenderSystem.depthMask(true);RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);RenderSystem.restoreProjectionMatrix();model.popPose();RenderSystem.applyModelViewMatrix();Minecraft.getInstance().getMainRenderTarget().bindWrite(true);RenderSystem.enableCull();RenderSystem.enableDepthTest();
  }
 }
 private static void vertex(BufferBuilder b,float x,float y,float z,float u,float v,int alpha){b.vertex(x,y,z).uv(u,v).color(255,255,255,alpha).endVertex();}
 private static void guiVertex(BufferBuilder b,Matrix4f matrix,float x,float y,float u,float v){b.vertex(matrix,x,y,0).uv(u,v).color(255,255,255,255).endVertex();}
 @Override public void close(){if(first!=null)first.destroyBuffers();if(second!=null)second.destroyBuffers();first=null;second=null;}
}
