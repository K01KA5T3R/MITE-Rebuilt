package dev.koikaster.mite;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** acu.K + afw, Underworld branch. js.getMapGenCaveNetwork returns null for
 * the separate Underworld provider, so no Overworld cave-network stub is invented. */
public final class SourceUnderworldSprings extends Feature<NoneFeatureConfiguration> {
 public SourceUnderworldSprings(){super(NoneFeatureConfiguration.CODEC);}
 public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
  var world=context.level();if(world.getLevel().dimension()!=WorldEvents.UNDERWORLD)return false;
  var origin=context.origin();if(SourceMyceliumFlags.nearby(world,origin.getX()>>4,origin.getZ()>>4))return false;
  // Native features have separate RNG ownership after the human-selected ore
  // layout. Keep the source decorator seed and spring-branch draw ordering.
  var random=new Random((long)(origin.getX()+origin.getZ()*65536)+world.getSeed()*0x100000000L);
  boolean eligible=false;
  for(int i=0;i<70;i++){
   int x=origin.getX()+random.nextInt(16)+8,y=random.nextInt(random.nextInt(120)+8),z=origin.getZ()+random.nextInt(16)+8;
   boolean lava;
   if(random.nextInt(32)+16<y)lava=false;else lava=random.nextFloat()<.95f;
   eligible|=generate(world,new BlockPos(x,y+120,z),lava);
  }
  return eligible;
 }
 public static boolean generate(WorldGenLevel world,BlockPos pos,boolean lava){
  if(!world.getBlockState(pos.above()).is(Blocks.STONE)||!world.getBlockState(pos.below()).is(Blocks.STONE))return false;
  var center=world.getBlockState(pos);if(!center.isAir()&&!center.is(Blocks.STONE))return false;
  int stones=0,air=0;
  for(var at:new BlockPos[]{pos.west(),pos.east(),pos.north(),pos.south()}){var state=world.getBlockState(at);if(state.is(Blocks.STONE))stones++;if(state.isAir())air++;}
  if(stones==3&&air==1){
   world.setBlock(pos,(lava?Blocks.LAVA:Blocks.WATER).defaultBlockState(),2);
   // afw calls the old moving-liquid update immediately. Native fluid ticks
   // belong to the chunk tick container, so enqueue its first update at1tick.
   world.scheduleTick(pos,lava?Fluids.LAVA:Fluids.WATER,1);
  }
  return true;
 }
}
