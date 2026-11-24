package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
/** Source precipitation/overcast and server brightness inputs on modern biome, sky-light and height APIs. */
public final class OriginalWeather {
 public static boolean blood(Level level){return Settings.MOONS&&level.dimension()==Level.OVERWORLD&&SourceMoonCycle.blood(level.getDayTime(),false);}
 public static boolean rain(Level level,BlockPos pos){if(!level.isRaining())return false;var biome=level.getBiome(pos).value();if(!biome.hasPrecipitation()&&!blood(level)||biome.getPrecipitationAt(pos)==Biome.Precipitation.SNOW)return false;return level.canSeeSky(pos)&&level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())<=pos.getY();}
 public static boolean overcast(Level level,BlockPos pos){return level.isRaining()&&(level.getBiome(pos).value().hasPrecipitation()||blood(level));}
 public static float brightness(Mob mob){var level=mob.level();var pos=BlockPos.containing(mob.getX(),mob.getY()+mob.getBbHeight()*.66,mob.getZ());if(!level.hasChunkAt(pos))return 0;int dimension=level.dimension()==Level.NETHER?-1:level.dimension()==Level.END?1:level.dimension()==WorldEvents.UNDERWORLD?-2:0;float angle=LegacyLighting.celestialAngle(level.getDayTime(),1,dimension);float dark=LegacyLighting.clamp(1-(LegacyLighting.cos(angle*(float)Math.PI*2)*2+.5f));boolean ignore=!level.getBiome(pos).value().hasPrecipitation()&&!blood(level);if(!ignore){float light=1-dark;light=(float)(light*(1-(double)(level.getRainLevel(1)*5)/16));light=(float)(light*(1-(double)(level.getThunderLevel(1)*5)/16));dark=1-light;}int raw=Math.max(level.getBrightness(LightLayer.BLOCK,pos),level.getBrightness(LightLayer.SKY,pos)-(int)(dark*11));return LegacyLighting.brightness(raw,dimension==-1?.1f:0);}
 private OriginalWeather(){}
}
