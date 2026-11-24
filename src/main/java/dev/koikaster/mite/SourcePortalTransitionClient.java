package dev.koikaster.mite;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** bdi's 20-tick execute/30-tick finish counter and avj's flat color fade. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class SourcePortalTransitionClient {
 private static UUID token;
 private static boolean active;
 private static int counter;
 private static long draws;
 private static int lastArgb,lastDrawCounter,requests,requestCounter;
 /** Read-only observation of this new transition; no player setting or HUD. */
 record Probe(UUID token,boolean active,int counter,long draws,int lastArgb,int lastDrawCounter,int requests,int requestCounter){}
 static Probe probe(){return new Probe(token,active,counter,draws,lastArgb,lastDrawCounter,requests,requestCounter);}
 public static void start(UUID value){token=value;active=true;counter=0;draws=0;lastArgb=lastDrawCounter=requests=requestCounter=0;}
 public static void finished(UUID value){if(!value.equals(token))return;active=false;counter=30;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;var client=Minecraft.getInstance();
  if(client.player==null||client.level==null){token=null;active=false;counter=0;return;}
  if(client.isPaused())return;
  if(active){if(++counter==20){requestCounter=counter;requests++;SourcePortalTransition.request(token);}else if(counter>20)counter=30;}
  else if(counter>0)--counter;
 }
 @SubscribeEvent public static void draw(RenderGuiEvent.Post event){
  var client=Minecraft.getInstance();if(counter<=0||client.level==null)return;
  int rgb=client.level.dimension()==Level.OVERWORLD?0x359fff:client.level.dimension()==WorldEvents.UNDERWORLD?0x4401b4:client.level.dimension()==Level.NETHER?0xe47b4e:0xffffff;
  int alpha=(int)(255*Math.min(counter/20f,1));
  lastArgb=alpha<<24|rgb;lastDrawCounter=counter;draws++;
  event.getGuiGraphics().fill(0,0,client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),lastArgb);
 }
 private SourcePortalTransitionClient(){}
}
