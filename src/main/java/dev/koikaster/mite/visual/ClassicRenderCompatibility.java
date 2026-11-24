package dev.koikaster.mite.visual;

import dev.koikaster.mite.MiteReforged;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Native renderer factories are retained before base-mod visual overrides. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClassicRenderCompatibility {
 private static final Map<EntityType<?>,EntityRendererProvider<?>> NATIVE=new HashMap<>();
 @SuppressWarnings("unchecked") private static Map<EntityType<?>,EntityRendererProvider<?>> providers() throws ReflectiveOperationException {
  for(var field:EntityRenderers.class.getDeclaredFields())if(Modifier.isStatic(field.getModifiers())&&Map.class.isAssignableFrom(field.getType())){
   field.setAccessible(true);var candidate=(Map<?,?>)field.get(null);
   if(candidate.containsKey(EntityType.PIG))return (Map<EntityType<?>,EntityRendererProvider<?>>)candidate;
  }
  throw new IllegalStateException("Unsupported entity renderer registry");
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void nativeFactories(EntityRenderersEvent.RegisterRenderers event){
  if(!NATIVE.isEmpty())return;
  try{NATIVE.putAll(providers());}catch(ReflectiveOperationException|RuntimeException failure){com.mojang.logging.LogUtils.getLogger().warn("Native visual fallback unavailable; keeping registered renderers",failure);}
 }
 @SuppressWarnings({"rawtypes","unchecked"}) @SubscribeEvent(priority=EventPriority.LOWEST) public static void selectFactories(EntityRenderersEvent.RegisterRenderers event){
  if(NATIVE.isEmpty())return;
  try{
   var current=providers();
   for(var entry:NATIVE.entrySet()){
    var type=entry.getKey();EntityRendererProvider classic=current.get(type),nativeRenderer=entry.getValue();
    if(classic==null||classic==nativeRenderer)continue;
    event.registerEntityRenderer((EntityType)type,(EntityRendererProvider)(context->(ClassicVisuals.entities()?classic:nativeRenderer).create(context)));
   }
  }catch(ReflectiveOperationException|RuntimeException failure){com.mojang.logging.LogUtils.getLogger().warn("Classic visual renderer selection unavailable; keeping registered renderers",failure);}
 }
 private ClassicRenderCompatibility(){}
}
