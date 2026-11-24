package dev.koikaster.mite;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;

/** R195 block families also govern their retained modern variants. Explicit
 * registry aliases are resolved first, so this only fills catalogue gaps. */
final class SourceBlockFamilyStacks {
 static Integer cap(ItemStack stack){
  if(!(stack.getItem() instanceof BlockItem item))return null;
  var block=item.getBlock();
  if(block instanceof DoorBlock)return 1;
  if(block instanceof SaplingBlock)return 16;
  if(block instanceof LeavesBlock)return 4;
  if(block instanceof FenceBlock)return 8;
  if(block instanceof FenceGateBlock||block instanceof TrapDoorBlock||block instanceof ButtonBlock)return 4;
  if(block instanceof BasePressurePlateBlock)return 8;
  return null;
 }
 private SourceBlockFamilyStacks(){}
}
