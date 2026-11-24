package dev.koikaster.mite;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Whole-batch capacity check. Automatic crafting never borrows the cursor or
 * consumes ingredients when the finished batch cannot fit in the main inventory.
 */
final class ContinuousCrafting {
 static boolean canStore(Player player,ItemStack result){
  if(result.isEmpty())return false;
  int left=result.getCount();
  int limit=Math.min(player.getInventory().getMaxStackSize(),result.getMaxStackSize());
  for(var stored:player.getInventory().items){
   if(stored.isEmpty())left-=limit;
   else if(ItemStack.isSameItemSameTags(stored,result))left-=Math.max(0,Math.min(limit,stored.getMaxStackSize())-stored.getCount());
   if(left<=0)return true;
  }
  return false;
 }
 private ContinuousCrafting(){}
}
