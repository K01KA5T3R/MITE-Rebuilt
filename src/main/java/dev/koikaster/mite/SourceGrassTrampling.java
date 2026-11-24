package dev.koikaster.mite;

import java.util.*;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

/** EntityLivestock.c runtime-only cell memory, aon and BlockPlant/aqv callbacks. */
public final class SourceGrassTrampling {
 static final class Cell{int x,y,z;}
 static final Map<Animal,Cell> CELLS=Collections.synchronizedMap(new WeakHashMap<>());
 public static void afterAi(Animal animal){
  if(animal.level().isClientSide||!SourceLivestockSpook.handles(animal)||animal.isBaby()||!animal.onGround())return;
  var cell=CELLS.computeIfAbsent(animal,k->new Cell());if(animal.tickCount%1000==0)cell.x=cell.y=cell.z=0;
  int x=Mth.floor(animal.getX()),y=Mth.floor(animal.getY())-1,z=Mth.floor(animal.getZ());
  if(cell.x==x&&cell.y==y&&cell.z==z)return;cell.x=x;cell.y=y;cell.z=z;
  var pos=new BlockPos(x,y,z);if(animal.level().getBlockState(pos).is(Blocks.GRASS_BLOCK))trample(animal.level(),pos,animal,Math::random);else SourcePlantTrampling619.trample(animal.level(),pos,animal);
 }
 static void trample(Level level,BlockPos pos,Animal animal,DoubleSupplier random){
  var state=level.getBlockState(pos);if(level.isClientSide||!SourceLivestockSpook.handles(animal)||!state.is(Blocks.GRASS_BLOCK))return;
  int count=state.getValue(SourceGrass.TRAMPLINGS);if(count<15){count++;level.setBlock(pos,state.setValue(SourceGrass.TRAMPLINGS,count),2);}
  float effect=SourceGrass.effect(count);if(animal instanceof Cow)effect*=2;
  if(effect>=.2f&&random.getAsDouble()<effect*2)SourcePlantTrampling619.trample(level,pos.above(),animal);
 }
 private SourceGrassTrampling(){}
}
