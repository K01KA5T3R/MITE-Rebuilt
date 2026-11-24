package dev.koikaster.mite;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/** Native capture/place/dispense boundaries. Item metadata never grants source
 * placement: Ctrl is the existing server-owned, expiring input record. */
public final class BucketUse342 {
 public static final Item[] FILLED={Items.COD_BUCKET,Items.SALMON_BUCKET,Items.PUFFERFISH_BUCKET,Items.TROPICAL_FISH_BUCKET,Items.AXOLOTL_BUCKET,Items.POWDER_SNOW_BUCKET};
 private record Use(Player player,boolean ctrl){}
 private static final ThreadLocal<Use> USE=new ThreadLocal<>();
 public static ItemStack captured(ItemStack water,ItemStack filled,LivingEntity creature){
  if(creature.level() instanceof ServerLevel level)return BucketLifetime336.captured(water,filled,level,creature.blockPosition());
  return BucketLifetime336.vessel(water,filled);
 }
 /** The bridge invokes the original BucketItem.use, preserving native events,
  * permission checks, raycast, statistics, advancement and creative behavior. */
 public static InteractionResultHolder<ItemStack> useMob(Object raw,Level level,Player player,InteractionHand hand){
  var held=player.getItemInHand(hand);
  if(level instanceof ServerLevel server){var result=BucketLifetime336.update(held,server,player.blockPosition());if(result.changed()){player.setItemInHand(hand,result.stack());held=result.stack();}if(!(held.getItem() instanceof MobBucketItem))return held.getItem().use(level,player,hand);}
  if(level.isClientSide)ClientVesselControl.send(hand);
  var prior=USE.get();USE.set(new Use(player,!level.isClientSide&&ClassicVessels.takeControl(player,hand)));
  try{return ((BucketUseAccess342.Mob)raw).mite$bucketNativeUse(level,player,hand);}finally{if(prior==null)USE.remove();else USE.set(prior);}
 }
 public static ItemStack emptySuccess(ItemStack nativeResult,ItemStack filled,Player player){return filled.getItem() instanceof MobBucketItem&&!player.getAbilities().instabuild?BucketLifetime336.empty(filled):nativeResult;}
 public static boolean allowSpawn(Level level,ItemStack filled,BlockPos pos){return !(level instanceof ServerLevel server)||!BucketLifetime336.fish(filled)||BucketLifetime336.fish(BucketLifetime336.update(filled,server,pos).stack());}
 public static boolean emptyMob(Player player,Level world,BlockPos pos,BlockHitResult hit,ItemStack stack){
  if(!(world instanceof ServerLevel level))return true;
  // Existing water is retained, rather than replaced by a new temporary source.
  if(level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER))return true;
  var use=USE.get();boolean source=player!=null&&use!=null&&use.player()==player&&LegacyProduction.placeSource(true,player.isCreative(),use.ctrl(),PlayerState.data(player).getInt("Xp"));
  if(ClassicVessels.place(level,player,pos,Fluids.WATER,source))return true;
  return hit!=null&&ClassicVessels.place(level,player,hit.getBlockPos().relative(hit.getDirection()),Fluids.WATER,source);
 }
 public static InteractionResult usePowder(Object raw,UseOnContext context){
  var player=context.getPlayer();var original=context.getItemInHand().copy();var world=context.getLevel();var hand=context.getHand();
  if(world instanceof ServerLevel level){var result=BucketLifetime336.update(original,level,player==null?context.getClickedPos():player.blockPosition());if(result.changed()){
   if(player==null)return InteractionResult.FAIL;player.setItemInHand(hand,result.stack());
   var hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
   return ClassicVessels.useAt(level,player,hand,hit,ClassicVessels.takeControl(player,hand)).getResult();
  }}
  if(world.isClientSide&&player!=null)ClientVesselControl.send(hand);
  var result=((BucketUseAccess342.Powder)raw).mite$bucketNativePlace(context);
  if(result.consumesAction()&&player!=null&&!player.isCreative())player.setItemInHand(hand,BucketLifetime336.empty(original));
  return result;
 }
 /** null means this is not a powder-snow capture; the original fluid path runs. */
 public static InteractionResultHolder<ItemStack> collectPowder(ServerLevel level,Player player,InteractionHand hand,BlockPos pos){
  var state=level.getBlockState(pos);if(!state.is(Blocks.POWDER_SNOW))return null;
  var original=player.getItemInHand(hand);var pickup=(BucketPickup)state.getBlock();var nativeResult=pickup.pickupBlock(level,pos,state);if(nativeResult.isEmpty())return InteractionResultHolder.fail(original);
  var filled=BucketLifetime336.captured(original,nativeResult,level,player.blockPosition());
  player.awardStat(Stats.ITEM_USED.get(original.getItem()));pickup.getPickupSound(state).ifPresent(sound->player.playSound(sound,1,1));level.gameEvent(player,GameEvent.FLUID_PICKUP,pos);
  var result=ItemUtils.createFilledResult(original,player,filled);player.setItemInHand(hand,result);if(player instanceof ServerPlayer server)CriteriaTriggers.FILLED_BUCKET.trigger(server,filled);
  return InteractionResultHolder.success(result);
 }
 public static ItemStack collectPowder(BlockSource source,ItemStack original){
  var level=source.getLevel();var pos=source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));var state=level.getBlockState(pos);if(!state.is(Blocks.POWDER_SNOW))return null;
  var filled=((BucketPickup)state.getBlock()).pickupBlock(level,pos,state);if(filled.isEmpty())return null;
  filled=BucketLifetime336.captured(original,filled,level,pos);level.gameEvent(null,GameEvent.FLUID_PICKUP,pos);return exchange(source,original,filled);
 }
 private static ItemStack exchange(BlockSource source,ItemStack original,ItemStack result){original.shrink(1);if(original.isEmpty())return result;if(((DispenserBlockEntity)source.getEntity()).addItem(result)<0)new DefaultDispenseItemBehavior().dispense(source,result);return original;}
 private static final class Filled extends DefaultDispenseItemBehavior {
  @Override protected ItemStack execute(BlockSource source,ItemStack stack){
   var level=source.getLevel();var pos=source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));var result=BucketLifetime336.update(stack,level,source.getPos());stack=result.stack();
   if(BucketLifetime336.waterVessel(stack))return VesselDispense.INSTANCE.execute(source,stack);
   var filled=(DispensibleContainerItem)stack.getItem();if(filled.emptyContents(null,level,pos,null,stack)){filled.checkExtraContent(null,level,stack,pos);return exchange(source,stack,BucketLifetime336.empty(stack));}
   return super.execute(source,stack);
  }
 }
 public static void register(){var behavior=new Filled();for(var item:FILLED)DispenserBlock.registerBehavior(item,behavior);}
 private BucketUse342(){}
}
