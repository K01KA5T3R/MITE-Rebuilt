package dev.koikaster.mite;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

/** aou brown variant: source cap/stem identity and metadata; no carryable giant-block item. */
public final class SourceGiantBrownMushroomBlock extends Block {
 public static final IntegerProperty META=IntegerProperty.create("source_meta",0,15);
 public SourceGiantBrownMushroomBlock(){super(Properties.of().mapColor(MapColor.WOOD).strength(.2f,.2f).sound(SoundType.WOOD));registerDefaultState(stateDefinition.any().setValue(META,14));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(META);}
 public ItemStack getCloneItemStack(BlockGetter world,BlockPos pos,BlockState state){return new ItemStack(Items.BROWN_MUSHROOM);}
 public List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){int count=loot.getLevel().random.nextInt(10)-7;return count>0?List.of(new ItemStack(Items.BROWN_MUSHROOM,count)):List.of();}
 public boolean dropFromExplosion(Explosion explosion){return false;}
 public void onBlockExploded(BlockState state,Level level,BlockPos pos,Explosion explosion){
  if(!level.isClientSide){int count=level.random.nextInt(10)-8;if(count>0)popResource(level,pos,new ItemStack(Items.BROWN_MUSHROOM,count));}
  super.onBlockExploded(state,level,pos,explosion);
 }
}
