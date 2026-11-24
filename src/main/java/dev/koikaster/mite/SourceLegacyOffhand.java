package dev.koikaster.mite;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

/** Modern save migration only. R195 has five equipment slots, so retain the
 * unsupported sixth stack separately rather than discarding its full NBT. */
public final class SourceLegacyOffhand {
 private static final String KEY="R195LegacyOffhand";
 private final Mob actor;
 private final List<ItemStack> stored=new ArrayList<>();
 public SourceLegacyOffhand(Mob actor){this.actor=actor;}
 private void capture(){var stack=actor.getOffhandItem();if(!stack.isEmpty()){stored.add(stack.copy());actor.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);}}
 public void load(CompoundTag tag){
  stored.clear();var list=tag.getList(KEY,Tag.TAG_COMPOUND);
  for(int i=0;i<list.size();i++){var stack=ItemStack.of(list.getCompound(i));if(!stack.isEmpty())stored.add(stack);}
  capture();
 }
 public void save(CompoundTag tag){
  capture();var list=new ListTag();for(var stack:stored)list.add(stack.save(new CompoundTag()));
  if(list.isEmpty())tag.remove(KEY);else tag.put(KEY,list);
  // The native parent may have serialized the hand just before capture.
  if(tag.contains("HandItems",Tag.TAG_LIST)){var hands=tag.getList("HandItems",Tag.TAG_COMPOUND);if(hands.size()>1)hands.set(1,new CompoundTag());}
 }
 public boolean hasItems(){return !stored.isEmpty()||!actor.getOffhandItem().isEmpty();}
 public int size(){return stored.size();}
 public void drop(){
  if(actor.level().isClientSide)return;capture();
  for(var it=stored.iterator();it.hasNext();){var stack=it.next().copy();MineralSupply.stock(stack);if(actor.spawnAtLocation(stack)!=null)it.remove();}
 }
}
