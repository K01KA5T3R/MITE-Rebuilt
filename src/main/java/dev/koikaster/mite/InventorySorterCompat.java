package dev.koikaster.mite;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Keep utility sorting out of timed recipes and dedicated machine inputs. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class InventorySorterCompat {
 public static boolean allowSlot(Slot slot) {
  return slot!=null&&!(slot.container instanceof CraftingContainer)&&!(slot.container instanceof ResultContainer);
 }
 @SubscribeEvent public static void enqueue(InterModEnqueueEvent event) {
  for(String name:new String[]{"dev.koikaster.mite.AdvancedEnchantMenu$2","dev.koikaster.mite.SourceEnchantMenu$2","dev.koikaster.mite.SourceFurnaceMenu$1","dev.koikaster.mite.SourceFurnaceMenu$2"})
   InterModComms.sendTo("inventorysorter","slotblacklist",()->name);
 }
 private InventorySorterCompat(){}
}
