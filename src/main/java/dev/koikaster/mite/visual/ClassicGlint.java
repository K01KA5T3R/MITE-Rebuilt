package dev.koikaster.mite.visual;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.koikaster.mite.MiteReforged;
import java.util.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.*;
import org.joml.Matrix4f;

/** Native buffered render types with the original bfj dual purple layers and 3000/4873ms periods.
 * Keeps native shader/output targets and atlas scale; compass/clock decal providers retain native rendering.
 */
@OnlyIn(Dist.CLIENT)
public abstract class ClassicGlint extends RenderType {
 private static final ResourceLocation TEXTURE=new ResourceLocation(MiteReforged.ID,"textures/misc/classic_glint.png");
 private static final Map<Integer,RenderType[]> TYPES=new HashMap<>();
 private static final Set<MultiBufferSource.BufferSource> REJECTED=Collections.newSetFromMap(new WeakHashMap<>());
 private record State(Matrix4f texture,float[] color){}
 private static final ThreadLocal<Deque<State>> STATES=ThreadLocal.withInitial(ArrayDeque::new);
 private static java.lang.reflect.Field fixedField;
 private ClassicGlint(String n,VertexFormat f,VertexFormat.Mode m,int size,boolean crumble,boolean sorted,Runnable setup,Runnable clear){super(n,f,m,size,crumble,sorted,setup,clear);}
 public static void resetVisualFailure(){REJECTED.clear();}
 public static VertexConsumer consumer(VertexConsumer nativeResult,MultiBufferSource buffers,RenderType base,boolean item,boolean foil,int factory){
  if(!ClassicVisualState.ready||!foil||!ClassicVisuals.particles()||!(buffers instanceof MultiBufferSource.BufferSource source)||REJECTED.contains(source))return nativeResult;
  try{
   int group=factory==2?(item?0:1):factory==1?(item?4:6):Minecraft.useShaderTransparency()&&base==Sheets.translucentItemSheet()?2:item?3:5;
   var layers=TYPES.computeIfAbsent(group,ClassicGlint::make);
   if(fixedField==null){for(var field:MultiBufferSource.BufferSource.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){fixedField=field;field.setAccessible(true);break;}if(fixedField==null)throw new IllegalStateException("Unsupported native buffer map");}
   @SuppressWarnings("unchecked") var fixed=(Map<RenderType,BufferBuilder>)fixedField.get(source);
   for(var type:layers)if(!fixed.containsKey(type))fixed.put(type,new BufferBuilder(type.bufferSize()));
   return VertexMultiConsumer.create(buffers.getBuffer(layers[0]),buffers.getBuffer(layers[1]),buffers.getBuffer(base));
  }catch(ReflectiveOperationException|RuntimeException error){REJECTED.add(source);com.mojang.logging.LogUtils.getLogger().warn("Classic glint unavailable for this buffer provider; retaining native rendering",error);return nativeResult;}
 }
 /** 1.20.1's Model armor overload draws its base separately and bypasses the
  * ItemRenderer foil factories. Return only the two glint layers here. */
 public static VertexConsumer armor(VertexConsumer nativeResult,MultiBufferSource buffers){
  if(!ClassicVisualState.ready||!ClassicVisuals.particles()||!(buffers instanceof MultiBufferSource.BufferSource source)||REJECTED.contains(source))return nativeResult;
  try{
   var layers=TYPES.computeIfAbsent(1,ClassicGlint::make);
   if(fixedField==null){for(var field:MultiBufferSource.BufferSource.class.getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){fixedField=field;field.setAccessible(true);break;}if(fixedField==null)throw new IllegalStateException("Unsupported native buffer map");}
   @SuppressWarnings("unchecked") var fixed=(Map<RenderType,BufferBuilder>)fixedField.get(source);
   for(var type:layers)if(!fixed.containsKey(type))fixed.put(type,new BufferBuilder(type.bufferSize()));
   return VertexMultiConsumer.create(buffers.getBuffer(layers[0]),buffers.getBuffer(layers[1]));
  }catch(ReflectiveOperationException|RuntimeException error){REJECTED.add(source);com.mojang.logging.LogUtils.getLogger().warn("Classic armor glint unavailable for this buffer provider; retaining native rendering",error);return nativeResult;}
 }
 private static RenderType[] make(int group){return new RenderType[]{layer(group,0),layer(group,1)};}
 private static RenderType layer(int group,int pass){
  var shader=switch(group){case 0->RENDERTYPE_ARMOR_GLINT_SHADER;case 1->RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER;case 2->RENDERTYPE_GLINT_TRANSLUCENT_SHADER;case 3->RENDERTYPE_GLINT_SHADER;case 4->RENDERTYPE_GLINT_DIRECT_SHADER;case 5->RENDERTYPE_ENTITY_GLINT_SHADER;default->RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER;};
  float scale=group==1||group==5||group==6?.16f:8;
  var builder=CompositeState.builder().setShaderState(shader).setTextureState(new TextureStateShard(TEXTURE,true,false)).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(new TexturingStateShard("mite_classic_glint_"+pass,()->setup(pass,scale),ClassicGlint::restore));
  if(group==0||group==1)builder.setLayeringState(VIEW_OFFSET_Z_LAYERING);
  if(group==2||group==5)builder.setOutputState(ITEM_ENTITY_TARGET);
  return RenderType.create("mite_classic_glint_"+group+"_"+pass,DefaultVertexFormat.POSITION_TEX,VertexFormat.Mode.QUADS,256,false,false,builder.createCompositeState(false));
 }
 private static void setup(int pass,float scale){
  STATES.get().push(new State(new Matrix4f(RenderSystem.getTextureMatrix()),RenderSystem.getShaderColor().clone()));
  long period=pass==0?3000:4873;float progress=(float)Math.floorMod(Util.getMillis(),period)/period;
  var matrix=new Matrix4f().translation(pass==0?progress:-progress,0,0).rotateZ((float)Math.toRadians(pass==0?-50:10)).scale(scale);
  RenderSystem.setTextureMatrix(matrix);RenderSystem.setShaderColor(.38f,.19f,.608f,1);
 }
 private static void restore(){var stack=STATES.get();if(stack.isEmpty())return;var state=stack.pop();RenderSystem.setTextureMatrix(state.texture());var color=state.color();RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);}
}
