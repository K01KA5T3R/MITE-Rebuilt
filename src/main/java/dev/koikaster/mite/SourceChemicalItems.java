package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

/** ss/ud chemical item damage, separate from an attack's ordinary weapon wear.
 * Native ItemEntity public item-NBT reader keeps its real health authoritative;
 * no second health counter is kept in persistent data. */
final class SourceChemicalItems {
 record Result(boolean affected,boolean destroyed){}
 static int scaled(ItemStack stack,float amount,boolean dropped){float factor=OriginalArmorItems.profile(stack)!=null?2:ClassicToolWear.profile(stack)!=null?100:1;return dropped?Math.max(1,Math.round(amount*factor)):(int)(amount*factor);}
 static boolean susceptible(ItemStack stack,boolean acid){return SourceChemicalMaterials.item(stack,acid);}
 static boolean inventoryStack(Player player,ItemStack stack,boolean acid,float amount){
  if(player.isCreative()||stack.isEmpty()||!susceptible(stack,acid))return false;
  if(stack.isDamageableItem()){
   int wear=scaled(stack,amount,false);if(wear<=0)return false;
   int before=stack.getDamageValue();boolean broken=stack.hurt(wear,player.getRandom(),player instanceof net.minecraft.server.level.ServerPlayer server?server:null);
   if(broken){stack.shrink(1);stack.setDamageValue(0);for(var slot:EquipmentSlot.values())if(player.getItemBySlot(slot)==stack){player.broadcastBreakEvent(slot);break;}}
   return broken||stack.getDamageValue()>before;
  }
  if(player.getRandom().nextFloat()*10>=amount)return false;
  ItemStack container=container(stack,acid);stack.shrink(1);
  if(!container.isEmpty()&&!player.getInventory().add(container))player.drop(container,false);
  return true;
 }
 static boolean inventory(Player player,boolean acid,float chance,float amount,boolean worn){
  boolean affected=false;
  for(var stack:player.getInventory().items)for(int count=0;count<stack.getCount();count++)if(player.getRandom().nextFloat()<chance&&inventoryStack(player,stack,acid,amount))affected=true;
  for(var stack:player.getInventory().offhand)for(int count=0;count<stack.getCount();count++)if(player.getRandom().nextFloat()<chance&&inventoryStack(player,stack,acid,amount))affected=true;
  if(worn)for(var stack:player.getInventory().armor)for(int count=0;count<stack.getCount();count++)if(player.getRandom().nextFloat()<chance*5&&inventoryStack(player,stack,acid,amount))affected=true;
  return affected;
 }
 private static ItemStack container(ItemStack stack,boolean acid){
  if(ClassicVessels.handled(stack)&&!ClassicVessels.fluid(stack).isEmpty()){var peer=ClassicVessels.peer(stack,"");return susceptible(peer,acid)?ItemStack.EMPTY:peer;}
  var peer=stack.getCraftingRemainingItem();return !peer.isEmpty()&&!susceptible(peer,acid)?peer:ItemStack.EMPTY;
 }
 static Result dropped(SourceGelatinousCube mob,ItemEntity entity,boolean acid){
  var stack=entity.getItem();if(entity.isRemoved()||!susceptible(stack,acid))return new Result(false,false);
  var data=new CompoundTag();entity.addAdditionalSaveData(data);int health=data.getShort("Health");
  boolean affected=true;
  if(stack.isDamageableItem()){
   int before=stack.getDamageValue();
   boolean destroyed=stack.hurt(scaled(stack,1,true),mob.getRandom(),null);
   affected=destroyed||stack.getDamageValue()>before;
   health=destroyed?0:Math.max(1,5*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage());
  }else health-=1;
  // Original ss sends its FX while the entity is still tracked, before removal.
  if(affected)SourceChemicalFx.entity(entity,health<=0?(acid?SourceChemicalFx.ENTITY_SMOKE_STEAM:SourceChemicalFx.ENTITY_STEAM):SourceChemicalFx.ENTITY_VANISH);
  if(health<=0){var peer=container(stack,acid);if(peer.isEmpty()){entity.discard();return new Result(affected,true);}convert(entity,peer.copyWithCount(stack.getCount()));data=new CompoundTag();entity.addAdditionalSaveData(data);health=5;}
  data.putShort("Health",(short)health);data.put("Item",entity.getItem().save(new CompoundTag()));entity.readAdditionalSaveData(data);
  return new Result(affected,false);
 }
 static void vessel(ItemEntity entity,String contents){convert(entity,ClassicVessels.peer(entity.getItem(),contents).copyWithCount(entity.getItem().getCount()));}
 private static void convert(ItemEntity entity,ItemStack result){
  int total=result.getCount(),max=result.getMaxStackSize();entity.setItem(result.copyWithCount(Math.min(total,max)));var data=new CompoundTag();entity.addAdditionalSaveData(data);data.putShort("Health",(short)5);entity.readAdditionalSaveData(data);
  for(int left=total-max;left>0;left-=max){var stack=result.copyWithCount(Math.min(left,max));var extra=new ItemEntity(entity.level(),entity.getX(),entity.getY(),entity.getZ(),stack);var saved=entity.saveWithoutId(new CompoundTag());saved.remove("UUID");saved.put("Item",stack.save(new CompoundTag()));extra.load(saved);extra.setDeltaMovement(entity.getDeltaMovement());entity.level().addFreshEntity(extra);}
 }
 private SourceChemicalItems(){}
}
