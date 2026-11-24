package dev.koikaster.mite;

import java.util.Random;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** BiomeGenUnderworld.a before the source biome decorator and the selected ore layout. */
public final class SourceUnderworldMycelium extends Feature<NoneFeatureConfiguration> {
 public SourceUnderworldMycelium(){super(NoneFeatureConfiguration.CODEC);}
 public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
  var level=context.level();if(level.getLevel().dimension()!=WorldEvents.UNDERWORLD)return false;
  var random=new Random(level.getSeed());random.nextInt();long hashed=random.nextLong();
  OriginalMyceliumPopulation.populate(new SourceMyceliumWorld(level),hashed,context.origin().getX(),context.origin().getZ());return true;
 }
}
