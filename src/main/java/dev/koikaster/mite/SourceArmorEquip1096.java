package dev.koikaster.mite;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Only ArmorItem.use's audited equipment call is replaced. No global Equipable hook. */
public final class SourceArmorEquip1096 {
 public static InteractionResultHolder<ItemStack> swap(Object rawEquipable,Object rawItem,Object rawLevel,Object rawPlayer,Object rawHand){
  var equipable=(Equipable)rawEquipable;var item=(Item)rawItem;var level=(Level)rawLevel;
  var player=(Player)rawPlayer;var hand=(InteractionHand)rawHand;var input=player.getItemInHand(hand);
  var id=BuiltInRegistries.ITEM.getKey(item);
  if(hand!=InteractionHand.MAIN_HAND||!(item instanceof ArmorItem)||input.isEmpty()||input.getItem()!=item||
     id==null||BuiltInRegistries.ITEM.get(id)!=item||!(id.getNamespace().equals("minecraft")||id.getNamespace().equals(MiteReforged.ID)))
   return equipable.swapWithEquipmentSlot(item,level,player,hand);
  boolean used=SourceArmorEquipRules1096.use(new Adapter(player,level,((ArmorItem)item).getEquipmentSlot()));
  var held=player.getItemInHand(hand);
  return used?InteractionResultHolder.sidedSuccess(held,level.isClientSide):InteractionResultHolder.fail(held);
 }
 private static final class Adapter implements SourceArmorEquipRules1096.Host<ItemStack> {
  final Player player;final Level level;final EquipmentSlot slot;
  Adapter(Player player,Level level,EquipmentSlot slot){this.player=player;this.level=level;this.slot=slot;}
  // classic_curses.js still runs its original mayWear/armorDenied prefix exactly once.
  public boolean mayWear(){return true;}
  public ItemStack held(){return player.getMainHandItem();}
  public int equipmentPosition(ItemStack stack){return slot.getIndex()+1;}
  public ItemStack equipped(int index){var stack=player.getItemBySlot(slot);return stack.isEmpty()?null:stack;}
  public boolean server(){return !level.isClientSide;}
  public ItemStack copy(ItemStack stack){return stack.copy();}
  public void equip(int index,ItemStack stack){player.setItemSlot(slot,stack);}
  public void convertHeld(){SourceHeldConversion1037.convert(player,InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
  // Native useItem has no R195 post-right-click statistic. Bypassing swap's award is the suppression.
  public void suppressStat(){}
 }
 private SourceArmorEquip1096(){}
}
