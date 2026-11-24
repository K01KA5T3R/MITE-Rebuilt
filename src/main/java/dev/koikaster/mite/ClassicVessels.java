package dev.koikaster.mite;
import java.util.Locale;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** R195 wr/ItemVessel and jv/ss water-conversion branches, adapted to native items, offhand and waterlogging. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ClassicVessels {
 private static final String CONTROL="MiteVesselControl";
 public static boolean handled(ItemStack stack){return stack.getItem() instanceof SpecialItems.MetalBucket||stack.is(Items.BUCKET)||stack.is(Items.WATER_BUCKET)||stack.is(Items.LAVA_BUCKET)||stack.is(Items.MILK_BUCKET);}
 public static boolean handlesUse(Object raw, Object rawPlayer,Object rawHand){var p=(Player)rawPlayer;var s=p.getItemInHand((InteractionHand)rawHand);return handled(s)&&!s.is(Items.MILK_BUCKET);}
 public static boolean handlesEmpty(Object item){return item==Items.WATER_BUCKET||item==Items.LAVA_BUCKET;}
 public static boolean nativeEmpty(Object item,Object rawPlayer,Object rawLevel,Object rawPos,Object rawHit,Object ignoredContainer){if(!(rawLevel instanceof ServerLevel level))return true;var player=(Player)rawPlayer;var fluid=item==Items.WATER_BUCKET?Fluids.WATER:Fluids.LAVA;var pos=(BlockPos)rawPos;boolean source=player!=null&&player.isCreative();if(place(level,player,pos,fluid,source))return true;return rawHit instanceof BlockHitResult hit&&place(level,player,hit.getBlockPos().relative(hit.getDirection()),fluid,source);}
 public static String fluid(ItemStack stack){if(stack.getItem() instanceof SpecialItems.MetalBucket bucket)return bucket.fluid(stack);return stack.is(Items.WATER_BUCKET)?"water":stack.is(Items.LAVA_BUCKET)?"lava":stack.is(Items.MILK_BUCKET)?"milk":"";}
 public static String material(ItemStack stack){return stack.getItem() instanceof SpecialItems.MetalBucket bucket?bucket.material:"iron";}
 public static float melting(ItemStack stack){String material=material(stack);if(material.equals("holy_steel"))return 0;try{return LegacyProduction.meltingChance(LegacyProduction.Metal.valueOf(material.toUpperCase(Locale.ROOT)));}catch(IllegalArgumentException ignored){return .01f*(64f/LegacyToolWear.durability(material));}}
 public static ItemStack peer(ItemStack stack,String fluid) {
  boolean nativePeer = !(stack.getItem() instanceof SpecialItems.MetalBucket)
      || stack.hasTag() && stack.getTag().getBoolean("NativeVessel");
  boolean nativeOutput = nativePeer && !fluid.equals("stone");
  Item output = nativeOutput ? switch (fluid) {
   case "water" -> Items.WATER_BUCKET;
   case "lava" -> Items.LAVA_BUCKET;
   case "milk" -> Items.MILK_BUCKET;
   default -> Items.BUCKET;
  } : stack.getItem() instanceof SpecialItems.MetalBucket ? stack.getItem() : Content.get("iron_bucket");

  ItemStack result = stack.is(output) ? stack.copyWithCount(1) : new ItemStack(output);
  var tag = stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
  tag.remove("Fluid");
  tag.remove("NativeVessel");
  tag.remove(BucketLifetime336.LIFE);
  tag.remove(BucketLifetime336.VESSEL);
  if (!nativeOutput) {
   if (!fluid.isEmpty()) tag.putString("Fluid", fluid);
   if (nativePeer) tag.putBoolean("NativeVessel", true);
  }
  result.setTag(tag.isEmpty() ? null : tag);
  return result;
 }
 public static void control(ServerPlayer player,InteractionHand hand,boolean ctrl){var data=player.getPersistentData().getCompound(CONTROL);data.putInt("Tick",player.tickCount);data.putString("Hand",hand.name());data.putBoolean("Ctrl",ctrl);player.getPersistentData().put(CONTROL,data);}
 private static boolean control(Player player,InteractionHand hand){var root=player.getPersistentData();var data=root.getCompound(CONTROL);root.remove(CONTROL);return data.getBoolean("Ctrl")&&data.getString("Hand").equals(hand.name())&&player.tickCount-data.getInt("Tick")>=0&&player.tickCount-data.getInt("Tick")<=20;}
 public static boolean takeControl(Player player,InteractionHand hand){return control(player,hand);}
 public static InteractionResultHolder<ItemStack> nativeUse(Object rawLevel,Object rawPlayer,Object rawHand){return use((Level)rawLevel,(Player)rawPlayer,(InteractionHand)rawHand);}
 public static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);if(fluid(stack).equals("stone"))return InteractionResultHolder.pass(stack);if(level.isClientSide){ClientVesselControl.send(hand);return InteractionResultHolder.success(stack);}boolean ctrl=control(player,hand);
  var hit=OriginalRaycast.itemPov(level,player,ClipContext.Fluid.SOURCE_ONLY);
  return useAt((ServerLevel)level,player,hand,hit,ctrl);
 }
 /** The production path computes hit+ctrl on the server; tests also use this to isolate exact world/material boundaries. */
 public static InteractionResultHolder<ItemStack> useAt(ServerLevel level,Player player,InteractionHand hand,BlockHitResult hit,boolean ctrl){
  var held=player.getItemInHand(hand);if(hit.getType()!=HitResult.Type.BLOCK||!handled(held))return InteractionResultHolder.pass(held);var pos=hit.getBlockPos();if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return InteractionResultHolder.fail(held);
  if(MinecraftForge.EVENT_BUS.post(new FillBucketEvent(player,held,level,hit)))return InteractionResultHolder.fail(held);
  String contents=fluid(held);var state=level.getBlockState(pos);var neighbor=pos.relative(hit.getDirection());
  if(contents.isEmpty()){
   var powder=BucketUse342.collectPowder(level,player,hand,pos);if(powder!=null)return powder;
   if(!waterOrLava(level.getFluidState(pos)))pos=neighbor;var found=level.getFluidState(pos);if(!waterOrLava(found))return InteractionResultHolder.fail(held);if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return InteractionResultHolder.fail(held);
   boolean lava=found.is(FluidTags.LAVA);if(LegacyProduction.removeCollectedSource(player.isCreative(),ctrl))clearFluid(level,pos);
   if(!player.isCreative()){var result=lava&&player.getRandom().nextFloat()<melting(held)?ItemStack.EMPTY:peer(held,lava?"lava":"water");if(result.isEmpty())player.displayClientMessage(Component.translatable("message.mite_reforged.bucket_melt"),true);player.awardStat(Stats.ITEM_USED.get(held.getItem()));replaceOne(player,hand,result);}
   level.playSound(null,pos,lava?SoundEvents.BUCKET_FILL_LAVA:SoundEvents.BUCKET_FILL,SoundSource.PLAYERS,1,1);level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP,pos);return InteractionResultHolder.success(player.getItemInHand(hand));
  }
  if(!contents.equals("water")&&!contents.equals("lava"))return InteractionResultHolder.pass(held);
  var liquid=contents.equals("lava")?Fluids.LAVA:Fluids.WATER;
  if(!player.isCreative()&&(level.getFluidState(pos).getType().isSame(liquid)||level.getFluidState(neighbor).getType().isSame(liquid))){replaceOne(player,hand,peer(held,""));return InteractionResultHolder.success(player.getItemInHand(hand));}
  boolean container=contents.equals("water")&&state.getBlock() instanceof LiquidBlockContainer c&&c.canPlaceLiquid(level,pos,state,liquid);
  var target=container||state.liquid()||state.canBeReplaced(liquid)?pos:neighbor;if(!level.mayInteract(player,target)||!player.mayUseItemAt(target,hit.getDirection(),held))return InteractionResultHolder.fail(held);
  boolean source=LegacyProduction.placeSource(true,player.isCreative(),ctrl,PlayerState.data(player).getInt("Xp"));
  if(!place(level,player,target,liquid,source))return InteractionResultHolder.fail(held);if(!player.isCreative())replaceOne(player,hand,peer(held,""));return InteractionResultHolder.success(player.getItemInHand(hand));
 }
 private static boolean waterOrLava(FluidState fluid){return fluid.is(FluidTags.WATER)||fluid.is(FluidTags.LAVA);}
 public static void replaceOne(Player player,InteractionHand hand,ItemStack result){SourceHeldConversion1037.convert(player,hand,result);}
 static void clearFluid(ServerLevel level,BlockPos pos){TemporaryVessels.get(level).cancel(pos);var state=level.getBlockState(pos);if(state.getBlock() instanceof BucketPickup pickup){pickup.pickupBlock(level,pos,state);if(level.getFluidState(pos).isEmpty())return;}if(state.liquid())level.removeBlock(pos,false);}
 private static void hiss(ServerLevel level,BlockPos pos){level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.5f,2.6f);level.sendParticles(ParticleTypes.LARGE_SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,8,.3,.3,.3,0);}
 private static boolean mixLava(ServerLevel level,BlockPos pos){var fluid=level.getFluidState(pos);if(!fluid.is(FluidTags.LAVA))return false;boolean obsidian=fluid.isSource()&&!TemporaryVessels.get(level).isTemporary(pos);TemporaryVessels.get(level).cancel(pos);level.setBlock(pos,(obsidian?Blocks.OBSIDIAN:Blocks.COBBLESTONE).defaultBlockState(),3);hiss(level,pos);return true;}
 public static boolean place(ServerLevel level,Player player,BlockPos pos,FlowingFluid fluid,boolean source){
  source=source&&LegacyProduction.placeSource(player!=null,player!=null&&player.isCreative(),true,player==null?0:PlayerState.data(player).getInt("Xp"));
  var state=level.getBlockState(pos);boolean water=fluid==Fluids.WATER;
  boolean contained=state.getBlock() instanceof LiquidBlockContainer container&&container.canPlaceLiquid(level,pos,state,fluid);
  if(!contained&&state.blocksMotion())return false;
  if(water&&state.getBlock() instanceof BaseFireBlock){level.removeBlock(pos,false);hiss(level,pos);return true;}
  if(water&&level.dimensionType().ultraWarm()){hiss(level,pos);return true;}
  if(water&&state.getFluidState().is(FluidTags.LAVA))return mixLava(level,pos);
  if(!water&&state.getFluidState().is(FluidTags.WATER)){TemporaryVessels.get(level).cancel(pos);level.setBlock(pos,Blocks.COBBLESTONE.defaultBlockState(),3);hiss(level,pos);return true;}
  if(contained){if(!((LiquidBlockContainer)state.getBlock()).placeLiquid(level,pos,state,fluid.getSource(false)))return false;}
  else {if(!state.isAir()&&!state.liquid())level.destroyBlock(pos,true);if(!level.setBlock(pos,fluid.getSource(false).createLegacyBlock(),3))return false;}
  TemporaryVessels.get(level).cancel(pos);if(source){if(player instanceof ServerPlayer server&&!player.isCreative())PlayerState.addXp(server,-100);}else TemporaryVessels.get(level).schedule(level,pos,!water,contained);
  level.playSound(null,pos,water?SoundEvents.BUCKET_EMPTY:SoundEvents.BUCKET_EMPTY_LAVA,SoundSource.PLAYERS,1,1);level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.FLUID_PLACE,pos);return true;
 }
 @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!player.isEyeInFluid(FluidTags.WATER))return;boolean converted=false;for(int i=0;i<player.getInventory().getContainerSize();i++){var item=player.getInventory().getItem(i);if(handled(item)&&fluid(item).equals("lava")){player.getInventory().setItem(i,peer(item,"stone").copyWithCount(item.getCount()));converted=true;}}if(converted)hiss(player.serverLevel(),player.blockPosition());}
 public static void itemTick(Object raw){BucketStorage337.dropped(raw);var entity=(ItemEntity)raw;OriginalBowls.dropped(entity);if(!(entity.level() instanceof ServerLevel level)||!handled(entity.getItem()))return;var item=entity.getItem();var pos=entity.blockPosition();var environment=level.getFluidState(pos);String contents=fluid(item),result=null;
  if(environment.is(FluidTags.WATER)&&!contents.equals("stone"))result=contents.equals("lava")?"stone":"water";
  else if(environment.is(FluidTags.LAVA)){if(contents.equals("water")){if(mixLava(level,pos))result="";}else if(!contents.equals("lava"))result="lava";}
  if(result==null||result.equals(contents))return;var converted=peer(item,result);int count=item.getCount();entity.setItem(converted.copyWithCount(1));for(int i=1;i<count;i++){var tag=entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.remove("UUID");tag.put("Item",converted.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()));var extra=new ItemEntity(level,entity.getX(),entity.getY(),entity.getZ(),converted.copyWithCount(1));extra.load(tag);level.addFreshEntity(extra);}if(result.equals("stone"))hiss(level,pos);
 }
 private ClassicVessels(){}
}
