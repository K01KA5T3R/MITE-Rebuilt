package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.*;

/** acu's original flower passes and aff's actual biome-only patch-size dispatch. */
public final class SourceFlowerGeneration extends Feature<NoneFeatureConfiguration> {
 public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,MiteReforged.ID);
 public static final RegistryObject<SourceFlowerGeneration> FEATURE=FEATURES.register("source_flowers",SourceFlowerGeneration::new);
 public SourceFlowerGeneration(){super(NoneFeatureConfiguration.CODEC);}
 static int passes(SourceFlora.BiomeInfo biome){return biome.plains()?4:biome.swamp()?1:biome.jungle()?6:2;}
 static int patchSize(SourceFlora.BiomeInfo biome){return biome.swamp()?16:32;}
 static void patch(WorldGenLevel world,RandomSource random,BlockPos origin,int subtype,SourceFlora.BiomeInfo biome){
  var block=subtype<0?net.minecraft.world.level.block.Blocks.DANDELION:SourceFlora.block(subtype);
  for(int attempt=0;attempt<patchSize(biome);attempt++){
   int y=origin.getY()+random.nextInt(4)-random.nextInt(4),x=origin.getX()+random.nextInt(8)-random.nextInt(8),z=origin.getZ()+random.nextInt(8)-random.nextInt(8);
   var at=new BlockPos(x,y,z);if(y<0||y>255||!world.isEmptyBlock(at)||world.dimensionType().hasCeiling()&&y>=127||!block.defaultBlockState().canSurvive(world,at))continue;world.setBlock(at,block.defaultBlockState(),2);
  }
 }
 @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
  var world=context.level();var random=context.random();var origin=context.origin();if(world.getLevel().dimension()!=Level.OVERWORLD)return false;
  var sample=origin.offset(16,0,16);sample=sample.atY(world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,sample.getX(),sample.getZ()));var biome=SourceFlora.biome(world,sample);
  for(int pass=0;pass<passes(biome);pass++){
   var yellow=new BlockPos(origin.getX()+random.nextInt(16)+8,random.nextInt(128),origin.getZ()+random.nextInt(16)+8);patch(world,random,yellow,-1,biome);
   if(random.nextInt(biome.swamp()?3:2)!=0)continue;
   var flower=new BlockPos(origin.getX()+random.nextInt(16)+8,random.nextInt(128),origin.getZ()+random.nextInt(16)+8);int subtype=SourceFlora.select(random,biome);if(subtype>=0)patch(world,random,flower,subtype,biome);
  }
  return true;
 }
}
