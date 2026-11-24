package dev.koikaster.mite;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;

/** Native equipment guard: food stays on the ground until the real source food owner consumes it. */
public final class OriginalWolfNativeFoodLoot {
 public static boolean preserve(boolean nativeResult,Object rawMob,Object rawStack){
  if(!nativeResult||!(rawMob instanceof Wolf wolf)||!(rawStack instanceof ItemStack stack))return nativeResult;
  if(wolf.getType()!=EntityType.WOLF&&wolf.getType()!=Mobs.WOLVES.get("dire_wolf").get())return nativeResult;
  // Reserve every source ItemMeat identity even when cooldown, love, sitting or health forbids actual eating.
  return !OriginalMeatItems.includes(stack);
 }
 private OriginalWolfNativeFoodLoot(){}
}
