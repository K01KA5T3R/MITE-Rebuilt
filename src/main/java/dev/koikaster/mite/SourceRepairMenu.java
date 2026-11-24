package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** va/vc slot positions and take transaction, without native anvil levels/prior-work cost. */
public final class SourceRepairMenu extends AbstractContainerMenu implements SourceCraftBatch1722.Access {
 public final BlockPos pos;public final String material;public final SimpleContainer inputs;
 public final ResultContainer output=new ResultContainer();private final Player owner;private final SourceAnvil openedAnvil;private boolean committing;private String name="";private ItemStack lastFirst=ItemStack.EMPTY;
 private ClassicRepair.Result result=ClassicRepair.Result.empty();private boolean skillSeen,skillWorld;private int skillBits;
 public SourceRepairMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,buffer.readBlockPos(),buffer.readUtf(32),null);}
 public SourceRepairMenu(int id,Inventory inventory,SourceAnvil anvil){this(id,inventory,anvil.getBlockPos(),anvil.material(),anvil);}
 private SourceRepairMenu(int id,Inventory inventory,BlockPos pos,String material,SourceAnvil openedAnvil){super(Content.REPAIR_MENU.get(),id);this.pos=pos;this.material=material;this.openedAnvil=openedAnvil;owner=inventory.player;inputs=new SimpleContainer(2){public void setChanged(){super.setChanged();SourceRepairMenu.this.slotsChanged(this);}};
  addSlot(new Slot(inputs,0,27,47));addSlot(new Slot(inputs,1,76,47){public boolean mayPlace(ItemStack stack){return acceptsConsumable(stack);}});
  addSlot(new Slot(output,0,134,47){
   public boolean mayPlace(ItemStack stack){return false;}
   public boolean mayPickup(Player player){
    return !getItem().isEmpty()&&stillValid(player)&&canTake(repairResult());
   }
   public ItemStack remove(int amount){return mayPickup(owner)?super.remove(amount):ItemStack.EMPTY;}
   public void onTake(Player player,ItemStack stack){take();super.onTake(player,stack);}
  });
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
 }
 public void slotsChanged(Container container){if(committing||container!=inputs)return;var first=inputs.getItem(0);if(!ItemStack.isSameItemSameTags(first,lastFirst)){name=first.hasCustomHoverName()?first.getHoverName().getString():"";lastFirst=first.copy();}result=SourceSkillRepair1710.result(owner,first,inputs.getItem(1),material,name);output.setItem(0,result.output());broadcastChanges();}
 public boolean rename(String text){if(text.length()>40||!ClassicRepair.canRename(inputs.getItem(0)))return false;String clean=net.minecraft.SharedConstants.filterText(text).trim();if(name.equals(clean))return false;name=clean;result=SourceSkillRepair1710.result(owner,inputs.getItem(0),inputs.getItem(1),material,name);output.setItem(0,result.output());broadcastChanges();return true;}
 public void broadcastChanges(){boolean enabled=SourceSkills1685.enabled(owner.level());int bits=enabled?SourceSkills1685.bits(owner):0;if(!committing&&(!skillSeen||enabled!=skillWorld||bits!=skillBits)){skillSeen=true;skillWorld=enabled;skillBits=bits;result=SourceSkillRepair1710.result(owner,inputs.getItem(0),inputs.getItem(1),material,name);output.setItem(0,result.output());}super.broadcastChanges();}
 private boolean acceptsConsumable(ItemStack stack){return ClassicRepair.consumable(stack,material,WorldContentMode.r196(owner.level()));}
 public String name(){return name;}
 private ClassicRepair.Result repairResult(){
  return SourceSkillRepair1710.result(owner,inputs.getItem(0),inputs.getItem(1),material,name);
 }
 boolean gradeBlocked(){
  return needsStrongerAnvil(repairResult());
 }
 private boolean needsStrongerAnvil(ClassicRepair.Result preview){
  return WorldContentMode.r196(owner.level())&&ClassicRepair.requiresStrongerAnvil(preview,inputs.getItem(0),material);
 }
 private boolean canTake(ClassicRepair.Result preview){
  return !preview.output().isEmpty()&&!needsStrongerAnvil(preview);
 }
 private void take(){
  var completed=repairResult();
  if(!canTake(completed))return;
  committing=true;
  try{
   inputs.setItem(0,ItemStack.EMPTY);
   if(completed.consumed()>0)inputs.removeItem(1,completed.consumed());
   else inputs.setItem(1,ItemStack.EMPTY);
   output.setItem(0,ItemStack.EMPTY);
   if(!owner.level().isClientSide&&originalAnvilPresent()){
    if(completed.anvilDamage()>0)openedAnvil.addDamage(completed.anvilDamage());
    owner.level().levelEvent(1030,pos,0);
   }
  }finally{committing=false;}
  lastFirst=ItemStack.EMPTY;
  slotsChanged(inputs);
 }
 private boolean originalAnvilPresent(){
  var level=owner.level();
  return openedAnvil!=null&&openedAnvil.getLevel()==level&&!openedAnvil.isRemoved()
      &&level.getBlockEntity(pos)==openedAnvil
      &&level.getBlockState(pos).getBlock() instanceof MetalAnvilBlock block&&material.equals(block.material);
 }
 public boolean stillValid(Player player){
  if(player!=owner||player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64)return false;
  return player.level().isClientSide?ClassicRepair.anvil(player.level().getBlockState(pos)):originalAnvilPresent();
 }
 public void removed(Player player){super.removed(player);if(!player.level().isClientSide)clearContainer(player,inputs);output.clearContent();}
 public ItemStack quickMoveStack(Player player,int index){if(!stillValid(player))return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
  if(index==2){if(!SourceCraftBatch1722.move(this,stack,3,39,true))return ItemStack.EMPTY;slot.onQuickCraft(stack,original);}
  else if(index<2){if(!moveItemStackTo(stack,3,39,false))return ItemStack.EMPTY;}
  else if(acceptsConsumable(stack)){if(!moveItemStackTo(stack,1,2,false)&&!moveItemStackTo(stack,0,2,false))return ItemStack.EMPTY;}
  else if(!moveItemStackTo(stack,0,2,false))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==original.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return original;
 }
 @Override public boolean mite$moveOneCraft1722(ItemStack stack,int start,int end,boolean reverse){
  return moveItemStackTo(stack,start,end,reverse);
 }
}
