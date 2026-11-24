package dev.koikaster.mite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** EntityLongdeadGuardian: one stowed stack, ten-tick checks and5/6 block
 * hysteresis. Its own source NBT key persists the real item, damage and enchants. */
public final class SourceLongdeadGuardian extends SourceLongdead {
 private ItemStack stowed=ItemStack.EMPTY;
 public SourceLongdeadGuardian(EntityType<? extends SourceLongdeadGuardian> type,Level level){super(type,level);}
 @Override protected void addRandomWeapon(){super.addRandomWeapon();if(getMainHandItem().getItem() instanceof BowItem)stowed=randomEquipment(Content.get("ancient_metal_dagger"));}
 public ItemStack stowedItem(){return stowed;}
 public void swapStowed(){var old=stowed;stowed=getMainHandItem();setItemSlot(EquipmentSlot.MAINHAND,old);}
 public boolean canStow(ItemStack item){var tool=ClassicToolWear.profile(item);return skeletonType()==0&&(item.getItem() instanceof BowItem||tool!=null&&tool.kind()==LegacyToolWear.Kind.DAGGER);}
 @Override public boolean canHoldItem(ItemStack item){return skeletonType()!=0||canStow(item);}
 @Override public boolean wantsToPickUp(ItemStack item){return (skeletonType()!=0||canStow(item))&&super.wantsToPickUp(item);}
 @Override public void aiStep(){
  super.aiStep();if(level().isClientSide||stowed.isEmpty()||!getMainHandItem().isEmpty()&&(tickCount+getId()*47)%10!=0)return;
  if(getMainHandItem().isEmpty()){swapStowed();return;}
  var target=getTarget();if(target==null||!OriginalVision.from(this,target,getEyePosition(),Double.MAX_VALUE,true))return;
  double distance=distanceTo(target);if(getMainHandItem().getItem() instanceof BowItem?distance<5:distance>6)swapStowed();
 }
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(!stowed.isEmpty())tag.put("stowed_item_stack",stowed.save(new CompoundTag()));}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);stowed=tag.contains("stowed_item_stack",10)?ItemStack.of(tag.getCompound("stowed_item_stack")):ItemStack.EMPTY;}
}
