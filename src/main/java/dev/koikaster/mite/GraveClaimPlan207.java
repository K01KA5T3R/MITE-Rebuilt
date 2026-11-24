package dev.koikaster.mite;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.*;

/** Uses the same native inventory wrapper as the live transfer, with a shadow
 * Inventory and without PlayerMainInvWrapper's premature menu broadcasts. */
final class GraveClaimPlan207 {
 final ListTag beforeInventory,afterInventory;
 final NonNullList<ItemStack> remaining;
 GraveClaimPlan207(ServerPlayer player,Grave.Entity grave){
  beforeInventory=player.getInventory().save(new ListTag());
  var shadow=new Inventory(player);shadow.load(beforeInventory.copy());
  var main=new RangedWrapper(new InvWrapper(shadow),0,shadow.items.size());
  remaining=NonNullList.withSize(grave.contents.size(),ItemStack.EMPTY);
  for(int i=0;i<grave.contents.size();i++){
   var restoring=grave.contents.get(i).copy();if(restoring.isEmpty())continue;
   var slot=Mob.getEquipmentSlotForItem(restoring);
   if(slot.getType()==EquipmentSlot.Type.ARMOR&&shadow.armor.get(slot.getIndex()).isEmpty())shadow.armor.set(slot.getIndex(),restoring.split(1));
   var rest=ItemHandlerHelper.insertItemStacked(main,restoring,false);
   var offhand=shadow.offhand.get(0);
   if(!rest.isEmpty()&&offhand.isEmpty()&&ModernOffhand.allowed(player,rest)){
    shadow.offhand.set(0,rest.split(Math.min(rest.getMaxStackSize(),shadow.getMaxStackSize())));offhand=shadow.offhand.get(0);
   }
   if(!rest.isEmpty()&&!offhand.isEmpty()&&ModernOffhand.allowed(player,rest)&&ItemStack.isSameItemSameTags(offhand,rest)){
    int moved=Math.min(rest.getCount(),Math.max(0,Math.min(shadow.getMaxStackSize(),offhand.getMaxStackSize())-offhand.getCount()));offhand.grow(moved);rest.shrink(moved);
   }
   remaining.set(i,rest);
  }
  afterInventory=shadow.save(new ListTag());
 }
 boolean changed(){return !beforeInventory.equals(afterInventory);}
 static void applyInventory(ServerPlayer player,ListTag saved){
  var shadow=new Inventory(player);shadow.load(saved.copy());var live=player.getInventory();
  for(int i=0;i<live.items.size();i++)live.items.set(i,shadow.items.get(i).copy());
  for(var slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.ARMOR){var item=shadow.armor.get(slot.getIndex());if(!ItemStack.matches(player.getItemBySlot(slot),item))player.setItemSlot(slot,item.copy());}
  var offhand=shadow.offhand.get(0);if(!ItemStack.matches(player.getOffhandItem(),offhand))player.setItemSlot(EquipmentSlot.OFFHAND,offhand.copy());
  live.setChanged();
 }
 static void sync(ServerPlayer player){player.inventoryMenu.broadcastChanges();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.broadcastChanges();}
}
