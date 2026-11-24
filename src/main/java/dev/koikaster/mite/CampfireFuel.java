package dev.koikaster.mite;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Fixed human rule: five minutes at20TPS, then one charcoal to relight.
 * Store an absolute world-tick deadline, so unloading never resets the fuel.
 * Cooking time remains the existing600ticks, matching vanilla's30seconds.
 */
public final class CampfireFuel {
 public static final int BURN_TICKS=6000;
 private static final String END="MiteCampfireBurnUntil046";
 static boolean use(PlayerInteractEvent.RightClickBlock event,CampfireBlockEntity be){
  if(!event.getItemStack().is(Items.CHARCOAL))return false;
  event.setCanceled(true);var state=be.getBlockState();var level=event.getLevel();
  if(state.getValue(CampfireBlock.WATERLOGGED)){event.setCancellationResult(InteractionResult.FAIL);return true;}
  event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
  if(level.isClientSide||state.getValue(CampfireBlock.LIT))return true;
  if(!event.getEntity().isCreative())event.getItemStack().shrink(1);
  be.getPersistentData().putLong(END,level.getGameTime()+BURN_TICKS);be.setChanged();
  level.setBlock(event.getPos(),state.setValue(CampfireBlock.LIT,true),3);
  level.playSound(null,event.getPos(),SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,1,1);
  level.gameEvent(event.getEntity(),GameEvent.BLOCK_CHANGE,event.getPos());
  return true;
 }
 static boolean burning(Level level,BlockPos pos,CampfireBlockEntity be){
  var state=be.getBlockState();if(!state.getValue(CampfireBlock.LIT))return false;
  var data=be.getPersistentData();if(!data.contains(END)){data.putLong(END,level.getGameTime()+BURN_TICKS);be.setChanged();}
  if(level.getGameTime()<data.getLong(END)&&!state.getValue(CampfireBlock.WATERLOGGED))return true;
  data.putLong(END,0);be.setChanged();CampfireBlock.dowse(null,level,pos,state);level.setBlock(pos,state.setValue(CampfireBlock.LIT,false),3);return false;
 }
 static void cold(CampfireBlockEntity be){if(be.getPersistentData().getLong(END)!=0){be.getPersistentData().putLong(END,0);be.setChanged();}}
 private CampfireFuel(){}
}
