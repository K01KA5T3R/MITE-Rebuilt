package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** BlockMantleOrCore and aqz registration. Two native block IDs carry the old
 * metadata0/1 identities; the fire walk and its Random calls remain source. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class SourceMantleBlock extends Block {
 public final boolean core;
 public SourceMantleBlock(boolean core){super(BlockBehaviour.Properties.of().mapColor(core?MapColor.COLOR_RED:MapColor.COLOR_ORANGE).strength(-1,6000000).sound(SoundType.STONE).lightLevel(state->13).randomTicks().noLootTable().pushReaction(PushReaction.BLOCK));this.core=core;}
 @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,Entity entity){return false;}
 @SubscribeEvent public static void breakAttempt(BlockEvent.BreakEvent event){if(event.getState().getBlock() instanceof SourceMantleBlock)event.setCanceled(true);}
 /** ape.m is Material.canCatchFire, explicitly excluding netherrack. Native
  * ignitedByLava carries that material flag for modern block counterparts. */
 private static boolean catches(Level level,BlockPos pos){var state=level.getBlockState(pos);return !state.is(Blocks.NETHERRACK)&&state.ignitedByLava();}
 @Override public void randomTick(BlockState state,ServerLevel level,BlockPos origin,RandomSource random){
  int x=origin.getX(),y=origin.getY(),z=origin.getZ(),steps=random.nextInt(3);
  for(int attempt=0;attempt<steps;attempt++){
   x+=random.nextInt(3)-1;++y;z+=random.nextInt(3)-1;var at=new BlockPos(x,y,z);var block=level.getBlockState(at);
   if(block.isAir()){
    boolean fuel=false;for(var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH,Direction.DOWN,Direction.UP})if(catches(level,at.relative(direction))){fuel=true;break;}
    if(!fuel)continue;level.setBlock(at,Blocks.FIRE.defaultBlockState(),3);return;
   }
   if(SourcePhaseNativeWorld.solid(block))return;
  }
  if(steps==0){int originalX=x,originalZ=z;for(int attempt=0;attempt<3;attempt++){x=originalX+random.nextInt(3)-1;z=originalZ+random.nextInt(3)-1;var at=new BlockPos(x,y,z);if(level.isEmptyBlock(at.above())&&catches(level,at))level.setBlock(at.above(),Blocks.FIRE.defaultBlockState(),3);}}
 }
 /** Source abw.isBottomBlock is dimension-specific, not every bedrock layer. */
 public static boolean bottom(Level level,BlockPos pos){var state=level.getBlockState(pos);return level.dimension()==Level.OVERWORLD?state.is(Blocks.BEDROCK):level.dimension()==WorldEvents.UNDERWORLD?state.is(SourceUnderworldContent.MANTLE.get()):level.dimension()==Level.NETHER&&state.is(SourceUnderworldContent.CORE.get());}
}
