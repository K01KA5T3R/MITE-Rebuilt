package dev.koikaster.mite;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
/** Reforged's simplified fishing fields; these are not asserted to be R195 formulas. */
public final class FishingRules {
 public record Habitat(int waterCells,boolean sky,boolean ocean,boolean cold,boolean rain,boolean night){
  public boolean viable(){return waterCells>=18&&sky;}
 }
 public static Habitat habitat(ServerLevel level,BlockPos center){
  int water=0;
  // Fixed 75-cell budget; inspecting fishing never generates or loads a chunk.
  for(BlockPos pos:BlockPos.betweenClosed(center.offset(-2,-2,-2),center.offset(2,0,2)))
   if(level.hasChunkAt(pos)&&level.getFluidState(pos).is(FluidTags.WATER))water++;
  var biome=level.getBiome(center);
  // Exposure is geometric: sky brightness can lag block placement and is not a roof test.
  boolean sky=level.dimensionType().hasSkyLight()&&level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,center.getX(),center.getZ())<=center.getY()+1;
  return new Habitat(water,sky,biome.is(BiomeTags.IS_OCEAN),biome.value().getBaseTemperature()<.2f,level.isRainingAt(center.above()),level.isNight());
 }
 public static void adjust(ObjectArrayList<ItemStack> loot,ServerLevel level,BlockPos center,RandomSource random){
  Habitat habitat=habitat(level,center);
  if(!habitat.viable()){loot.clear();return;}
  boolean caughtFish=false;
  for(int i=0;i<loot.size();i++){
   ItemStack stack=loot.get(i);
   if(!isFish(stack))continue;
   caughtFish=true;float roll=random.nextFloat();
   Item fish=habitat.cold()?(roll<.75f?Items.SALMON:Items.COD):habitat.ocean()?(roll<.1f?Items.PUFFERFISH:roll<.25f?Items.TROPICAL_FISH:Items.COD):(roll<.5f?Items.SALMON:Items.COD);
   loot.set(i,new ItemStack(fish,stack.getCount()));
  }
  if(caughtFish&&random.nextFloat()<Rules.fishingBonusChance(habitat.rain(),habitat.night()))loot.add(new ItemStack(habitat.cold()?Items.SALMON:Items.COD));
 }
 static boolean isFish(ItemStack stack){return stack.is(Items.COD)||stack.is(Items.SALMON)||stack.is(Items.TROPICAL_FISH)||stack.is(Items.PUFFERFISH);}
 private FishingRules(){}
}
