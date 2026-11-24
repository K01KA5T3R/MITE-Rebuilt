package dev.koikaster.mite;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Transport the actual source heat field alongside the native furnace clock. */
final class SourceFurnaceHeat1533 extends DataSlot {
 private final Container furnace;private int remote;
 SourceFurnaceHeat1533(Container furnace){this.furnace=furnace;}
 @Override public int get(){
  if(furnace instanceof ThermalFurnace.Entity entity)return entity.heatLevel();
  if(furnace instanceof AbstractFurnaceBlockEntity entity)return ModernCooking.state(entity).getInt("Heat");
  return remote;
 }
 @Override public void set(int value){remote=value;}
}
