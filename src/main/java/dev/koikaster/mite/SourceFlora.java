package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.*;

/** R195 BlockPlant/ane/BlockFlowerMulti; original metadata uses the matching modern registry entries. */
public final class SourceFlora {
 static final int[] TYPES={0,1,2,5,7,8};
 record BiomeInfo(boolean plains,boolean swamp,boolean jungle,float temperature){}
 static BiomeInfo biome(LevelReader level,BlockPos pos){var b=level.getBiome(pos);return new BiomeInfo(b.is(Biomes.PLAINS)||b.is(Biomes.SUNFLOWER_PLAINS),b.is(Biomes.SWAMP)||b.is(Biomes.MANGROVE_SWAMP),b.is(BiomeTags.IS_JUNGLE),b.value().getBaseTemperature());}
 public static int type(Block b){if(b==null)return -1;if(b==Blocks.POPPY)return 0;if(b==Blocks.BLUE_ORCHID)return 1;if(b==Blocks.ALLIUM)return 2;if(b==Blocks.ORANGE_TULIP)return 5;if(b==Blocks.PINK_TULIP)return 7;if(b==Blocks.OXEYE_DAISY)return 8;return -1;}
 static Block block(int type){return switch(type){case 0->Blocks.POPPY;case 1->Blocks.BLUE_ORCHID;case 2->Blocks.ALLIUM;case 5->Blocks.ORANGE_TULIP;case 7->Blocks.PINK_TULIP;case 8->Blocks.OXEYE_DAISY;default->throw new IllegalArgumentException("Invalid original flower subtype "+type);};}
 static boolean original(Block block){return block!=null&&(block==Blocks.DANDELION||type(block)>=0);}
 static boolean suitable(BiomeInfo b,int type){
  if(type!=0&&type!=1&&type!=2&&type!=5&&type!=7&&type!=8)return false;
  if(type==2&&!b.swamp()||b.swamp()&&type!=2)return false;
  if(type==1&&b.temperature()<=.8f)return false;
  if((type==5||type==7)&&b.temperature()<.7f)return false;
  return !b.jungle()||type!=8;
 }
 static int select(RandomSource random,BiomeInfo biome){if(biome.plains()&&random.nextInt(2)==0)return 8;int[] candidates=new int[6];int size=0;for(int t:TYPES)if(suitable(biome,t))candidates[size++]=t;return size==0?-1:candidates[random.nextInt(size)];}
 /** Worldgen lighting is not initialized yet: use the old opacity-column sky test there. */
 static boolean sky(LevelReader level,BlockPos pos){
  if(!level.dimensionType().hasSkyLight())return false;if(level instanceof Level live)return live.canSeeSky(pos);
  int top=level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ());if(pos.getY()>=top)return true;
  var at=pos.mutable();for(int y=pos.getY();y<top;y++){at.setY(y);if(level.getBlockState(at).getLightBlock(level,at)>0)return false;}return true;
 }
 static boolean site(LevelReader level,BlockPos pos){var below=level.getBlockState(pos.below());return (below.is(Blocks.GRASS_BLOCK)||below.is(Blocks.DIRT)||below.is(Blocks.FARMLAND))&&(sky(level,pos)||level.getRawBrightness(pos,0)>=8);}
 public static boolean survives(boolean original,Object rawState,Object rawLevel,Object rawPos){var state=(BlockState)rawState;if(!original(state.getBlock()))return SourceModernFlowerParent539.small(state.getBlock())?site((LevelReader)rawLevel,(BlockPos)rawPos):original;var level=(LevelReader)rawLevel;var pos=(BlockPos)rawPos;int type=type(state.getBlock());return site(level,pos)&&(type<0||suitable(biome(level,pos),type));}
 public static BlockState afterUpdate(Object rawState,Object rawWorld,Object rawPos){var result=(BlockState)rawState;if(result.isAir()&&rawWorld instanceof net.minecraft.server.level.ServerLevel level){var pos=(BlockPos)rawPos;if((original(level.getBlockState(pos).getBlock())||SourceModernFlowerParent539.managed(level.getBlockState(pos).getBlock()))&&level.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,16,false)==null)level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}return result;}
 public static VoxelShape shape(Object rawShape,Object rawState,Object rawLevel,Object rawPos){
  var state=(BlockState)rawState;if(!original(state.getBlock()))return (VoxelShape)rawShape;
  int t=type(state.getBlock());double width=t==1?.3:.2,height=switch(t){case 1,7,8->.8125;case 2->.875;case 5->.75;default->.6;};var offset=state.getOffset((BlockGetter)rawLevel,(BlockPos)rawPos);
  return Shapes.box(.5-width,0,.5-width,.5+width,height,.5+width).move(offset.x,offset.y,offset.z);
 }
 /** ane's override differs from the generic BlockPlant: pigs and sheep also consume flower drops. */
 static boolean flowerDrops(Block plant,Animal animal){return !(animal instanceof Cow||animal instanceof Pig||animal instanceof Sheep);}
 public static boolean worldgenSurvives(Object rawState,Object rawWorld,Object rawPos){var state=(BlockState)rawState;return !original(state.getBlock())&&state.canSurvive((LevelReader)rawWorld,(BlockPos)rawPos);}
 private SourceFlora(){}
}
