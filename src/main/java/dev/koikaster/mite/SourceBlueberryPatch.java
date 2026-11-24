package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** acz/acu forest2 tenths of a patch per chunk + WorldGenPlants64 attempts.
 * Origin Y remains the source0..127, with source triangular8/4/8 offsets. */
public final class SourceBlueberryPatch extends Feature<NoneFeatureConfiguration> {
 public SourceBlueberryPatch(){super(NoneFeatureConfiguration.CODEC);}
 public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){var level=context.level();var random=context.random();if(!level.getLevel().dimension().equals(Level.OVERWORLD)||random.nextInt(10)>=2)return false;var origin=context.origin();int x=origin.getX()+random.nextInt(16)+8,y=random.nextInt(128),z=origin.getZ()+random.nextInt(16)+8;int placed=0;
  for(int attempt=0;attempt<64;attempt++){var pos=new BlockPos(x+random.nextInt(8)-random.nextInt(8),y+random.nextInt(4)-random.nextInt(4),z+random.nextInt(8)-random.nextInt(8));if(level.isOutsideBuildHeight(pos)||!level.isEmptyBlock(pos)||!SourceBlueberryBush.canPlace(level,pos))continue;level.setBlock(pos,SourceBlueberryBush.BUSH.get().defaultBlockState().setValue(SourceBlueberryBush.GROWTH,3),2);if(++placed>3&&random.nextInt(3)==0)break;}
  return placed>0;
 }
}
