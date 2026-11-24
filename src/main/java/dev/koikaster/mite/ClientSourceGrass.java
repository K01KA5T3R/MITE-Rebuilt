package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSourceGrass {
 public static int color(BlockState state,BlockAndTintGetter world,BlockPos pos,int tint){
  if(world==null||pos==null)return net.minecraft.world.level.GrassColor.get(.5,1);
  LevelReader biomes=world instanceof LevelReader reader?reader:world instanceof SourceGrassBiomeAccess region?region.mite$grassBiomes():null;
  int rgb;
  if(biomes==null)rgb=BiomeColors.getAverageGrassColor(world,pos);
  else {int red=0,green=0,blue=0;for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){var at=pos.offset(x,0,z);int c=biomes.getBiome(at).value().getGrassColor(at.getX(),at.getZ());red+=c>>16&255;green+=c>>8&255;blue+=c&255;}rgb=(red/9&255)<<16|(green/9&255)<<8|blue/9&255;}
  return SourceGrass.mix(rgb,state.getValue(SourceGrass.TRAMPLINGS));
 }
 @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block event){event.register(ClientSourceGrass::color,Blocks.GRASS_BLOCK);}
 private ClientSourceGrass(){}
}
