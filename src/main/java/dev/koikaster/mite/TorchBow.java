package dev.koikaster.mite;
import java.util.function.Predicate;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
/** The reserved torch is the payload;Infinity never supplies free ammunition. */
public final class TorchBow extends BowItem {
 public TorchBow(Properties properties){super(properties);}
 static int ammunition(Player player){for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.TORCH))return i;return-1;}
 @Override public Predicate<ItemStack> getAllSupportedProjectiles(){return stack->stack.is(Items.TORCH);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var bow=player.getItemInHand(hand);if(!WorldContentMode.rebuilt(level))return InteractionResultHolder.fail(bow);boolean loaded=ammunition(player)>=0;var event=ForgeEventFactory.onArrowNock(bow,level,player,hand,loaded);if(event!=null)return event;if(!loaded)return InteractionResultHolder.fail(bow);player.startUsingItem(hand);return InteractionResultHolder.consume(bow);}
 @Override public void releaseUsing(ItemStack bow,Level level,LivingEntity actor,int remaining){
  if(level.isClientSide||!WorldContentMode.rebuilt(level)||!(actor instanceof Player player))return;
  int drawn=ForgeEventFactory.onArrowLoose(bow,level,player,getUseDuration(bow)-remaining,ammunition(player)>=0);if(drawn<0)return;
  int slot=ammunition(player);float power=ClassicArchery.power(drawn,bow);if(slot<0||power<.1f)return;
  var payload=player.getInventory().getItem(slot).split(1);player.getInventory().setChanged();
  var shot=new TorchBowShot(level,player,payload);int trueFlight=Math.round(EnchantmentEffects.fraction("true_flight",bow)*40);float spread=SourceBowRules815.wander(PlayerState.level(player),false,true,trueFlight,ClassicArchery.suspended(player));
  shot.shootFromRotation(player,player.getXRot(),player.getYRot(),0,power*3,spread);
  if(!level.addFreshEntity(shot)){var refund=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()),payload.copy(),false);if(!refund.isEmpty())player.drop(refund,false);return;}
  bow.hurtAndBreak(1,player,owner->owner.broadcastBreakEvent(player.getUsedItemHand()));
  level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1/(level.random.nextFloat()*.4f+1.2f)+power*.5f);player.awardStat(Stats.ITEM_USED.get(this));
 }
}
