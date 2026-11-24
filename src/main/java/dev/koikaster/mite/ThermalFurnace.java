package dev.koikaster.mite;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.ForgeHooks;
public final class ThermalFurnace {
 public static final class BlockImpl extends BaseEntityBlock {
  public final int capacity;public final boolean acceptsLarge;
  public BlockImpl(int c,Properties p){this(c,true,p);}
  public BlockImpl(int c,boolean large,Properties p){super(p);capacity=c;acceptsLarge=large;registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BlockStateProperties.LIT,false).setValue(FurnaceEmbers.FADE,0));}
  protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(BlockStateProperties.HORIZONTAL_FACING,BlockStateProperties.LIT,FurnaceEmbers.FADE);}
  public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection().getOpposite());}
  public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
  public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Entity(p,s);}
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.FURNACE_ENTITY.get(),Entity::tick);}
  public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){
   if(!ClassicFurnaces.openFront(l,p,s))return InteractionResult.PASS;
   if(player instanceof net.minecraft.server.level.ServerPlayer server&&l.getBlockEntity(p) instanceof Entity be){net.minecraftforge.network.NetworkHooks.openScreen(server,be,buffer->{buffer.writeBlockPos(p);buffer.writeVarInt(capacity);buffer.writeBoolean(acceptsLarge);buffer.writeVarInt(ModernCooking.CLASSIC);});}
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  public void onRemove(BlockState s,Level l,BlockPos p,BlockState ns,boolean moving){
   if(!s.is(ns.getBlock())&&l.getBlockEntity(p) instanceof Entity e){Containers.dropContents(l,p,e);l.updateNeighbourForOutputSignal(p,this);}
   super.onRemove(s,l,p,ns,moving);
  }
  public boolean hasAnalogOutputSignal(BlockState s){return true;}
  public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(l.getBlockEntity(p));}
 }
 public static final class Entity extends BaseContainerBlockEntity implements WorldlyContainer {
  private NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
  private int burning,burnTotal,cooking,cookTotal=200,fuelHeat;
  private net.minecraftforge.common.util.LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers=net.minecraftforge.items.wrapper.SidedInvWrapper.create(this,Direction.UP,Direction.DOWN,Direction.NORTH,null);
  public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,Direction side){
   if(!isRemoved()&&capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER){int index=side==null?3:side==Direction.UP?0:side==Direction.DOWN?1:2;return handlers[index].cast();}
   return super.getCapability(capability,side);
  }
  public void invalidateCaps(){super.invalidateCaps();for(var h:handlers)h.invalidate();}
  public void reviveCaps(){super.reviveCaps();handlers=net.minecraftforge.items.wrapper.SidedInvWrapper.create(this,Direction.UP,Direction.DOWN,Direction.NORTH,null);}

  private final ContainerData data=new ContainerData(){
   public int get(int n){return switch(n){case 0->burning;case 1->burnTotal;case 2->cooking;case 3->cookTotal;default->0;};}
   public void set(int n,int v){switch(n){case 0->burning=v;case 1->burnTotal=v;case 2->cooking=v;case 3->cookTotal=v;}}
   public int getCount(){return 4;}
  };
  public Entity(BlockPos p,BlockState s){super(Content.FURNACE_ENTITY.get(),p,s);}
  protected Component getDefaultName(){return Component.translatable(getBlockState().getBlock().getDescriptionId());}
  protected AbstractContainerMenu createMenu(int id,Inventory inv){return new SourceFurnaceMenu(id,inv,this);}
  ContainerData clock(){return data;}
  int heatLevel(){return fuelHeat;}
  public int capacity(){return ((BlockImpl)getBlockState().getBlock()).capacity;}
  public boolean acceptsLarge(){return ((BlockImpl)getBlockState().getBlock()).acceptsLarge;}
  public int getContainerSize(){return 3;}
  public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
  public ItemStack getItem(int i){return items.get(i);}
  public ItemStack removeItem(int i,int n){ItemStack s=ContainerHelper.removeItem(items,i,n);if(!s.isEmpty())setChanged();return s;}
  public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
  public void setItem(int i,ItemStack s){boolean same=ItemStack.isSameItemSameTags(items.get(i),s);items.set(i,s);if(s.getCount()>getMaxStackSize())s.setCount(getMaxStackSize());if(i==0&&!same)cooking=0;setChanged();}
  public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
  public void clearContent(){items.clear();setChanged();}
  public boolean canPlaceItem(int i,ItemStack s){return i==0?level!=null&&ClassicFurnaces.inputAllowed(level,s,acceptsLarge()):i==1&&ClassicFurnaces.fuelAllowed(s,capacity(),acceptsLarge());}
  public int[] getSlotsForFace(Direction d){return d==null?new int[]{0,1,2}:d==Direction.UP?new int[]{0}:d==Direction.DOWN?new int[]{2,1}:new int[]{1};}
  public boolean canPlaceItemThroughFace(int i,ItemStack s,Direction d){return canPlaceItem(i,s);}
  public boolean canTakeItemThroughFace(int i,ItemStack s,Direction d){return d!=Direction.DOWN||i!=1||s.is(Items.BUCKET)||s.getItem() instanceof SpecialItems.MetalBucket bucket&&bucket.fluid(s).isEmpty();}
  protected void saveAdditional(CompoundTag t){super.saveAdditional(t);ContainerHelper.saveAllItems(t,items);t.putInt("Burn",burning);t.putInt("Total",burnTotal);t.putInt("Cook",cooking);t.putInt("CookTotal",cookTotal);t.putInt("Heat",fuelHeat);}
  public void load(CompoundTag t){super.load(t);items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(t,items);burning=Math.max(0,t.getInt("Burn"));burnTotal=Math.max(0,t.getInt("Total"));cooking=Math.max(0,Math.min(199,t.getInt("Cook")));cookTotal=LegacyFurnace.COOK_TICKS;fuelHeat=t.getInt("Heat");}
  static int heat(ItemStack s){int duration=ForgeHooks.getBurnTime(s,RecipeType.SMELTING);return ClassicFuel.resolve(s,duration).heat();}
  static int burnTime(ItemStack s){return ForgeHooks.getBurnTime(s,RecipeType.SMELTING);}
  private ClassicFurnaces.Plan usable(int heat){if(level==null||heat<1||heat>capacity()||!acceptsLarge()&&ClassicFurnaces.large(items.get(0)))return ClassicFurnaces.NONE;var plan=ClassicFurnaces.recipe(level,items.get(0),heat);return heat>=plan.requiredHeat()&&ClassicFurnaces.fits(items.get(2),plan.output(),getMaxStackSize())?plan:ClassicFurnaces.NONE;}
  private boolean canRefuel(ItemStack fuel,int heat){
   return level!=null&&WorldContentEvents.itemActionAllowed(level,fuel)&&ClassicFurnaces.fuelAllowed(fuel,capacity(),acceptsLarge())&&usable(heat).exists();
  }
  public static void tick(Level l,BlockPos p,BlockState state,Entity e){
   if(l.isClientSide)return;
   boolean was=e.burning>0;var fuel=e.items.get(1);int fuelHeat=heat(fuel),duration=burnTime(fuel);boolean canStart=e.canRefuel(fuel,fuelHeat);
   int afterHeat=e.burning>0?e.fuelHeat:0;if(e.burning<=1&&canStart&&duration>0)afterHeat=fuelHeat;var after=e.usable(afterHeat);
   int cookTarget=after.exists()?ClassicFurnaces.cookTicks(l,e.items.get(0),afterHeat):LegacyFurnace.COOK_TICKS;
   boolean flooded=ClassicFurnaces.flooded(l,p,state);var next=LegacyFurnace.tick(true,e.burning,e.fuelHeat,e.cooking,flooded,ClassicFurnaces.smothered(l,p,state),canStart,duration,fuelHeat,after.exists(),cookTarget);
   e.burning=next.burn();e.fuelHeat=next.heat();e.cooking=next.cook();e.cookTotal=cookTarget;
   if(next.extinguished()&&was&&flooded&&l instanceof ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,p.getX()+.5,p.getY()+.7,p.getZ()+.5,5,.2,.1,.2,.02);
   if(next.consumedFuel()){e.burnTotal=duration;ItemStack remaining=fuel.getCraftingRemainingItem();fuel.shrink(1);if(fuel.isEmpty())e.items.set(1,remaining);}
   if(next.completed()){
    var input=e.items.get(0);var result=e.items.get(2);var output=after.output();boolean clay=input.is(Items.CLAY_BALL)&&output.is(Items.BRICK);
    ClassicFurnaces.captureSpongeWater(e,input,after);
    input.shrink(after.consumed());if(result.isEmpty())e.items.set(2,output.copy());else result.grow(output.getCount());
    int produced=output.getCount();if(clay){var current=e.items.get(2);int extra=LegacyFurnace.extraClay(current.getMaxStackSize(),current.getCount(),input.getCount());if(extra>0){current.grow(extra);input.shrink(extra);produced+=extra;}}
    FurnaceExperience.completed(e,after,produced);
   }
   FurnaceEmbers.update(l,p,state,e.burning,e.burnTotal,()->{
    var pending=e.items.get(1);
    return e.canRefuel(pending,heat(pending));
   });
   if(e.burning>0||was||next.completed()||next.consumedFuel())e.setChanged();
  }
 }
}
