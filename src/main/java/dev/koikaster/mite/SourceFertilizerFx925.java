package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
/** Native level-event transport, with no new channel message or particle registry entry. */
public final class SourceFertilizerFx925 {
 public static final int MANURE_EVENT=195046;
 public static void manure(Object raw,Object at){var level=(Level)raw;if(!level.isClientSide)level.levelEvent(MANURE_EVENT,(BlockPos)at,0);}
 public static void growth(Object raw,Object at,int count){if(raw instanceof Level level&&level.isClientSide)ClientFertilizerParticles925.growth(level,(BlockPos)at,count);}
 public static void aura(Object raw,double x,double y,double z,double vx,double vy,double vz){if(raw instanceof Level level&&level.isClientSide)ClientFertilizerParticles925.aura(level,x,y,z,vx,vy,vz);}
 private SourceFertilizerFx925(){}
}
