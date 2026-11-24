package dev.koikaster.mite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;

/** Client/server share source slots; native furnace layout, result handling and progress remain usable. */
public final class SourceFurnaceMenu extends AbstractFurnaceMenu {
 public final int capacity;public final boolean acceptsLarge;public final BlockPos furnacePos;public final int cookingMode;
 private final Player skillOwner;private final Container furnace;private final ContainerData experience;private final DataSlot sourceHeat;
 public SourceFurnaceMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,new SimpleContainer(3),new SimpleContainerData(4),buffer.readBlockPos(),buffer.readVarInt(),buffer.readBoolean(),buffer.readVarInt());}
 public SourceFurnaceMenu(int id,Inventory inventory,ThermalFurnace.Entity entity){this(id,inventory,entity,entity.clock(),entity.getBlockPos(),entity.capacity(),entity.acceptsLarge(),ModernCooking.CLASSIC);}
 public SourceFurnaceMenu(int id,Inventory inventory,net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity entity){this(id,inventory,entity,ModernCooking.clock(entity),entity.getBlockPos(),ModernCooking.capacity(ModernCooking.mode(entity.getBlockState())),true,ModernCooking.mode(entity.getBlockState()));}
 private SourceFurnaceMenu(int id,Inventory inventory,Container container,ContainerData clock,BlockPos pos,int capacity,boolean acceptsLarge,int cookingMode){
  super(Content.FURNACE_MENU.get(),RecipeType.SMELTING,RecipeBookType.FURNACE,id,inventory,container,clock);this.capacity=capacity;this.acceptsLarge=acceptsLarge;this.furnacePos=pos;this.furnace=container;this.cookingMode=cookingMode;this.skillOwner=inventory.player;
  experience=container instanceof net.minecraft.world.level.block.entity.BlockEntity be?FurnaceExperience.data(be):new SimpleContainerData(2);addDataSlots(experience);sourceHeat=new SourceFurnaceHeat1533(container);addDataSlot(sourceHeat);
  Slot input=new Slot(container,0,56,17){public boolean mayPlace(ItemStack stack){return canSmelt(stack);}};input.index=0;slots.set(0,input);
  Slot fuel=new Slot(container,1,56,53){
   @Override public boolean mayPlace(ItemStack stack) {
    return isFuel(stack) || isSpongeBucket(stack);
   }
   @Override public int getMaxStackSize(ItemStack stack) {
    return isSpongeBucket(stack) ? 1 : super.getMaxStackSize(stack);
   }
   private boolean isSpongeBucket(ItemStack stack) {
    return acceptsLarge && cookingMode != ModernCooking.SMOKER && ClassicFurnaces.emptySpongeBucket(stack);
   }
  };fuel.index=1;slots.set(1,fuel);
  Slot result=new SourceResultSlot(inventory.player,container);result.index=2;slots.set(2,result);
 }
 protected boolean isFuel(ItemStack stack){return ClassicFurnaces.fuelAllowed(stack,capacity,acceptsLarge);}
 public int heatLevel(){return sourceHeat.get();}
 public int storedExperience(){return (experience.get(0)&65535)|(experience.get(1)&32767)<<16;}
 public boolean clickMenuButton(Player player,int button){return button==0&&player instanceof net.minecraft.server.level.ServerPlayer server&&player.containerMenu==this&&stillValid(player)&&furnace instanceof net.minecraft.world.level.block.entity.BlockEntity be&&FurnaceExperience.claim(be,server)>0;}
 protected boolean canSmelt(ItemStack stack){if(!SourceSkillInputs1791.furnace(skillOwner,stack,cookingMode))return false;return cookingMode==ModernCooking.CLASSIC?ClassicFurnaces.inputAllowed(level,stack,acceptsLarge):ModernCooking.inputAllowed(level,stack,cookingMode);}
 public boolean stillValid(Player player){if(furnace instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity entity)return entity.stillValid(player);var state=player.level().getBlockState(furnacePos);return (state.getBlock() instanceof ThermalFurnace.BlockImpl||state.is(net.minecraft.world.level.block.Blocks.FURNACE)||state.is(net.minecraft.world.level.block.Blocks.SMOKER)||state.is(net.minecraft.world.level.block.Blocks.BLAST_FURNACE))&&player.distanceToSqr(furnacePos.getX()+.5,furnacePos.getY()+.5,furnacePos.getZ()+.5)<=64;}
 private static final class SourceResultSlot extends Slot {
  private final Player owner;private int taken;private ItemStack identity=ItemStack.EMPTY;
  SourceResultSlot(Player player,Container container){super(container,2,116,35);owner=player;}
  public boolean mayPlace(ItemStack stack){return false;}
  public ItemStack remove(int amount){if(hasItem()){identity=getItem().copyWithCount(1);taken+=Math.min(amount,getItem().getCount());}return super.remove(amount);}
  public void onQuickCraft(ItemStack remaining,ItemStack original){int count=original.getCount()-remaining.getCount();if(count>0){identity=original.copyWithCount(1);taken+=count;}}
  protected void onSwapCraft(int count){if(hasItem())identity=getItem().copyWithCount(1);taken+=count;}
  public void onTake(Player player,ItemStack stack){
   var item=identity.isEmpty()?stack.copyWithCount(1):identity;int quantity=taken;identity=ItemStack.EMPTY;taken=0;
   if(quantity>0&&!item.isEmpty()){
    if(owner.level() instanceof net.minecraft.server.level.ServerLevel server&&!WorldContentMode.rebuilt(server)){
     int reward=ClassicFurnaces.reward(item);
     if(reward>0)server.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(server,owner.getX(),owner.getY()+.5,owner.getZ()+.5,reward*quantity));
    }
    item.onCraftedBy(owner.level(),owner,quantity);net.minecraftforge.event.ForgeEventFactory.firePlayerSmeltedEvent(owner,item.copyWithCount(quantity));
   }
   super.onTake(player,stack);
  }
 }
}
