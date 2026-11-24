package dev.koikaster.mite;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

/** aon.fertilize's128 attempts and separate item/world random sources. */
public final class SourceGrassFertilizer {
 private static final Random ITEM_RANDOM=LegacyItemRandom749.RANDOM;
 public static void perform(Object world,Object random,Object pos,Object state){grow((ServerLevel)world,(BlockPos)pos,ITEM_RANDOM,((ServerLevel)world).random);}
 static void grow(ServerLevel world,BlockPos pos,Random itemRand,RandomSource worldRand){
  for(int dx=-2;dx<=2;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-2;dz<=2;dz++){var at=pos.offset(dx,dy,dz);if(world.getBlockState(at).is(Blocks.GRASS_BLOCK)&&world.isEmptyBlock(at.above()))world.levelEvent(1505,at.above(),1);}
  attempts:for(int attempt=0;attempt<128;attempt++){
   int x=pos.getX(),y=pos.getY()+1,z=pos.getZ();
   for(int step=0;step<attempt/16;step++){
    x+=itemRand.nextInt(3)-1;y+=(itemRand.nextInt(3)-1)*itemRand.nextInt(3)/2;z+=itemRand.nextInt(3)-1;
    var at=new BlockPos(x,y,z);if(!world.getBlockState(at.below()).is(Blocks.GRASS_BLOCK)||world.getBlockState(at).isSolidRender(world,at))continue attempts;
   }
   var at=new BlockPos(x,y,z);if(!world.isEmptyBlock(at)||itemRand.nextInt(2)!=0)continue;
   if(itemRand.nextInt(10)!=0){if(SourceFlora.site(world,at))world.setBlock(at,Blocks.GRASS.defaultBlockState(),3);continue;}
   if(itemRand.nextInt(3)!=0){if(SourceFlora.site(world,at))world.setBlock(at,Blocks.DANDELION.defaultBlockState(),3);continue;}
   // The old retry loop never terminates when every subtype shares an illegal light/soil site.
   // Such a site cannot become legal by choosing another subtype; skip it without hanging the server.
   if(!SourceFlora.site(world,at))continue;int subtype=SourceFlora.select(worldRand,SourceFlora.biome(world,at));
   if(subtype==2&&itemRand.nextFloat()<.8f)subtype=-1;if(subtype>=0)world.setBlock(at,SourceFlora.block(subtype).defaultBlockState(),3);
  }
 }
 /** The original grass routine already emits the neighboring single-particle events. */
 public static void particles(Object world,int id,Object pos,int data){var level=(Level)world;var at=(BlockPos)pos;if(id==1505&&level.getBlockState(at).is(Blocks.GRASS_BLOCK))return;level.levelEvent(id,at,data);}
 private SourceGrassFertilizer(){}
}
