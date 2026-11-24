package dev.koikaster.mite;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;
/** Presentation only: synchronized aggression and item-use drive the native
 * SkeletonModel arm pose and bow pull model. No release callback is invoked,
 * so the sealed source firing cadence/damage/projectile owner remains in charge. */
public final class SourceSkeletonBowPose {
 public static boolean renderBow(Object rawStack,Object rawItem,Object actor){
  var stack=(net.minecraft.world.item.ItemStack)rawStack;var item=(net.minecraft.world.item.Item)rawItem;
  if((actor instanceof SourceSkeleton||actor instanceof SourceWitherSkeleton)&&item==net.minecraft.world.item.Items.BOW&&stack.getItem() instanceof BowItem)return true;
  return stack.is(item);
 }
 static void hold(SourceSkeleton actor){
  if(!(actor.getMainHandItem().getItem() instanceof BowItem)){stop(actor);return;}
  actor.setAggressive(true);if(!actor.isUsingItem())actor.startUsingItem(InteractionHand.MAIN_HAND);
 }
 static void stop(SourceSkeleton actor){actor.stopUsingItem();actor.setAggressive(false);}
 static void shot(SourceSkeleton actor){actor.stopUsingItem();}
 private SourceSkeletonBowPose(){}
}
