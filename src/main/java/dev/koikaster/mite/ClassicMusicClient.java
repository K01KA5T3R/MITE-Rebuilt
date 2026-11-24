package dev.koikaster.mite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID,value=Dist.CLIENT)
public final class ClassicMusicClient {
 private static final Music UNDERWORLD_GAME = new Music(
     Musics.GAME.getEvent(), Musics.GAME.getMinDelay(), Musics.GAME.getMaxDelay(), true);
 /** Normalize actual MusicManager selection, including the custom Underworld. */
 public static net.minecraft.sounds.Music situationalMusic(Object raw){
  var mc=(Minecraft)raw;
  boolean modern=ModernMusicClient.refresh(mc);
  var selected=mc.getSituationalMusic();
  if(modern)selected=ModernMusicClient.sharedSurvivalMusic(mc,selected);
  return choose(selected,mc.player!=null&&mc.player.level().dimension()==WorldEvents.UNDERWORLD);
 }
 public static net.minecraft.sounds.Music choose(net.minecraft.sounds.Music selected,boolean underworld){
  if(selected==net.minecraft.sounds.Musics.CREDITS||selected==net.minecraft.sounds.Musics.MENU)return selected;
  // R195 bln.c() draws from the ordinary music pool without a dimension branch.
  if(underworld)return UNDERWORLD_GAME;
  if(selected==net.minecraft.sounds.Musics.END_BOSS)return net.minecraft.sounds.Musics.END;
  return selected==net.minecraft.sounds.Musics.CREATIVE?net.minecraft.sounds.Musics.GAME:selected;
 }
 public static boolean classicCreditsPlaying(){
  return Minecraft.getInstance().screen instanceof ClassicEndCreditsScreen credits&&credits.usesClassicCredits();
 }
 public static boolean blocksOrdinaryMusic(){
  return classicCreditsPlaying()||ClientBossMusic.takesMusicChannel();
 }
 @SubscribeEvent
 public static void play(PlaySoundEvent event) {
  if (classicCreditsPlaying()) event.setSound(null);
 }

 /** The native SoundEngine has already chosen the sample for this playback request. */
 public static boolean allowsResolved(Object rawInstance, Object rawSample) {
  var instance = (SoundInstance) rawInstance;
  var source = instance.getSource();
  if (source != SoundSource.MUSIC && source != SoundSource.RECORDS) return true;

  var sample = (Sound) rawSample;
  if (ClientBossMusic.isBossLoop(instance)) return ClientBossMusic.permits(instance, sample);
  if (source == SoundSource.MUSIC && ClientBossMusic.takesMusicChannel()) return false;

  var location = sample.getLocation();
  // Note blocks share the RECORDS volume category with music discs.
  if (source == SoundSource.RECORDS && !instance.getLocation().getPath().startsWith("music_disc.")
      && !location.getPath().startsWith("records/")) return true;
  return location.getNamespace().equals("minecraft")
      && (ClassicMusic.TRACKS.contains(location.getPath()) || ModernMusicClient.allowsResolved(instance,sample));
 }
 private ClassicMusicClient(){}
}
