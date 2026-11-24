package dev.koikaster.mite;
import net.minecraft.client.Minecraft;import net.minecraft.client.multiplayer.ClientLevel;import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;import net.minecraft.sounds.SoundEvents;import net.minecraft.sounds.SoundSource;import net.minecraft.world.level.Level;import net.minecraft.world.level.levelgen.Heightmap;
/** Shared rain strength and sleep mix for both the original sampler and the native fallback. */
public final class SourceRainSound {
 public static float strength(ClientLevel level,double x,double z){
  boolean blood=level.dimension()==Level.OVERWORLD&&SourceMoonCycle.blood(level.getDayTime(),false);
  return SourceRainSoundRules.strength(level.getRainLevel(1),x,z,blood,new SourceRainSoundRules.Columns(){
   public boolean loaded(int x,int z){return level.hasChunk(x>>4,z>>4);}
   public boolean wet(int x,int z){return loaded(x,z)&&level.getBiome(new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z),z)).value().hasPrecipitation();}
  });
 }
 public static void play(Object raw,BlockPos position,SoundEvent sound,SoundSource category,float volume,float pitch,boolean delayed,float sleep){
  play((ClientLevel)raw,position.getX()+.5,position.getY()+.5,position.getZ()+.5,sound,category,volume,pitch,delayed,sleep);
 }
 static void play(ClientLevel level,double x,double y,double z,SoundEvent sound,SoundSource category,float volume,float pitch,boolean delayed,float sleep){
  var mc=Minecraft.getInstance();var player=mc.player;var viewer=mc.getCameraEntity();
  if(player!=null&&viewer!=null&&(sound==SoundEvents.WEATHER_RAIN||sound==SoundEvents.WEATHER_RAIN_ABOVE)){
   boolean outdoors=SourceSpawnOutdoors.isOutdoors(level,player.blockPosition());var adjusted=SourceRainSoundRules.sound(volume,pitch,outdoors,strength(level,viewer.getX(),viewer.getZ()),sleep);volume=adjusted.volume();pitch=adjusted.pitch();
  }else volume*=sleep;
  level.playLocalSound(x,y,z,sound,category,volume,pitch,delayed);
 }
 private SourceRainSound(){}
}
