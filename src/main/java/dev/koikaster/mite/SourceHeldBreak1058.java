package dev.koikaster.mite;
import java.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.stats.Stats;

/** R195 ye's player destruction branch after the wear result is known. */
public final class SourceHeldBreak1058 {
 private record Frame(Player player,Item item,boolean wasHeld,boolean effect,boolean pigNibble){}
 private static final ThreadLocal<ItemStack> nibbling=new ThreadLocal<>();
 private static final Map<Player,Long> pickupUntil=Collections.synchronizedMap(new WeakHashMap<>());
 public static Object capture(Object rawStack,Object rawOwner){
  var stack=(ItemStack)rawStack;
  if(!(rawOwner instanceof Player player)||stack.isEmpty()||!ClassicToolCatalogue.sourceItem(stack)||player.getOffhandItem()==stack)return null;
  var armor=OriginalArmorItems.profile(stack);
  return new Frame(player,stack.getItem(),player.getMainHandItem()==stack,armor==null||!armor.material().equals("leather"),nibbling.get()==stack);
 }
 public static boolean finish(Object rawStack,Object rawOwner,Object rawFrame){
  if(!(rawFrame instanceof Frame f))return false;
  var stack=(ItemStack)rawStack;var player=f.player;
  if(rawOwner!=player||player.level().isClientSide)return false;
  player.awardStat(Stats.ITEM_BROKEN.get(f.item));
  if(f.wasHeld){
   var produced=f.pigNibble&&SourceCarrotSticks.handles(stack)?SourceCarrotSticks.returnedRod(stack):ItemStack.EMPTY;
   if(produced.isEmpty()&&f.effect)SourceToolBreakTransport1058.broken(player,f.item,player.getMainHandItem()==stack?player.getInventory().selected:-1);
   SourceHeldConversion1037.convert(player,InteractionHand.MAIN_HAND,produced);
   if(player.getMainHandItem().isEmpty())pickupUntil.put(player,System.currentTimeMillis()+1500L);
  }else{
   stack.shrink(1);
   if(stack.getCount()==0){
    if(f.effect)SourceToolBreakTransport1058.broken(player,f.item,player.getMainHandItem()==stack?player.getInventory().selected:-1);
    var inventory=player.getInventory();boolean found=false;
    for(int slot=0;slot<inventory.items.size();slot++)if(inventory.items.get(slot)==stack){inventory.setItem(slot,ItemStack.EMPTY);found=true;break;}
    if(!found)for(int slot=0;slot<inventory.armor.size();slot++)if(inventory.armor.get(slot)==stack){inventory.armor.set(slot,ItemStack.EMPTY);break;}
    inventory.setChanged();
   }
  }
  if(stack.getCount()<0)stack.setCount(0);
  stack.setDamageValue(0);
  return true;
 }
 public static void pigNibble(Player player,ItemStack stack,int amount){
  var outer=nibbling.get();nibbling.set(stack);
  try{stack.hurtAndBreak(amount,player,p->{});}finally{if(outer==null)nibbling.remove();else nibbling.set(outer);}
 }
 public static boolean admitted(boolean hasHeld,long now,long deadline){return hasHeld||now>=deadline;}
 public static boolean mayPickup(Object rawPlayer){
  var player=(Player)rawPlayer;if(player.level().isClientSide)return true;
  return admitted(!player.getMainHandItem().isEmpty(),System.currentTimeMillis(),pickupUntil.getOrDefault(player,0L));
 }
 private SourceHeldBreak1058(){}
}
