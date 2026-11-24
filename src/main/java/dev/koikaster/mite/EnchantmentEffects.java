package dev.koikaster.mite;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** aaw/uc/ni/yb/ItemMattock/ItemScythe/jw and original action fatigue. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class EnchantmentEffects {
 private static final UUID SPEED=UUID.fromString("d6726c88-73ca-491c-b330-99abbf156283"),POTIONS=UUID.fromString("86800428-de9a-4bea-9619-171e84bf7fba");
 public static Enchantment enchantment(String name){return ForgeRegistries.ENCHANTMENTS.getValue(Content.id(name));}
 public static int level(String name,ItemStack stack){var e=enchantment(name);return e==null?0:EnchantmentHelper.getItemEnchantmentLevel(e,stack);}
 public static float fraction(String name,ItemStack stack){var e=enchantment(name);return e==null?0:EnchantmentHelper.getItemEnchantmentLevel(e,stack)/(float)e.getMaxLevel();}
 public static int wornLevel(String name,LivingEntity actor){int result=0;for(var stack:actor.getArmorSlots())result=Math.max(result,level(name,stack));return result;}
 public static float endurance(LivingEntity actor){return 1-wornLevel("endurance",actor)*.2f;}
 public static float paralysisResistance(LivingEntity actor){return wornLevel("free_action",actor)*(.8f/4);}
 private static void modifier(AttributeInstance attribute,UUID id,String name,double amount){var old=attribute.getModifier(id);if(amount==0){if(old!=null)attribute.removeModifier(id);return;}if(old!=null&&Math.abs(old.getAmount()-amount)<1e-9)return;if(old!=null)attribute.removeModifier(id);attribute.addTransientModifier(new AttributeModifier(id,name,amount,AttributeModifier.Operation.MULTIPLY_TOTAL));}
 public static void refreshMovement(Object rawActor){var actor=(LivingEntity)rawActor;if(actor.level().isClientSide||!ClassicDefense.scoped(actor))return;var attribute=actor.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute==null)return;int speed=actor.hasEffect(MobEffects.MOVEMENT_SPEED)?actor.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()+1:0,slow=actor.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)?actor.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()+1:0;var speedModifier=MobEffects.MOVEMENT_SPEED.getAttributeModifiers().get(Attributes.MOVEMENT_SPEED);var slowModifier=MobEffects.MOVEMENT_SLOWDOWN.getAttributeModifiers().get(Attributes.MOVEMENT_SPEED);if(speedModifier!=null)attribute.removeModifier(speedModifier.getId());if(slowModifier!=null)attribute.removeModifier(slowModifier.getId());float resistance=paralysisResistance(actor),factor;
  if(actor instanceof Player player){boolean web=SourcePlayerWebContact.active(player);float combined=(speed-slow)*.2f-(web?.75f:0);if(combined<0)combined*=1-resistance;factor=net.minecraft.util.Mth.clamp(1+combined,0,4)*(SurvivalFood.hasFoodEnergy(player)?1:.75f);}else if(actor instanceof SourceZombieOwner||actor instanceof SourceVillager){var data=actor.getPersistentData();boolean web=data.contains("MiteSourceWebTick")&&actor.tickCount-data.getInt("MiteSourceWebTick")<=1;float combined=(speed-slow)*.2f-(web?.75f:0);if(combined<0)combined*=1-resistance;factor=net.minecraft.util.Mth.clamp(1+combined,0,4);}else factor=(1+speed*.2f)*(1-slow*.2f*(1-resistance));
  modifier(attribute,POTIONS,"R195 potion movement",factor-1);modifier(attribute,SPEED,"R195 speed boots",actor instanceof Player?wornLevel("speed",actor)*.05f:0);
 }
 @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent event){var actor=event.getEntity();if(actor.level().isClientSide)return;refreshMovement(actor);if(actor instanceof ServerPlayer player&&!player.isCreative()&&!player.isSpectator()&&player.isUsingItem()&&player.getUseItem().getItem() instanceof BowItem)player.causeFoodExhaustion(.01f*endurance(player));}
 private record UseContext(Player player,ItemStack stack){}
 private static final ThreadLocal<Deque<UseContext>> USE=ThreadLocal.withInitial(ArrayDeque::new);
 public static void beforeUse(Object rawContext){var context=(UseOnContext)rawContext;USE.get().push(new UseContext(context.getPlayer(),context.getItemInHand().copy()));}
 public static InteractionResult afterUse(Object rawResult){var queue=USE.get();if(!queue.isEmpty())queue.pop();if(queue.isEmpty())USE.remove();return (InteractionResult)rawResult;}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void placed(BlockEvent.EntityPlaceEvent event){if(event.isCanceled()||!(event.getEntity() instanceof ServerPlayer player)||player.isCreative()||player.isSpectator())return;var use=USE.get().peek();boolean placing=use!=null&&use.player()==player?use.stack().getItem() instanceof BlockItem:player.getMainHandItem().getItem() instanceof BlockItem||player.getOffhandItem().getItem() instanceof BlockItem;if(!placing)return;float hardness=ClassicMining.hardness(event.getPlacedBlock(),event.getLevel(),event.getPos());if(hardness>0){float amount=Math.min(hardness,20)*endurance(player);if(!SourcePlacement1075.deferHunger(player,amount))player.causeFoodExhaustion(amount);}}
 private record TillContext(BlockState before,ItemStack tool){}
 public static boolean denyTilling(Object rawContext){var context=(UseOnContext)rawContext;var player=context.getPlayer();return player!=null&&!player.isCreative()&&!SurvivalFood.hasFoodEnergy(player);}
 public static Object beforeTill(Object rawContext){var context=(UseOnContext)rawContext;return new TillContext(context.getLevel().getBlockState(context.getClickedPos()),context.getItemInHand().copy());}
 public static InteractionResult afterTill(Object rawResult,Object rawBefore,Object rawContext){var result=(InteractionResult)rawResult;var before=(TillContext)rawBefore;var context=(UseOnContext)rawContext;var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos();if(level.isClientSide||player==null||!result.consumesAction()||before.before().is(Blocks.FARMLAND)||!level.getBlockState(pos).is(Blocks.FARMLAND))return result;if(!player.isCreative())player.causeFoodExhaustion(ClassicMining.hardness(before.before(),level,pos)/2f*endurance(player));fertilize(level,pos,before.tool());return result;}
 private static void fertilize(Level level,BlockPos pos,ItemStack tool){float chance=fraction("fertility",tool);if(chance>0&&level.random.nextFloat()<chance){var soil=level.getBlockState(pos);if(soil.is(Blocks.FARMLAND))level.setBlock(pos,soil.setValue(Agriculture.FERTILIZED,true),3);}}
 private static final class DestroyContext {
  final ServerPlayer player;
  final ServerLevel level;
  final BlockPos pos;
  final BlockState before;
  final ItemStack tool;
  ItemStack usedBonePickaxe;

  DestroyContext(ServerPlayer player,BlockPos pos) {
   this.player=player;
   this.level=player.serverLevel();
   this.pos=pos.immutable();
   this.before=level.getBlockState(this.pos);
   this.tool=player.getMainHandItem().copy();
  }
 }
 private static final ThreadLocal<Deque<DestroyContext>> DESTROY=ThreadLocal.withInitial(ArrayDeque::new);
 private static final ThreadLocal<Boolean> FELLING=ThreadLocal.withInitial(()->false);
 public static void beforeDestroy(Object rawPlayer,Object rawPos) {
  DESTROY.get().push(new DestroyContext((ServerPlayer)rawPlayer,(BlockPos)rawPos));
 }

 public static int checkBoneSweepAfterEvent(int experience) {
  if (experience == -1) return experience;
  var context = DESTROY.get().peek();
  if (context != null && !LargeBonePickaxe.allowSweepTool(context.player, context.level, context.pos,
          context.player.getMainHandItem())) return -1;
  return experience;
 }

 public static boolean mayUseBoneSweepTool(Object rawStack,Object rawLevel,Object rawPos,Object rawPlayer) {
  return LargeBonePickaxe.allowSweepTool((ServerPlayer)rawPlayer,(Level)rawLevel,(BlockPos)rawPos,(ItemStack)rawStack);
 }

 static void noteBonePickaxeUse(ServerPlayer player,Level level,BlockPos pos,ItemStack tool) {
  var context = DESTROY.get().peek();
  if (context != null && context.player == player && context.level == level && context.pos.equals(pos)) {
   context.usedBonePickaxe = tool;
  }
 }

 public static boolean afterDestroy(boolean removed) {
  var queue=DESTROY.get();
  if(queue.isEmpty())return removed;
  var context=queue.pop();
  if(queue.isEmpty())DESTROY.remove();
  var player=context.player;
  DirectionalMining1765.completed(player,context.pos,context.before,removed);
  LargeBonePickaxe.afterCentralBreak(player,context.level,context.pos,context.before,context.usedBonePickaxe,
      removed&&!context.level.getBlockState(context.pos).is(context.before.getBlock()));
  if(!removed||player.isCreative()||player.level().getBlockState(context.pos).is(context.before.getBlock()))return removed;
  var state=context.before;
  var profile=ClassicToolWear.profile(context.tool);
  if(profile!=null&&Agriculture.supports(state)&&((CropBlock)state.getBlock()).isMaxAge(state)){boolean scythe=profile.kind()==LegacyToolWear.Kind.SCYTHE,hoe=profile.kind()==LegacyToolWear.Kind.HOE,mattock=profile.kind()==LegacyToolWear.Kind.MATTOCK;if((hoe||mattock)&&!state.is(Blocks.WHEAT)||scythe&&state.is(Blocks.WHEAT))fertilize(player.level(),context.pos.below(),context.tool);}
  if(state.is(BlockTags.LOGS)&&!FELLING.get()){int count=level("tree_felling",player.getMainHandItem());if(count>0){FELLING.set(true);try{for(int dy=1;dy<=count&&player.level().getBlockState(context.pos.above(dy)).is(BlockTags.LOGS);dy++)player.gameMode.destroyBlock(context.pos.above(dy));}finally{FELLING.remove();}}}return removed;
 }
 public static void web(Object rawActor,Object rawState){if(rawActor instanceof Player player&&!player.level().isClientSide&&((BlockState)rawState).is(Blocks.COBWEB)){SourcePlayerWebContact.mark(player);refreshMovement(player);}}
 public static void miningTick(Object rawPlayer){var player=(Player)rawPlayer;if(!player.level().isClientSide&&!player.isCreative()&&!player.isSpectator())player.causeFoodExhaustion(.01f*endurance(player));}
 public static float initialMining(Object rawState,Object rawPlayer,Object rawGetter,Object rawPos,Object rawAction){var state=(BlockState)rawState;var player=(Player)rawPlayer;float progress=state.getDestroyProgress(player,(net.minecraft.world.level.BlockGetter)rawGetter,(BlockPos)rawPos);if(rawAction==net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK&&!state.isAir()&&progress>=1){miningTick(player);DirectionalMining1765.extraTick(player,state,rawPos);}return progress;}
 public static void nativeMiningFatigue(Object actor,float ignored){}
 private EnchantmentEffects(){}
}
