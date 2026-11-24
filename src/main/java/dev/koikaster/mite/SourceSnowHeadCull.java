package dev.koikaster.mite;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.opengl.GL11;

/** bhb keeps sb's head equipment double-sided. Change culling at the actual
 * deferred draw, retaining the native type's shader and every other state. */
public final class SourceSnowHeadCull {
 private static final Map<RenderType,WeakReference<RenderType>> CACHE=new WeakHashMap<>();
 private static final ThreadLocal<ArrayDeque<Boolean>> CULL=ThreadLocal.withInitial(ArrayDeque::new);
 static MultiBufferSource buffers(MultiBufferSource source){return type->source.getBuffer(layer(type));}
 static RenderType layer(RenderType type){
  if(type instanceof Layer)return type;
  synchronized(CACHE){var reference=CACHE.get(type);var existing=reference==null?null:reference.get();if(existing!=null)return existing;var wrapped=new Layer(type);CACHE.put(type,new WeakReference<>(wrapped));return wrapped;}
 }
 private static void setup(RenderType original){original.setupRenderState();CULL.get().push(GL11.glIsEnabled(GL11.GL_CULL_FACE));RenderSystem.disableCull();}
 private static void clear(RenderType original){boolean cull=CULL.get().pop();if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();original.clearRenderState();}
 private static final class Layer extends RenderType {
  private final RenderType original;
  Layer(RenderType original){super("mite_snow_head",original.format(),original.mode(),original.bufferSize(),original.affectsCrumbling(),ObfuscationReflectionHelper.<Boolean,RenderType>getPrivateValue(RenderType.class,original,"f_110393_"),()->setup(original),()->clear(original));this.original=original;}
  @Override public Optional<RenderType> outline(){return original.outline().map(SourceSnowHeadCull::layer);}
  @Override public boolean isOutline(){return original.isOutline();}
 }
 private SourceSnowHeadCull(){}
}
