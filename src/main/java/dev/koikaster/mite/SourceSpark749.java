package dev.koikaster.mite;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.RegistryObject;

/** Original internal ID246: one state, invisible, portal first, then two-tick ignition. */
public final class SourceSpark749 extends BaseFireBlock {
 public static final RegistryObject<SourceSpark749> BLOCK=Content.BLOCKS.register("spark",SourceSpark749::new);
 public static void register(){}
 private SourceSpark749(){super(BlockBehaviour.Properties.copy(Blocks.FIRE).randomTicks().noLootTable(),1f);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
 @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState();}
 @Override protected boolean canBurn(BlockState state){return state.is(Blocks.NETHERRACK)||OriginalFireSpread.encouragement(state,EmptyBlockGetter.INSTANCE,BlockPos.ZERO)>0;}
 @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return OriginalFireSpread.survives(level,pos);}
 @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moving){if(!level.isClientSide&&!canSurvive(state,level,pos))level.removeBlock(pos,false);}
 private SourceIgnitionRules749.Spark host(Level level,BlockPos pos){return new SourceIgnitionRules749.Spark(){
  public boolean portal(){
   if(!SourcePortalState.allowIgnition(level))return false;
   var shape=ForgeEventFactory.onTrySpawnPortal(level,pos,PortalShape.findEmptyPortalShape(level,pos,Direction.Axis.X));
   if(shape.isEmpty())return false;shape.get().createPortalBlocks();return !level.getBlockState(pos).is(SourceSpark749.this);
  }
  public void schedule(int ticks){level.scheduleTick(pos,SourceSpark749.this,ticks);}
  public boolean adjacentFuel(){return OriginalFireSpread.hasFuelAdjacent(level,pos);}
  public boolean netherrackBelow(){return level.getBlockState(pos.below()).is(Blocks.NETHERRACK);}
  public void fire(){var fire=Blocks.FIRE.defaultBlockState();level.setBlock(pos,fire.updateShape(Direction.DOWN,level.getBlockState(pos.below()),level,pos,pos.below()),3);}
  public void remove(){level.removeBlock(pos,false);}
 };}
 @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving){if(!level.isClientSide&&!previous.is(this))SourceIgnitionRules749.added(host(level,pos));}
 @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.getBlockState(pos).is(this))SourceIgnitionRules749.tick(host(level,pos));}
 @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
}
