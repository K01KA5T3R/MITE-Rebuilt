package dev.koikaster.mite;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Reserve source repair materials before Mob.aiStep's native equipment pickup.
 * The source repair owner alone decides injury, cooldown and actual consumption.
 */
public final class OriginalRepairNativeLoot {
 public static boolean preserve(boolean nativeResult,Object rawMob,Object rawStack){
  if(!nativeResult||!(rawMob instanceof Mob mob)||!OriginalRepairPickup.handles(mob)
   ||!(rawStack instanceof ItemStack stack))return nativeResult;
  return !stack.is(mob.getType()==EntityType.SNOW_GOLEM?Items.SNOWBALL:Items.BONE);
 }
 private OriginalRepairNativeLoot(){}
}
