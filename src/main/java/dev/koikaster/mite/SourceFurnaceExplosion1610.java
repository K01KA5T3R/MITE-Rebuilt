package dev.koikaster.mite;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Blocks;import net.minecraft.world.level.block.state.BlockState;import net.minecraftforge.registries.ForgeRegistries;
/** aok delegates exploded furnaces to a material model; ovens have no block drop. */
public final class SourceFurnaceExplosion1610 {
 private static int model(BlockState state){
  var key=ForgeRegistries.BLOCKS.getKey(state.getBlock());if(key==null)return -1;
  if(key.getNamespace().equals("minecraft"))return switch(key.getPath()){case "furnace","smoker","blast_furnace"->2;default->-1;};
  if(!key.getNamespace().equals(MiteReforged.ID)||!(state.getBlock() instanceof ThermalFurnace.BlockImpl))return -1;
  return switch(key.getPath()){case "clay_oven","hardened_clay_oven"->0;case "sandstone_oven"->1;case "stone_furnace"->2;case "obsidian_furnace"->3;case "netherrack_furnace"->4;default->-1;};
 }
 static boolean drop(SourceExplosion explosion,BlockPos pos,BlockState captured,boolean currentMetadata){
  int model=model(captured);if(model<0)return false;if(model==0)return true;
  var block=switch(model){case 1->Blocks.SANDSTONE;case 2->Blocks.COBBLESTONE;case 3->Blocks.OBSIDIAN;case 4->Blocks.NETHERRACK;default->throw new IllegalStateException("Furnace material model");};
  SourceExplosionDrops.drop(explosion,pos,block.defaultBlockState(),currentMetadata);return true;
 }
 private SourceFurnaceExplosion1610(){}
}
