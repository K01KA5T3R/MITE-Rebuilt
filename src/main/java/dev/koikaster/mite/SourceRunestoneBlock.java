package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

/** Original BlockRunestone/apq. aqz.b(float) is a no-op: both strengths are2.4. */
public final class SourceRunestoneBlock extends Block {
 public static final IntegerProperty RUNE=IntegerProperty.create("rune",0,15);
 public final boolean adamantium;
 public SourceRunestoneBlock(boolean adamantium){
  super(Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.4f,2.4f).sound(SoundType.STONE).requiresCorrectToolForDrops());
  this.adamantium=adamantium;registerDefaultState(stateDefinition.any().setValue(RUNE,0));
 }
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(RUNE);}
 public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state){return SourceRunestoneItem.select(new ItemStack(this),state.getValue(RUNE));}
 public List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){return List.of(SourceRunestoneItem.select(new ItemStack(this),state.getValue(RUNE)));}
 public boolean dropFromExplosion(Explosion explosion){return false;}
 public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion){
  if(!level.isClientSide)for(int i=0;i<6;i++)if(level.random.nextFloat()<.5f)popResource(level,pos,new ItemStack(Content.OBSIDIAN_SHARD.get()));
  super.onBlockExploded(state,level,pos,explosion);
 }
 public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){schedule(level,pos);}
 public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){schedule(level,pos);super.onRemove(state,level,pos,next,moving);}
 private static void schedule(Level level,BlockPos pos){
  if(level.isClientSide)return;
  for(int y:new int[]{1,-1})for(int horizontal:new int[]{-1,1}){
   for(BlockPos at:new BlockPos[]{pos.offset(horizontal,y,0),pos.offset(0,y,horizontal)})
    if(level.getBlockState(at).is(Blocks.NETHER_PORTAL))level.scheduleTick(at,Blocks.NETHER_PORTAL,1);
  }
 }
}
