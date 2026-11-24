package dev.koikaster.mite;
import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
/** Keep each native insertion and result transaction; repeat the native empty-slot
 * step when an entire recipe batch spans several source-sized inventory stacks. */
public final class SourceCraftBatch1722 {
 public interface Access {boolean mite$moveOneCraft1722(ItemStack stack,int start,int end,boolean reverse);}
 public static boolean move(Object raw,ItemStack stack,int start,int end,boolean reverse){
  var menu=(AbstractContainerMenu)raw;var access=(Access)raw;if(stack.isEmpty()||start<0||end>menu.slots.size()||start>=end)return false;
  int count=stack.getCount(),left=count;var before=new ArrayList<ItemStack>(end-start);
  for(int i=start;i<end;i++){
   var slot=menu.getSlot(i);var stored=slot.getItem();before.add(stored.copy());
   if(!slot.mayPlace(stack))continue;
   int limit=Math.min(slot.getMaxStackSize(stack),stack.getMaxStackSize());
   if(stored.isEmpty())left-=limit;
   else if(ItemStack.isSameItemSameTags(stored,stack))left-=Math.max(0,Math.min(limit,stored.getMaxStackSize())-stored.getCount());
  }
  if(left>0)return false;
  // These ranges contain only the player's main inventory and hotbar. The
  // native method changes counts and slot contents, without consuming inputs.
  while(!stack.isEmpty()){
   int previous=stack.getCount();
   if(!access.mite$moveOneCraft1722(stack,start,end,reverse)||stack.getCount()>=previous){
    for(int i=start;i<end;i++)menu.getSlot(i).setByPlayer(before.get(i-start));
    stack.setCount(count);return false;
   }
  }
  return true;
 }
 public static boolean cartographyShiftBlocked(AbstractContainerMenu menu,Player player,int index){
  if(index!=2)return false;
  var result=menu.getSlot(index).getItem();
  return result.getCount()>1&&!ContinuousCrafting.canStore(player,result);
 }
 private SourceCraftBatch1722(){}
}
