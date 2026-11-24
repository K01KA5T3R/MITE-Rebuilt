package dev.koikaster.mite;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;

/** Native queries adapt source full opaque cubes; collision support alone never decides. */
public final class SourceMyceliumWorld implements OriginalMyceliumPopulation.World,OriginalGiantBrownMushroom.World {
 private final WorldGenLevel world;
 public SourceMyceliumWorld(WorldGenLevel world){this.world=world;}
 public int underworldYOffset(){return 120;}
 public boolean air(int x,int y,int z){return world.isEmptyBlock(new BlockPos(x,y,z));}
 public boolean alwaysSolidOpaqueCube(int x,int y,int z){
  var pos=new BlockPos(x,y,z);var state=world.getBlockState(pos);
  return SourcePhaseNativeWorld.solid(state)&&state.isSolidRender(world,pos)&&state.isCollisionShapeFullBlock(world,pos);
 }
 public boolean hugeMushroomBlock(int x,int y,int z){var block=world.getBlockState(new BlockPos(x,y,z)).getBlock();return block instanceof HugeMushroomBlock||block instanceof SourceGiantBrownMushroomBlock;}
 public boolean setMycelium(int x,int y,int z){return world.setBlock(new BlockPos(x,y,z),Blocks.MYCELIUM.defaultBlockState(),2);}
 public void markNaturalMycelium(int x,int z){SourceMyceliumFlags.mark(world.getChunk(x>>4,z>>4));}
 public boolean giantBrownMushroom(Random random,int x,int y,int z){return OriginalGiantBrownMushroom.generate(this,random,x,y,z);}
 public void setSmallBrownMushroom(int x,int y,int z){world.setBlock(new BlockPos(x,y,z),Blocks.BROWN_MUSHROOM.defaultBlockState(),2);}
 public int block(int x,int y,int z){
  var state=world.getBlockState(new BlockPos(x,y,z));if(state.isAir())return 0;
  if(state.getBlock() instanceof LeavesBlock)return 18;
  if(state.is(Blocks.DIRT))return 3;if(state.is(Blocks.GRASS_BLOCK))return 2;if(state.is(Blocks.MYCELIUM))return 110;
  return state.isSolidRender(world,new BlockPos(x,y,z))?1:20;
 }
 public boolean opaque(int token){return token==1||token==2||token==3||token==110;}
 public void setMushroom(int x,int y,int z,int token,int metadata){
  if(token!=99)throw new IllegalArgumentException("Source Underworld default afl generates brown mushrooms only");
  world.setBlock(new BlockPos(x,y,z),SourceUnderworldContent.GIANT_BROWN.get().defaultBlockState().setValue(SourceGiantBrownMushroomBlock.META,metadata),2);
 }
}
