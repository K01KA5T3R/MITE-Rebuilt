package dev.koikaster.mite.visual;

import dev.koikaster.mite.MiteReforged;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** acq.H=white and adi.H=14745518; source water PNG supplies its blue. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClassicWater {
 public static int color(net.minecraft.world.level.BlockAndTintGetter world,net.minecraft.core.BlockPos pos){
  if(world==null||pos==null)return ClassicVisuals.water()?0xffffff:4159204;
  if(!ClassicVisuals.water())return BiomeColors.getAverageWaterColor(world,pos);
  net.minecraft.world.level.LevelReader biomes=world instanceof net.minecraft.world.level.LevelReader reader?reader:world instanceof dev.koikaster.mite.SourceGrassBiomeAccess region?region.mite$grassBiomes():null;
  if(biomes==null)return BiomeColors.getAverageWaterColor(world,pos);
  // apc.c samples exactly nine biome colors; ClientLevel only caches registered native resolvers.
  int red=0,green=0,blue=0;
  for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
   int nativeTint=biomes.getBiome(pos.offset(x,0,z)).value().getWaterColor();
   int tint=nativeTint==6388580||nativeTint==3832426?14745518:0xffffff;
   red+=tint>>16&255;green+=tint>>8&255;blue+=tint&255;
  }
  return (red/9&255)<<16|(green/9&255)<<8|blue/9&255;
 }
 /** Forge's liquid mesh reads FluidType tint rather than the block color registry. */
 public static int fluidTint(int nativeColor,Object rawFluid,Object rawWorld,Object rawPos){
  if(!ClassicVisuals.water()||!(rawFluid instanceof net.minecraft.world.level.material.FluidState fluid))return nativeColor;
  var type=fluid.getType();
  if(type!=net.minecraft.world.level.material.Fluids.WATER&&type!=net.minecraft.world.level.material.Fluids.FLOWING_WATER)return nativeColor;
  return (nativeColor&0xff000000)|color((net.minecraft.world.level.BlockAndTintGetter)rawWorld,(net.minecraft.core.BlockPos)rawPos);
 }
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block event){
  event.register((state,world,pos,tint)->color(world,pos),Blocks.WATER,Blocks.BUBBLE_COLUMN);
 }
 private ClassicWater(){}
}
