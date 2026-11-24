package dev.koikaster.mite;
import net.minecraft.core.*;import net.minecraft.resources.ResourceKey;import net.minecraft.server.level.ServerLevel;import net.minecraft.util.RandomSource;import net.minecraft.world.item.context.BlockPlaceContext;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;import net.minecraft.world.phys.shapes.*;
/** Native IDs stay brown_mushroom/red_mushroom; source age and legal/growth controls own their behavior. */
public final class SourceSmallMushroomBlock599 extends MushroomBlock {
 private static final VoxelShape SOURCE_SHAPE=Block.box((.5f-.2f)*16d,0,(.5f-.2f)*16d,(.5f+.2f)*16d,.4f*16d,(.5f+.2f)*16d);
 public SourceSmallMushroomBlock599(BlockBehaviour.Properties properties,ResourceKey<ConfiguredFeature<?,?>> feature){super(properties.lightLevel(state->feature.location().getPath().equals("huge_brown_mushroom")?2:0),feature);registerDefaultState(stateDefinition.any().setValue(SourceMushroomProperties599.GROWTH,0));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(SourceMushroomProperties599.GROWTH);}
 int type(){return this==Blocks.BROWN_MUSHROOM?39:40;}
 public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return SOURCE_SHAPE;}
 public boolean canSurvive(BlockState state,LevelReader world,BlockPos pos){return SourceSmallMushroom587.legal(new SourceMushroomWorld599(world,null,null),type(),pos.getX(),pos.getY(),pos.getZ(),state.getValue(SourceMushroomProperties599.GROWTH));}
 public BlockState getStateForPlacement(BlockPlaceContext context){var host=new SourceMushroomWorld599(context.getLevel(),context.getPlayer(),null,context);var p=context.getClickedPos();return SourceSmallMushroom587.canPlace(host,type(),p.getX(),p.getY(),p.getZ(),0)?defaultBlockState():null;}
 public void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random){var host=new SourceMushroomWorld599(world,null,random);SourceSmallMushroom587.tick(host,type(),pos.getX(),pos.getY(),pos.getZ(),host.rng());}
 public BlockState updateShape(BlockState state,Direction direction,BlockState other,LevelAccessor world,BlockPos pos,BlockPos otherPos){return state;}
 public void neighborChanged(BlockState state,Level world,BlockPos pos,Block block,BlockPos from,boolean moved){if(world instanceof ServerLevel server&&!canSurvive(state,world,pos)){var host=new SourceMushroomWorld599(server,null,server.random);SourceSmallMushroom587.tick(host,type(),pos.getX(),pos.getY(),pos.getZ(),host.rng());}}
 public boolean isValidBonemealTarget(LevelReader world,BlockPos pos,BlockState state,boolean client){return false;}public boolean isBonemealSuccess(Level world,RandomSource random,BlockPos pos,BlockState state){return false;}public void performBonemeal(ServerLevel world,RandomSource random,BlockPos pos,BlockState state){}
 public boolean growMushroom(ServerLevel world,BlockPos pos,BlockState state,RandomSource random){var host=new SourceMushroomWorld599(world,null,random);return SourceSmallMushroom587.giant(host,type(),pos.getX(),pos.getY(),pos.getZ(),null,host.rng());}
}
