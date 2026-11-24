package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;

/** R195 aon: state, legality and first-success random recovery/spread. */
public final class SourceGrass {
 public static final IntegerProperty TRAMPLINGS=IntegerProperty.create("tramplings",0,15);
 @SuppressWarnings("unchecked") public static void define(Object raw){((StateDefinition.Builder<Block,BlockState>)raw).add(TRAMPLINGS);}
 public static float effect(int count){return Mth.clamp((count-3)*.05f,0,.5f);}
 public static int mix(int rgb,int count){float e=effect(count);if(e==0)return rgb;float g=1-e;int r=(int)((rgb>>16&255)*g+134f*e),green=(int)((rgb>>8&255)*g+96f*e),b=(int)((rgb&255)*g+67f*e);return r<<16|green<<8|b;}
 public static boolean legal(LevelReader level,BlockPos pos){
  var up=pos.above();var above=level.getBlockState(up);var block=above.getBlock();
  if(above.isAir()||block instanceof LeavesBlock||block instanceof SnowLayerBlock||above.is(Blocks.SNOW_BLOCK))return true;
  // ast is the original piston base, including its extended form.
  if(block instanceof PistonBaseBlock)return false;
  return !above.isSolidRender(level,up)||!above.isFaceSturdy(level,up,Direction.DOWN);
 }
 public static boolean survives(Object state,Object level,Object pos){return legal((LevelReader)level,(BlockPos)pos);}
 static int opacity(LevelReader level,BlockPos pos){var state=level.getBlockState(pos);return state.is(Blocks.WATER)||state.is(Blocks.ICE)?3:state.getLightBlock(level,pos);}
 public static BlockState placement(Object raw,Object context){var c=(BlockPlaceContext)context;return legal(c.getLevel(),c.getClickedPos())?(BlockState)raw:Blocks.DIRT.defaultBlockState();}
 public static BlockState update(Object raw,Object access,Object rawPos){
  var level=(LevelAccessor)access;var pos=(BlockPos)rawPos;var state=(BlockState)raw;
  if(level.isClientSide())return state;
  if(!legal(level,pos))return Blocks.DIRT.defaultBlockState();
  if(level.getFluidState(pos.above()).is(Fluids.LAVA)){level.levelEvent(1501,pos,0);return Blocks.DIRT.defaultBlockState();}
  return state;
 }
 public static void randomTick(Object state,Object world,Object pos,Object random){tick((ServerLevel)world,(BlockPos)pos,(RandomSource)random,false);}
 /** Modern sleep skips time without simulating random ticks; the original fast-forward branch stays explicit. */
 static boolean tick(ServerLevel level,BlockPos pos,RandomSource random,boolean fastForward){
  if(!level.isAreaLoaded(pos,3))return false;
  var up=pos.above();int light=level.getMaxLocalRawBrightness(up);var above=level.getBlockState(up);
  if(light<4){if(opacity(level,up)>2&&!above.is(Blocks.ICE)&&!above.is(Blocks.SNOW_BLOCK))return level.setBlock(pos,Blocks.DIRT.defaultBlockState(),3);}
  else if(light>=13)for(int attempt=0;attempt<4;attempt++){
   var target=pos.offset(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
   if(!legal(level,target))continue;
   var roof=target.above();var ground=level.getBlockState(target);
   if(!(ground.is(Blocks.DIRT)||ground.is(Blocks.GRASS_BLOCK))||level.getMaxLocalRawBrightness(roof)<4||opacity(level,roof)>2)continue;
   if(ground.is(Blocks.GRASS_BLOCK)){if(fastForward)return false;int n=ground.getValue(TRAMPLINGS);return n>0&&level.setBlock(target,ground.setValue(TRAMPLINGS,n-1),2);}
   var fresh=Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyDirtBlock.SNOWY,level.getBlockState(roof).is(net.minecraft.tags.BlockTags.SNOW));return level.setBlock(target,fresh,3);
  }
  return false;
 }
 private SourceGrass(){}
}
