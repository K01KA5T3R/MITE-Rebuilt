package dev.koikaster.mite;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.screens.inventory.BookViewScreen;import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;import net.minecraft.client.resources.sounds.SimpleSoundInstance;import net.minecraft.client.resources.sounds.SoundInstance;import net.minecraft.sounds.SoundEvent;import net.minecraft.sounds.SoundSource;
/** Original UI and remote howl audio; no extra game text or desktop input. */
public final class ClientContextSounds {
 private static void ui(SoundEvent sound,float volume){float gain=SourceSoundGain.ui(volume);if(gain>0)Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,1,gain));}
 public static void opened(Object screen){ui(SourceCreatureSounds.BOOK_OPEN.get(),.5f);}
 public static void closed(Object screen){ui(SourceCreatureSounds.BOOK_CLOSE.get(),2);}
 public static void page(Object screen,int before,int after,boolean enabled){if(enabled&&before!=after&&Minecraft.getInstance().screen==screen)ui(SourceCreatureSounds.BOOK_PAGE.get(),.5f);}
 public static boolean pageButton(){var screen=Minecraft.getInstance().screen;return screen instanceof BookViewScreen||screen instanceof BookEditScreen;}
 public static void howl(SourceWolfHowl.Message message){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!mc.level.dimension().location().equals(message.dimension()))return;double distance=(float)Math.sqrt(mc.player.distanceToSqr(message.x(),message.y(),message.z()));float volume=SourceContextSoundRules.howlVolume(distance,message.volume());if(volume<=0)return;mc.getSoundManager().play(new Howl(message,volume));}
 private static final class Howl extends AbstractSoundInstance {
  Howl(SourceWolfHowl.Message message,float adjusted){super(SourceCreatureSounds.WOLF_HOWL.get(),SoundSource.NEUTRAL,SoundInstance.createUnseededRandom());x=message.x();y=message.y();z=message.z();volume=adjusted;pitch=message.pitch();attenuation=Attenuation.NONE;relative=false;}
 }
 private ClientContextSounds(){}
}
