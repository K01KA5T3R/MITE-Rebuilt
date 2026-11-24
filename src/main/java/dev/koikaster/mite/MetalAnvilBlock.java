package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.List;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;
/** amv falling and carried damage; modern native anvil model/shape only. */
public final class MetalAnvilBlock extends FallingBlock implements EntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 public static final IntegerProperty DAMAGE=IntegerProperty.create("damage",0,2);
 public final String material;
 public MetalAnvilBlock(String material,Properties properties){super(properties);this.material=material;registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH).setValue(DAMAGE,0));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,DAMAGE);}
 public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
 public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING,state.getValue(FACING)).getShape(level,pos,context);}
 public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
 public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
 public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SourceAnvil(pos,state);}
 public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(level.getBlockEntity(pos) instanceof SourceAnvil entity){if(stack.hasCustomHoverName())entity.name=stack.getHoverName();int damage=stack.getTagElement("BlockEntityTag")==null?0:stack.getTagElement("BlockEntityTag").getInt("damage");entity.setDamage(damage);}}
 public List<ItemStack> getDrops(BlockState state,LootParams.Builder params){var entity=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);return List.of(entity instanceof SourceAnvil anvil?anvil.carried():new ItemStack(asItem()));}
 public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state){return level.getBlockEntity(pos) instanceof SourceAnvil anvil?anvil.carried():new ItemStack(asItem());}
 public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(FallingBlock.isFree(level.getBlockState(pos.below()))&&pos.getY()>=level.getMinBuildHeight())SourceAnvil.Falling.start(level,pos,state);}
 public void onLand(Level level,BlockPos pos,BlockState state,BlockState replaced,net.minecraft.world.entity.item.FallingBlockEntity entity){level.levelEvent(1031,pos,0);}
}
