package dev.koikaster.mite;
import java.util.*;import net.minecraft.nbt.*;import net.minecraft.world.SimpleContainer;import net.minecraft.world.item.ItemStack;

/** Respect per-item limits in empty-slot insertion. Preserve old oversized NBT
 * contents as ordinary saved item stacks until visible space becomes available. */
public final class SourceSimpleContainer475 {
 private static final Map<SimpleContainer,List<ItemStack>> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
 private static final ThreadLocal<Set<SimpleContainer>> BUSY=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<>()));
 public static boolean emptySlots(SimpleContainer inventory,ItemStack incoming){
  if(!ClassicToolCatalogue.sourceItem(incoming))return false;int limit=Math.min(inventory.getMaxStackSize(),incoming.getMaxStackSize());if(limit<=0)return true;
  for(int i=0;i<inventory.getContainerSize()&&!incoming.isEmpty();i++)if(inventory.getItem(i).isEmpty())inventory.setItem(i,incoming.split(limit));return true;
 }
 private static List<ItemStack> parts(SimpleContainer inventory,ItemStack original){var result=new ArrayList<ItemStack>();var stack=original.copy();int limit=Math.max(1,Math.min(inventory.getMaxStackSize(),stack.getMaxStackSize()));while(!stack.isEmpty())result.add(stack.split(limit));return result;}
 private static void place(SimpleContainer inventory,List<ItemStack> input,List<ItemStack> remaining){for(var source:input)for(var part:parts(inventory,source)){var rest=inventory.addItem(part);if(!rest.isEmpty())remaining.add(rest.copy());}}
 public static boolean read(SimpleContainer inventory,ListTag tag){
  var input=new ArrayList<ItemStack>();boolean selected=false;for(int i=0;i<tag.size();i++){var stack=ItemStack.of(tag.getCompound(i));if(!stack.isEmpty()){input.add(stack);selected|=ClassicToolCatalogue.sourceItem(stack);}}if(!selected)return false;
  var busy=BUSY.get();if(!busy.add(inventory))throw new IllegalStateException("Reentrant simple-container load");try{inventory.clearContent();var remaining=new ArrayList<ItemStack>();place(inventory,input,remaining);if(!remaining.isEmpty())PENDING.put(inventory,remaining);}finally{busy.remove(inventory);}drain(inventory);return true;
 }
 public static void drain(SimpleContainer inventory){
  var busy=BUSY.get();if(busy.contains(inventory))return;var previous=PENDING.remove(inventory);if(previous==null||previous.isEmpty())return;busy.add(inventory);try{var remaining=new ArrayList<ItemStack>();place(inventory,previous,remaining);if(!remaining.isEmpty())PENDING.put(inventory,remaining);}finally{busy.remove(inventory);}
 }
 public static void clear(SimpleContainer inventory){PENDING.remove(inventory);}
 public static ListTag append(ListTag tag,SimpleContainer inventory){var pending=PENDING.get(inventory);if(pending!=null)for(var stack:pending)tag.add(stack.save(new CompoundTag()));return tag;}
 public static List<ItemStack> takeAll(SimpleContainer inventory){
  var pending=PENDING.remove(inventory);if(pending==null||pending.isEmpty())return null;var result=new ArrayList<ItemStack>();for(int i=0;i<inventory.getContainerSize();i++)if(!inventory.getItem(i).isEmpty())result.addAll(parts(inventory,inventory.getItem(i)));for(var stack:pending)result.addAll(parts(inventory,stack));inventory.clearContent();return result;
 }
 public static boolean empty(boolean original,SimpleContainer inventory){if(!original||!PENDING.containsKey(inventory))return original;drain(inventory);if(PENDING.containsKey(inventory))return false;for(int i=0;i<inventory.getContainerSize();i++)if(!inventory.getItem(i).isEmpty())return false;return true;}
 private SourceSimpleContainer475(){}
}
