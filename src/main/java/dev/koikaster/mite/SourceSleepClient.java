package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static dev.koikaster.mite.SourceSleepControlRules.*;

/** bdi's100/110 fade and the two ka signals, driven by client ticks. The native
 * sleep GUI consumes the counter; no additional GUI text or input is added. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class SourceSleepClient {
 private static LocalPlayer owner;private static State state=new State();private static long epoch;
 private static void reset(LocalPlayer player){owner=player;state=new State();epoch=0;}
 public static void receive(SourceSleepNetwork.Phase packet){
  var player=Minecraft.getInstance().player;
  if(player==null||player.getId()!=packet.entity()||!player.level().dimension().location().equals(packet.dimension()))return;
  if(owner!=player)reset(player);if(packet.epoch()<=0||packet.epoch()<epoch)return;epoch=packet.epoch();
  switch(packet.phase()){
   case FULLY_AWAKE->{state.phase=Phase.FULLY_AWAKE;state.counter=0;}
   case FALLING_ASLEEP->SourceSleepControlRules.falling(state);
   case WAKING_UP->SourceSleepControlRules.waking(state);
   case SLEEPING->{state.phase=Phase.SLEEPING;state.counter=110;}
  }
 }
 public static int timer(int original,Object raw){
  if(!(raw instanceof LocalPlayer player)||player.level().dimension()!=Level.OVERWORLD)return original;
  if(owner!=player)reset(player);return player.isSleeping()?state.counter:0;
 }
 @SubscribeEvent public static void fade(RenderGuiOverlayEvent.Pre event){
  var mc=Minecraft.getInstance();var player=mc.player;
  if(player==null||player.level().dimension()!=Level.OVERWORLD||!event.getOverlay().id().equals(VanillaGuiOverlay.SLEEP_FADE.id()))return;
  event.setCanceled(true);
 }
 @SubscribeEvent public static void background(RenderGuiEvent.Pre event){
  var mc=Minecraft.getInstance();var player=mc.player;
  if(player==null||player.level().dimension()!=Level.OVERWORLD)return;
  if(owner!=player)reset(player);
  // avj draws its black sleep rectangle before the normal HUD. Forge's late
  // native sleep overlay is separately cancelled, preserving that ordering.
  if(player.isSleeping()&&state.counter>0)event.getGuiGraphics().fill(net.minecraft.client.renderer.RenderType.guiOverlay(),0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),SourceSleepVisualRules.color(state.counter));
 }
 private static float rainFactor(){
  var player=Minecraft.getInstance().player;
  return player!=null&&player==owner&&player.isSleeping()&&player.level().dimension()==Level.OVERWORLD?SourceSleepVisualRules.rainFactor(state.counter):1;
 }
 public static void rainSound(Object raw,net.minecraft.core.BlockPos pos,net.minecraft.sounds.SoundEvent sound,net.minecraft.sounds.SoundSource category,float volume,float pitch,boolean delayed){
  SourceRainSound.play(raw,pos,sound,category,volume,pitch,delayed,rainFactor());
 }
 static void rainSound(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,boolean above){
  SourceRainSound.play(level,x,y,z,net.minecraft.sounds.SoundEvents.WEATHER_RAIN,net.minecraft.sounds.SoundSource.WEATHER,
   above?.1F:.2F,above?.5F:1F,false,rainFactor());
 }
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
  if(event.phase!=TickEvent.Phase.END)return;var player=Minecraft.getInstance().player;
  if(player==null){reset(null);return;}if(owner!=player)reset(player);
  if(player.level().dimension()!=Level.OVERWORLD){state.phase=Phase.FULLY_AWAKE;state.counter=0;return;}
  // A custom phase packet can precede the native tracked bed pose. Preserve
  // the pending phase until that pose arrives; a reset here would lose its
  // only falling signal and leave the server waiting forever. FULLY_AWAKE,
  // respawn, dimension and disconnect messages still reset the actual state.
  if(!player.isSleeping())return;
  boolean cannotSleep=state.phase==Phase.FALLING_ASLEEP&&OriginalCurseEffects.has(player,12,true);
  var signal=SourceSleepControlRules.clientTick(state,cannotSleep);if(signal!=Signal.NONE)SourceSleepNetwork.signal(epoch,signal);
 }
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){reset(null);}
 private SourceSleepClient(){}
}
