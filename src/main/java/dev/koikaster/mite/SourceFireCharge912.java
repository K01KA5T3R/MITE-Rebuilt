package dev.koikaster.mite;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;

/** The original fire-charge action on the existing native item and Forge placement path. */
public final class SourceFireCharge912 {
 /** Null retains the previously selected modern candle lighting branch. */
 public static InteractionResult block(Object raw){
  var context=(UseOnContext)raw;var stack=context.getItemInHand();
  if(!stack.is(Items.FIRE_CHARGE))return null;
  var player=context.getPlayer();if(player==null||player.isSpectator())return InteractionResult.FAIL;
  var level=context.getLevel();var clicked=context.getClickedPos();
  if(level.isOutsideBuildHeight(clicked)||!level.getWorldBorder().isWithinBounds(clicked)||!level.hasChunkAt(clicked))return InteractionResult.FAIL;
  var state=level.getBlockState(clicked);
  if(state.isAir()||state.getBlock() instanceof BaseFireBlock||state.getBlock() instanceof LiquidBlock)return InteractionResult.FAIL;
  // Campfire.canLight is already disabled by the existing charcoal-only relighting owner.
  if(CampfireBlock.canLight(state)||CandleBlock.canLight(state)||CandleCakeBlock.canLight(state))return null;
  if(!level.mayInteract(player,clicked))return InteractionResult.FAIL;
  var next=clicked.relative(context.getClickedFace());
  boolean accepted=SourceFireChargeRules912.use(new SourceFireChargeRules912.Use(){
   public boolean blockHit(){return true;}
   public boolean neighborAir(){return level.isEmptyBlock(next);}
   public boolean editNeighbor(){return !level.isOutsideBuildHeight(next)&&level.getWorldBorder().isWithinBounds(next)&&level.hasChunkAt(next)&&level.mayInteract(player,next)&&player.mayUseItemAt(next,context.getClickedFace(),stack);}
   public boolean client(){return level.isClientSide;}
   public boolean creative(){return player.getAbilities().instabuild;}
   public void swing(){} // The sided-success result supplies the native client swing.
   public void sound(){level.playSound(null,next,SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,1f,LegacyItemRandom749.RANDOM.nextFloat()*.4f+.8f);}
   public void fire(){level.setBlock(next,Blocks.FIRE.defaultBlockState(),3);}
   public void consume(){SourceHeldConversion1037.convert(player,context.getHand(),net.minecraft.world.item.ItemStack.EMPTY);}
  });
  return accepted?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.FAIL;
 }
 /** Original TNT has no fire-charge activation: allow ItemStack's normal placement transaction. */
 public static InteractionResult tnt(Object rawPlayer,Object rawHand){
  var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;
  return player.getItemInHand(hand).is(Items.FIRE_CHARGE)?InteractionResult.PASS:null;
 }
 private SourceFireCharge912(){}
}
