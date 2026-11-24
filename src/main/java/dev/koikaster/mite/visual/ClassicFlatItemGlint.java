package dev.koikaster.mite.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.koikaster.mite.MiteReforged;
import java.util.*;
import net.minecraft.Util;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.*;
import org.joml.Matrix4f;

/** R195 bfj flat-icon foil: independent 0..1 UVs, 256 extrusion steps and
 * texture scale 1/8. Other native glint providers remain authoritative. */
@OnlyIn(Dist.CLIENT)
public abstract class ClassicFlatItemGlint extends RenderType {
 private static final ResourceLocation TEXTURE=new ResourceLocation(MiteReforged.ID,"textures/misc/classic_glint.png");
 private static final RenderType[] LAYERS={layer(0),layer(1)};
 private static final Set<MultiBufferSource.BufferSource> REJECTED=Collections.newSetFromMap(new WeakHashMap<>());
 private record State(Matrix4f texture,float[] color){}
 private static final ThreadLocal<Deque<State>> STATES=ThreadLocal.withInitial(ArrayDeque::new);
 private static java.lang.reflect.Field fixedField;
 private ClassicFlatItemGlint(String n,VertexFormat f,VertexFormat.Mode m,int size,boolean crumble,boolean sorted,Runnable setup,Runnable clear){super(n,f,m,size,crumble,sorted,setup,clear);}
 public static VertexConsumer acquire(MultiBufferSource buffers){
  if(!ClassicVisualState.ready||!ClassicVisuals.particles()||!(buffers instanceof MultiBufferSource.BufferSource source)||REJECTED.contains(source))return null;
  try{
   if(fixedField==null){for(var field:MultiBufferSource.BufferSource.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){fixedField=field;field.setAccessible(true);break;}if(fixedField==null)throw new IllegalStateException("Unsupported native buffer map");}
   @SuppressWarnings("unchecked") var fixed=(Map<RenderType,BufferBuilder>)fixedField.get(source);
   for(var type:LAYERS)if(!fixed.containsKey(type))fixed.put(type,new BufferBuilder(type.bufferSize()));
   return VertexMultiConsumer.create(buffers.getBuffer(LAYERS[0]),buffers.getBuffer(LAYERS[1]));
  }catch(ReflectiveOperationException|RuntimeException error){REJECTED.add(source);com.mojang.logging.LogUtils.getLogger().warn("Classic flat glint unavailable; retaining native rendering",error);return null;}
 }
 public static Matrix4f textureMatrix(int pass,long millis){
  long period=pass==0?3000:4873;float progress=(float)Math.floorMod(millis,period)/period;
  return new Matrix4f().translation(pass==0?progress:-progress,0,0).rotateZ((float)Math.toRadians(pass==0?-50:10)).scale(.125f);
 }
 private static RenderType layer(int pass){
  var state=CompositeState.builder().setShaderState(RENDERTYPE_GLINT_DIRECT_SHADER).setTextureState(new TextureStateShard(TEXTURE,true,false)).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(new TexturingStateShard("mite_flat_glint_"+pass,()->setup(pass),ClassicFlatItemGlint::restore)).createCompositeState(false);
  return RenderType.create("mite_flat_glint_"+pass,DefaultVertexFormat.POSITION_TEX,VertexFormat.Mode.QUADS,65536,false,false,state);
 }
 private static void setup(int pass){
  STATES.get().push(new State(new Matrix4f(RenderSystem.getTextureMatrix()),RenderSystem.getShaderColor().clone()));
  RenderSystem.setTextureMatrix(textureMatrix(pass,Util.getMillis()));RenderSystem.setShaderColor(.38f,.19f,.608f,1);
 }
 private static void restore(){var saved=STATES.get();if(saved.isEmpty())return;var state=saved.pop();RenderSystem.setTextureMatrix(state.texture());var color=state.color();RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);}
}
