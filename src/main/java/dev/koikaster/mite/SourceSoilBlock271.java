package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** anx source callbacks; no automatic FallingBlock neighbor scheduling. */
public final class SourceSoilBlock271 extends FallingBlock {
 public SourceSoilBlock271(BlockBehaviour.Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(SourceSoilProperties258.UNDERMINED,false).setValue(SourceSoilProperties258.PENDING,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(SourceSoilProperties258.UNDERMINED,SourceSoilProperties258.PENDING);}
 @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){SourceSoilControl271.onPlace(state,level,pos,old);}
 @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){return state;}
 @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){SourceSoilControl271.tick(level,pos);}
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){SourceSoilControl271.placedBy(level,pos,state,placer,stack);}
 @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){SourceSoilControl271.walked(level,pos);}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){SourceSoilControl271.collided(level,pos);}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){}
}
