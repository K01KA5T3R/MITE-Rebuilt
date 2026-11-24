package dev.koikaster.mite;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** bln's reciprocal long-distance attenuation; ordinary linear audio would
 * incorrectly silence the old Witch cackle beyond the nearby sound radius. */
final class SourceWitchClientAudio {
 static void accept(SourceWitchCackle packet){
  var mc=Minecraft.getInstance();if(mc.player==null)return;float x=(float)packet.x()/8f,y=(float)packet.y()/8f,z=(float)packet.z()/8f;
  double distance=Math.sqrt(mc.player.distanceToSqr(x,y,z));if(distance<16||distance>64||packet.volume()<=0)return;
  float volume=packet.volume()*(1f/((float)distance-16f+1f));
  mc.getSoundManager().play(new SimpleSoundInstance(SourceWitchContent.CACKLE.get().getLocation(),SoundSource.HOSTILE,volume,(float)packet.pitch()/63f,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,x,y,z,false));
 }
 private SourceWitchClientAudio(){}
}
