package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

/** aoc/vm. Passability adapts the original !block.isSolid and excludes liquids. */
public final class OriginalEnchantingTables {
 public static boolean table(BlockState state){return state.is(Blocks.ENCHANTING_TABLE)||state.getBlock() instanceof EmeraldTable;}
 public static float resistance(Object block){return 2.4f;}
 public static boolean passable(Level level,BlockPos pos){if(level.isOutsideBuildHeight(pos))return true;if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);return state.getFluidState().isEmpty()&&!state.isSolid();}
 public static int bookshelves(Level level,BlockPos pos){int count=0;for(int dy=0;dy<=1&&(dy!=1||passable(level,pos.above()));dy++){boolean[][] accessible=new boolean[5][5];
  if(passable(level,pos.offset(0,dy,-1))){accessible[2][0]=true;if(passable(level,pos.offset(-1,dy,-1))){accessible[0][1]=true;accessible[1][0]=true;}if(passable(level,pos.offset(1,dy,-1))){accessible[3][0]=true;accessible[4][1]=true;}}
  if(passable(level,pos.offset(1,dy,0))){accessible[4][2]=true;if(passable(level,pos.offset(1,dy,-1))){accessible[3][0]=true;accessible[4][1]=true;}if(passable(level,pos.offset(1,dy,1))){accessible[4][3]=true;accessible[3][4]=true;}}
  if(passable(level,pos.offset(0,dy,1))){accessible[2][4]=true;if(passable(level,pos.offset(1,dy,1))){accessible[4][3]=true;accessible[3][4]=true;}if(passable(level,pos.offset(-1,dy,1))){accessible[1][4]=true;accessible[0][3]=true;}}
  if(passable(level,pos.offset(-1,dy,0))){accessible[0][2]=true;if(passable(level,pos.offset(-1,dy,1))){accessible[1][4]=true;accessible[0][3]=true;}if(passable(level,pos.offset(-1,dy,-1))){accessible[0][1]=true;accessible[1][0]=true;}}
  for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(accessible[2+dx][2+dz]&&level.getBlockState(pos.offset(dx,dy,dz)).is(Blocks.BOOKSHELF))count++;
 }return count;}
 public static InteractionResult use(Object rawLevel,Object rawPos,Object rawPlayer){var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var player=(Player)rawPlayer;if(!passable(level,pos.above()))return InteractionResult.PASS;if(player instanceof ServerPlayer server){boolean emerald=level.getBlockState(pos).getBlock() instanceof EmeraldTable;var title=level.getBlockEntity(pos) instanceof EnchantmentTableBlockEntity entity?entity.getDisplayName():net.minecraft.network.chat.Component.translatable("container.enchant");NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->new SourceEnchantMenu(id,inv,pos,emerald),title),buffer->{buffer.writeBlockPos(pos);buffer.writeBoolean(emerald);});}return InteractionResult.sidedSuccess(level.isClientSide);}
 public static final class EmeraldTable extends EnchantmentTableBlock {
  public EmeraldTable(Properties properties){super(properties);}
  public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EmeraldEntity(pos,state);}
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?createTickerHelper(type,Content.EMERALD_ENCHANTING_ENTITY.get(),EnchantmentTableBlockEntity::bookAnimationTick):null;}
 }
 public static final class EmeraldEntity extends EnchantmentTableBlockEntity {
  public EmeraldEntity(BlockPos pos,BlockState state){super(pos,state);}
  public BlockEntityType<?> getType(){return Content.EMERALD_ENCHANTING_ENTITY.get();}
 }
 private OriginalEnchantingTables(){}
}
