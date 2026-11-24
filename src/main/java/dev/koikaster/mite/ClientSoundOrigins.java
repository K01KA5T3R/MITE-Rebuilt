package dev.koikaster.mite;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Logs the sound actually admitted by the engine, after its sample was chosen.
 * Do not resolve it a second time, consume RNG or change playback. The sound
 * thread only captures values; world access stays on the Minecraft thread. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClientSoundOrigins {
 @SubscribeEvent public static void buffered(PlaySoundSourceEvent event){record(event);}
 @SubscribeEvent public static void streaming(PlayStreamingSourceEvent event){record(event);}
 private static void record(SoundEvent.SoundSourceEvent event){
  var sound=event.getSound();if(sound==null)return;
  ResourceLocation id=sound.getLocation();String path=id.getPath();
  boolean ghast=id.getNamespace().equals("minecraft")&&path.startsWith("entity.ghast.");
  if(!ghast&&!id.equals(new ResourceLocation("minecraft:ambient.cave"))
   &&!(id.getNamespace().equals(MiteReforged.ID)&&(path.startsWith("shadow.")||path.startsWith("invisible_stalker."))))return;
  var resolved=sound.getSound();String sample=resolved==null?"unresolved":resolved.getLocation().toString();
  String category=sound.getSource().getName();double x=sound.getX(),y=sound.getY(),z=sound.getZ();
  float volume=sound.getVolume(),pitch=sound.getPitch();
  var mc=Minecraft.getInstance();mc.execute(()->{
   var level=mc.level;if(level==null)return;
   double distance=mc.player==null?-1:Math.sqrt(mc.player.distanceToSqr(x,y,z));
   var log=LogUtils.getLogger();
   String format="[MITE sound source] event={} sample={} category={} dimension={} distance={} volume={} pitch={}";
   Object[] values={id,sample,category,level.dimension().location(),Math.round(distance*10)/10.0,volume,pitch};
   if(ghast&&level.dimension()!=Level.NETHER)log.warn(format,values);else log.info(format,values);
  });
 }
 private ClientSoundOrigins(){}
}
