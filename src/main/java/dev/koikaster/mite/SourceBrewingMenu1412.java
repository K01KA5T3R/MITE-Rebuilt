package dev.koikaster.mite;
import net.minecraft.world.*;import net.minecraft.world.entity.player.*;import net.minecraft.world.inventory.*;import net.minecraft.world.item.*;import net.minecraft.world.item.alchemy.PotionUtils;import net.minecraft.server.level.ServerPlayer;
/** vf's four slot positions, transfer ranges and source boil listener owner. */
public final class SourceBrewingMenu1412 extends AbstractContainerMenu{
 private final Container stand;private final ContainerData clock;private final Player owner;private final Slot ingredient;
 public SourceBrewingMenu1412(int id,Inventory inventory){this(id,inventory,new SimpleContainer(4),new SimpleContainerData(1));}
 public SourceBrewingMenu1412(int id,Inventory inventory,Container stand,ContainerData clock){super(Content.BREW_MENU.get(),id);checkContainerSize(stand,4);checkContainerDataCount(clock,1);this.stand=stand;this.clock=clock;owner=inventory.player;stand.startOpen(owner);
  for(int i=0;i<3;i++){final int slot=i;addSlot(new Slot(stand,i,new int[]{56,79,102}[i],new int[]{46,53,46}[i]){public int getMaxStackSize(){return 1;}public boolean mayPlace(ItemStack stack){return SourceBrewingStand1412.bottle(stack);}public ItemStack remove(int count){return SourceAchievementPotion572.removed(super.remove(count),this);}protected void onQuickCraft(ItemStack original,int count){SourceAchievementPotion572.quick(this,original,count);super.onQuickCraft(original,count);}public void onTake(Player player,ItemStack stack){SourceAchievementPotion572.take(this,player,stack);if(player instanceof ServerPlayer server&&stack.getItem() instanceof PotionItem)net.minecraft.advancements.CriteriaTriggers.BREWED_POTION.trigger(server,PotionUtils.getPotion(stack));super.onTake(player,stack);}});}
  ingredient=addSlot(new Slot(stand,3,79,17){public boolean mayPlace(ItemStack stack){return SourceSkillInputs1791.brewing(owner)&&net.minecraftforge.common.brewing.BrewingRecipeRegistry.isValidIngredient(stack);}});
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));addDataSlots(clock);
 }
 public int getBrewingTicks(){return clock.get(0);}public boolean stillValid(Player player){return stand.stillValid(player);}
 public void broadcastChanges(){if(stand instanceof SourceBrewingStand1412 source)source.recoverFuel(owner);super.broadcastChanges();SourceBrewingSound.changed(this,stand,getBrewingTicks());}
 public void addSlotListener(net.minecraft.world.inventory.ContainerListener listener){super.addSlotListener(listener);SourceBrewingSound.watch(this,stand,listener,true);}public void removeSlotListener(net.minecraft.world.inventory.ContainerListener listener){super.removeSlotListener(listener);SourceBrewingSound.watch(this,stand,listener,false);}
 public void removed(Player player){super.removed(player);stand.stopOpen(player);}
 public ItemStack quickMoveStack(Player player,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();
  if(index<4){if(!moveItemStackTo(stack,4,40,true))return ItemStack.EMPTY;slot.onQuickCraft(stack,before);}
  else if(!ingredient.hasItem()&&ingredient.mayPlace(stack)){if(!moveItemStackTo(stack,3,4,false))return ItemStack.EMPTY;}
  else if(SourceBrewingStand1412.bottle(stack)){if(!moveItemStackTo(stack,0,3,false))return ItemStack.EMPTY;}
  else if(index<31){if(!moveItemStackTo(stack,31,40,false))return ItemStack.EMPTY;}
  else if(!moveItemStackTo(stack,4,31,false))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return before;
 }
}
