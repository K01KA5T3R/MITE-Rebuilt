package dev.koikaster.mite;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;

/** pp.a uses nn's center-foot material sample as well as the full-water flag.
 * Box overlap alone may come from an adjacent deeper water column. */
public final class OriginalSwimmingWater {
 static boolean gate(PathfinderMob mob){if(mob.isInWater()&&!footInWater(mob)&&!fullWater(mob))return mob.isInLava();return mob.isInWater()||mob.isInLava();}
 private static boolean footInWater(PathfinderMob mob){var at=mob.blockPosition();var state=mob.level().getBlockState(at);var fluid=state.getFluidState();if(!fluid.is(FluidTags.WATER))return false;
  double surface;if(state.getBlock() instanceof LiquidBlock){int metadata=state.getValue(LiquidBlock.LEVEL);if(metadata>=8)metadata=0;float offset=(metadata+1)/9.0f-.11111111f;surface=(float)(at.getY()+1)-offset;}else surface=at.getY()+fluid.getHeight(mob.level(),at);
  double sample=mob.getY()+(double)mob.getEyeHeight()+(double)(-mob.getEyeHeight());return sample<surface;
 }
 private static boolean fullWater(PathfinderMob mob){var state=mob.level().getBlockState(mob.blockPosition());return state.is(Blocks.WATER)&&state.getValue(LiquidBlock.LEVEL)==0;}
 private OriginalSwimmingWater(){}
}
