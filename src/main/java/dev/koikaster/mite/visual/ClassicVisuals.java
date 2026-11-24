package dev.koikaster.mite.visual;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Global 1.6.4 classic visual presentation engine (shared uniformly across 1.6.4 MITE and 1.20.1 MITE modes). */
@Mod.EventBusSubscriber(modid=dev.koikaster.mite.MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClassicVisuals {
 private static boolean value(net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag){try{return flag.get();}catch(IllegalStateException ignored){return flag.getDefault();}}
 private static boolean enabled(net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag){return value(ClassicVisualConfig.ENABLED)&&value(flag);}
 public static boolean modernTextures(){
  var minecraft=net.minecraft.client.Minecraft.getInstance();
  return dev.koikaster.mite.ClientWorldContentMode.modernVisuals()&&minecraft!=null&&minecraft.level!=null
   &&dev.koikaster.mite.WorldContentMode.modernTextures(minecraft.level);
 }
 public static boolean textures(){return !modernTextures()&&value(ClassicVisualConfig.ENABLED)&&value(ClassicVisualConfig.TEXTURES);}
 public static boolean lighting(){return enabled(ClassicVisualConfig.LIGHTING);}
 public static boolean sky(){return enabled(ClassicVisualConfig.SKY);}
 public static boolean fog(){return enabled(ClassicVisualConfig.FOG);}
 public static boolean water(){return enabled(ClassicVisualConfig.WATER);}
 public static boolean entities(){return enabled(ClassicVisualConfig.ENTITIES);}
 public static boolean firstPerson(){return enabled(ClassicVisualConfig.FIRST_PERSON);}
 public static boolean particles(){return enabled(ClassicVisualConfig.PARTICLES);}
 public static boolean gui(){return enabled(ClassicVisualConfig.GUI);}
 public static boolean font(){return enabled(ClassicVisualConfig.FONT);}
 public static String signature(){return ""+modernTextures()+textures()+lighting()+sky()+fog()+water()+entities()+firstPerson()+particles()+gui()+font();}
 public static java.util.function.Predicate<ResourceLocation> resourceFilter(){
  final boolean textures=textures(),sky=sky(),water=water(),fog=fog(),entities=entities(),particles=particles(),gui=gui(),font=font(),lighting=lighting();
  return id->{
   String path=id.getPath();
   if(!id.getNamespace().equals("minecraft")){if(path.startsWith("textures/font/source164_"))return font;return true;}
   if(path.equals("shaders/include/light.glsl"))return lighting;
   if(path.equals("shaders/include/fog.glsl"))return water||fog;
   if(path.startsWith("font/")||path.startsWith("textures/font/"))return font;
   if(path.startsWith("textures/gui/")||path.startsWith("textures/misc/pumpkinblur"))return gui;
   if(path.startsWith("particles/")||path.startsWith("textures/particle/")||path.endsWith("environment/rain.png")||path.endsWith("environment/snow.png")||(path.contains("enchanted_item_glint")||path.contains("enchanted_glint")))return particles;
   if(path.startsWith("textures/environment/"))return sky;
   if(path.startsWith("textures/entity/")||path.endsWith("misc/shadow.png"))return entities;
   if(path.contains("water_")||path.endsWith("/water.png")||path.endsWith("/underwater.png"))return water;
   if(path.contains("vignette"))return lighting;
   if(path.startsWith("textures/")||path.startsWith("models/")||path.startsWith("blockstates/"))return textures;
   return true;
  };
 }
 @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->ClassicVisualState.ready=true);}
 @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){
  event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{dev.koikaster.mite.ClassicLightmap.resetVisualFailure();ClassicFirstPersonAnimations.resetVisualFailure();ClassicGlint.resetVisualFailure();});
 }
 private ClassicVisuals(){}
}
