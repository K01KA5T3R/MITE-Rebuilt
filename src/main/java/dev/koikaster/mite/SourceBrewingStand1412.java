package dev.koikaster.mite;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.world.*;import net.minecraft.world.entity.player.*;import net.minecraft.world.inventory.*;import net.minecraft.world.item.*;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraftforge.common.brewing.BrewingRecipeRegistry;import net.minecraftforge.event.ForgeEventFactory;
/** arx's four slots and400tick lifecycle, with native containers/Forge retained. */
public final class SourceBrewingStand1412 extends BrewingStandBlockEntity{
 private static final int[] INGREDIENT={3},BOTTLES={0,1,2};private final NonNullList<ItemStack> contents=NonNullList.withSize(4,ItemStack.EMPTY);private ItemStack oldFuel=ItemStack.EMPTY;private int remaining;private Item startedIngredient=Items.AIR;
 private final ContainerData clock=new ContainerData(){public int getCount(){return 1;}public int get(int id){return id==0?remaining:0;}public void set(int id,int value){if(id==0)remaining=value;}};
 public SourceBrewingStand1412(BlockPos pos,BlockState state){super(pos,state);}
 public int brewingTicks(){return remaining;}
 public static boolean tick(Object rawLevel,Object rawPos,Object rawState,Object raw){if(!(raw instanceof SourceBrewingStand1412 stand))return false;if(!((Level)rawLevel).isClientSide)stand.tickServer();return true;}
 private ItemStack allowedOutput(int slot,ItemStack ingredient){
  var bottle=contents.get(slot);
  if(!WorldContentEvents.itemActionAllowed(level,bottle))return ItemStack.EMPTY;
  var output=BrewingRecipeRegistry.getOutput(bottle,ingredient);
  return WorldContentEvents.itemActionAllowed(level,output)?output:ItemStack.EMPTY;
 }
 private boolean canBrew(){
  var ingredient=contents.get(3);
  if(ingredient.isEmpty()||!WorldContentEvents.itemActionAllowed(level,ingredient))return false;
  for(int slot:BOTTLES)if(!allowedOutput(slot,ingredient).isEmpty())return true;
  return false;
 }
 private void tickServer(){int before=remaining;
  if(remaining>0){--remaining;if(remaining==0){if(canBrew())complete();}else if(!canBrew()||startedIngredient!=contents.get(3).getItem())remaining=0;}
  else if(canBrew()){remaining=400;startedIngredient=contents.get(3).getItem();}
  if(before!=remaining)setChanged();var state=getBlockState();if(state.getBlock() instanceof BrewingStandBlock){var next=state;for(int i=0;i<3;i++)next=next.setValue(BrewingStandBlock.HAS_BOTTLE[i],!contents.get(i).isEmpty());if(next!=state)level.setBlock(worldPosition,next,2);}
 }
 private void complete(){if(ForgeEventFactory.onPotionAttemptBrew(contents))return;var ingredient=contents.get(3);
  if (ingredient.isEmpty() || !WorldContentEvents.itemActionAllowed(level,ingredient)) return;
  boolean changed=false;
  for(int slot:BOTTLES){
   var output=allowedOutput(slot,ingredient);
   if(!output.isEmpty()){contents.set(slot,output);changed=true;}
  }
  if(!changed)return;
  ForgeEventFactory.onPotionBrewed(contents);
  if(ingredient.hasCraftingRemainingItem()){var extra=ingredient.getCraftingRemainingItem();ingredient.shrink(1);if(ingredient.isEmpty())ingredient=extra;else Containers.dropItemStack(level,worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),extra);}else ingredient.shrink(1);contents.set(3,ingredient);level.levelEvent(1035,worldPosition,0);setChanged();
 }
 // A fifth entry exists only while recovering an old save's unused fuel. It is
 // never an input slot. Container drops can therefore recover it even when full.
 public int getContainerSize(){return oldFuel==null||oldFuel.isEmpty()?4:5;}
 public boolean isEmpty(){return contents.stream().allMatch(ItemStack::isEmpty)&&oldFuel.isEmpty();}
 public ItemStack getItem(int slot){return slot==4?oldFuel:slot>=0&&slot<4?contents.get(slot):ItemStack.EMPTY;}
 public ItemStack removeItem(int slot,int amount){if(slot==4){var out=oldFuel.split(amount);if(!out.isEmpty())setChanged();return out;}var out=ContainerHelper.removeItem(contents,slot,amount);if(!out.isEmpty())setChanged();return out;}
 public ItemStack removeItemNoUpdate(int slot){if(slot==4){var out=oldFuel;oldFuel=ItemStack.EMPTY;return out;}return ContainerHelper.takeItem(contents,slot);}
 public void setItem(int slot,ItemStack value){if(slot>=0&&slot<4){contents.set(slot,value);setChanged();}}
 public void clearContent(){contents.clear();oldFuel=ItemStack.EMPTY;setChanged();}
 public boolean stillValid(Player player){return level!=null&&Container.stillValidBlockEntity(this,player);}
 public boolean canPlaceItem(int slot,ItemStack stack){
  if(level!=null&&!WorldContentEvents.itemActionAllowed(level,stack))return false;
  return slot==3?BrewingRecipeRegistry.isValidIngredient(stack):slot>=0&&slot<3&&bottle(stack);
 }
 static boolean bottle(ItemStack stack){return stack.getItem() instanceof PotionItem||stack.is(Items.GLASS_BOTTLE)||BrewingRecipeRegistry.isValidInput(stack);}
 public int[] getSlotsForFace(Direction face){return (face==Direction.UP?INGREDIENT:BOTTLES).clone();}
 public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return canPlaceItem(slot,stack);}
 public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return slot>=0&&slot<4;}
 public void load(CompoundTag tag){super.load(tag);contents.clear();oldFuel=ItemStack.EMPTY;for(var raw:tag.getList("Items",Tag.TAG_COMPOUND)){var item=(CompoundTag)raw;int slot=item.getByte("Slot")&255;if(slot<4)contents.set(slot,ItemStack.of(item));else if(slot==4)oldFuel=ItemStack.of(item);}remaining=Math.max(0,Math.min(400,tag.getShort("BrewTime")));var stored=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("MiteBrewingIngredient"));startedIngredient=stored==null?contents.get(3).getItem():net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(stored);if(startedIngredient==null)startedIngredient=Items.AIR;}
 protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.remove("Fuel");tag.putShort("BrewTime",(short)remaining);var items=new ListTag();for(int slot=0;slot<getContainerSize();slot++){var stack=getItem(slot);if(!stack.isEmpty()){var item=stack.save(new CompoundTag());item.putByte("Slot",(byte)slot);items.add(item);}}tag.put("Items",items);var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(startedIngredient);if(key!=null)tag.putString("MiteBrewingIngredient",key.toString());}
 /** Merge explicitly: native Inventory.add may discard overflow in creative. */
 void recoverFuel(Player player){if(oldFuel.isEmpty()||level==null||level.isClientSide||!stillValid(player)||!player.isAlive()||player.isSpectator())return;int before=oldFuel.getCount();var inventory=player.getInventory();for(int pass=0;pass<2&&!oldFuel.isEmpty();pass++)for(int slot=0;slot<36&&!oldFuel.isEmpty();slot++){var current=inventory.getItem(slot);if(pass==0){if(current.isEmpty()||!ItemStack.isSameItemSameTags(current,oldFuel))continue;int room=Math.min(inventory.getMaxStackSize(),current.getMaxStackSize())-current.getCount();if(room>0){int moved=Math.min(room,oldFuel.getCount());current.grow(moved);oldFuel.shrink(moved);}}else if(current.isEmpty()){int moved=Math.min(oldFuel.getCount(),Math.min(inventory.getMaxStackSize(),oldFuel.getMaxStackSize()));inventory.setItem(slot,oldFuel.split(moved));}}
  if(oldFuel.getCount()!=before){inventory.setChanged();setChanged();player.inventoryMenu.broadcastChanges();}
 }
 protected AbstractContainerMenu createMenu(int id,Inventory inventory){recoverFuel(inventory.player);return new SourceBrewingMenu1412(id,inventory,this,clock);}
}
