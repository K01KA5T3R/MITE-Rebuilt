package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** aqn undermining with the retained modern bubble-column feature. */
public final class SourceSoulSandBlock271 extends SoulSandBlock {
 public SourceSoulSandBlock271(BlockBehaviour.Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(SourceSoilProperties258.UNDERMINED,false).setValue(SourceSoilProperties258.PENDING,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(SourceSoilProperties258.UNDERMINED,SourceSoilProperties258.PENDING);}
 @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){SourceSoilControl271.onPlace(state,level,pos,old);super.onPlace(level.getBlockState(pos),level,pos,old,moved);}
 @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){SourceSoilControl271.tick(level,pos);var current=level.getBlockState(pos);if(current.is(this))super.tick(current,level,pos,random);}
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){SourceSoilControl271.placedBy(level,pos,state,placer,stack);}
 @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){SourceSoilControl271.walked(level,pos);}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){entity.setDeltaMovement(entity.getDeltaMovement().multiply(.4,1,.4));SourceSoilControl271.collided(level,pos);}
}
