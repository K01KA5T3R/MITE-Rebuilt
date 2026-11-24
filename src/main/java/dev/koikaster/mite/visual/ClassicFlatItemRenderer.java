package dev.koikaster.mite.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Adapts a native generated icon's sprites/tints to the complete original mesh.
 * Retains native render passes, shader targets, item colors and foil consumers.
 * Nonstandard geometry is returned to the native renderer before any draw.
 */
public final class ClassicFlatItemRenderer {
 public record Plane(TextureAtlasSprite sprite,int tint){}
 public record Pass(RenderType type,List<Plane> planes){}
 private static final Map<BakedModel,List<Plane>> GEOMETRY=new WeakHashMap<>();
 public static List<Pass> prepare(BakedModel model,ItemStack stack){
  if(stack.is(Items.COMPASS)||stack.is(Items.RECOVERY_COMPASS)||stack.is(Items.CLOCK))return null;
  return prepareGround(model,stack,true);
 }
 static List<Pass> prepareGround(BakedModel model,ItemStack stack,boolean direct){
  var passes=new ArrayList<Pass>();
  for(var layer:model.getRenderPasses(stack,direct)){
   List<Plane> planes;
   if(GEOMETRY.containsKey(layer))planes=GEOMETRY.get(layer);else{planes=inspect(layer);GEOMETRY.put(layer,planes);}
   if(planes==null)return null;
   for(var type:layer.getRenderTypes(stack,direct))passes.add(new Pass(type,planes));
  }
  return passes.isEmpty()?null:passes;
 }
 private static List<Plane> inspect(BakedModel model){
  var quads=model.getQuads(null,null,RandomSource.create(42));var front=new ArrayList<Plane>();var back=new ArrayList<Plane>();
  for(var quad:quads){
   var data=quad.getVertices();int stride=data.length/4;
   for(int i=0;i<4;i++){
    float x=Float.intBitsToFloat(data[i*stride]),y=Float.intBitsToFloat(data[i*stride+1]),z=Float.intBitsToFloat(data[i*stride+2]);
    if(x<-.00001f||x>1.00001f||y<-.00001f||y>1.00001f||Math.min(Math.abs(z-7.5f/16),Math.abs(z-8.5f/16))>.00001f)return null;
   }
   if(quad.getDirection()==Direction.NORTH||quad.getDirection()==Direction.SOUTH){
    if(!fullPlane(quad))return null;
    (quad.getDirection()==Direction.SOUTH?front:back).add(new Plane(quad.getSprite(),quad.isTinted()?quad.getTintIndex():-1));
   }
  }
  if(front.isEmpty()||front.size()!=back.size()||!new HashSet<>(front).equals(new HashSet<>(back)))return null;
  return List.copyOf(front);
 }
 private static boolean fullPlane(BakedQuad quad){
  var data=quad.getVertices();int stride=data.length/4,mask=0;var sprite=quad.getSprite();float uvTolerance=Math.max(.006f,.5f*sprite.uvShrinkRatio()+.0011f);
  for(int i=0;i<4;i++){
   float x=Float.intBitsToFloat(data[i*stride]),y=Float.intBitsToFloat(data[i*stride+1]);
   if(Math.min(Math.abs(x),Math.abs(x-1))>.00001f||Math.min(Math.abs(y),Math.abs(y-1))>.00001f)return false;
   float u=(Float.intBitsToFloat(data[i*stride+4])-sprite.getU0())/(sprite.getU1()-sprite.getU0());
   float v=(Float.intBitsToFloat(data[i*stride+5])-sprite.getV0())/(sprite.getV1()-sprite.getV0());
   if(Math.abs(u-x)>uvTolerance||Math.abs(v-(1-y))>uvTolerance)return false;
   mask|=1<<((x>.5f?1:0)+(y>.5f?2:0));
  }
  return mask==15;
 }
 public static void render(List<Pass> passes,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean mirrored){
  var mc=Minecraft.getInstance();var matrix=pose.last();
  // R195 applies the separate icon foil only to the first icon pass. Retain
  // native providers for layered models until their compositing is qualified.
  boolean hasFoil=ClassicPotionAppearance.hasFoil(stack);
  var glint=hasFoil&&passes.size()==1&&passes.get(0).planes().size()==1?ClassicFlatItemGlint.acquire(buffers):null;
  for(var pass:passes){
   var out=ItemRenderer.getFoilBufferDirect(buffers,pass.type(),true,hasFoil&&glint==null);
   for(var plane:pass.planes()){
    var sprite=plane.sprite();int tint=plane.tint()<0?0xffffff:mc.getItemColors().getColor(stack,plane.tint());float r=(tint>>16&255)/255f,g=(tint>>8&255)/255f,b=(tint&255)/255f;
    ClassicFlatItemMesh.Vertex vertex=(x,y,z,u,v,nx,ny,nz)->out.vertex(matrix.pose(),x,y,z).color(r,g,b,1).uv(u,v).overlayCoords(overlay).uv2(light).normal(matrix.normal(),nx,ny,nz).endVertex();
    if(mirrored)ClassicFlatItemMesh.emitMirrored(vertex,sprite.getU1(),sprite.getV0(),sprite.getU0(),sprite.getV1(),sprite.contents().width(),sprite.contents().height());
    else ClassicFlatItemMesh.emit(vertex,sprite.getU1(),sprite.getV0(),sprite.getU0(),sprite.getV1(),sprite.contents().width(),sprite.contents().height());
   }
  }
  if(glint!=null){
   ClassicFlatItemMesh.Vertex foil=(x,y,z,u,v,nx,ny,nz)->glint.vertex(matrix.pose(),x,y,z).uv(u,v).endVertex();
   if(mirrored)ClassicFlatItemMesh.emitMirrored(foil,0,0,1,1,256,256);else ClassicFlatItemMesh.emit(foil,0,0,1,1,256,256);
  }
 }
 static List<Pass> groundSprite(TextureAtlasSprite sprite){
  return List.of(new Pass(RenderType.entityCutout(sprite.atlasLocation()),List.of(new Plane(sprite,-1))));
 }
 static void renderBillboard(List<Pass> passes,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light){
  var colors=Minecraft.getInstance().getItemColors();var matrix=pose.last();
  for(var pass:passes){
   var out=buffers.getBuffer(pass.type());
   for(var plane:pass.planes()){
    var sprite=plane.sprite();int tint=plane.tint()<0?0xffffff:colors.getColor(stack,plane.tint());
    float r=(tint>>16&255)/255f,g=(tint>>8&255)/255f,b=(tint&255)/255f;
    ClassicFlatItemMesh.Vertex vertex=(x,y,z,u,v,nx,ny,nz)->out.vertex(matrix.pose(),x,y,z).color(r,g,b,1)
        .uv(u,v).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(light)
        .normal(matrix.normal(),nx,ny,nz).endVertex();
    vertex.emit(0,0,0,sprite.getU0(),sprite.getV1(),0,1,0);
    vertex.emit(1,0,0,sprite.getU1(),sprite.getV1(),0,1,0);
    vertex.emit(1,1,0,sprite.getU1(),sprite.getV0(),0,1,0);
    vertex.emit(0,1,0,sprite.getU0(),sprite.getV0(),0,1,0);
   }
  }
 }
 private ClassicFlatItemRenderer(){}
}
