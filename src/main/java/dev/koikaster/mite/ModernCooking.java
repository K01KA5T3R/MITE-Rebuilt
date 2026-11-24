package dev.koikaster.mite;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Human choice79A: modern devices use source heat/cost rules. These adapters are not R195 inventions. */
@Mod.EventBusSubscriber(modid=MiteReforged.ID)
public final class ModernCooking {
 public static final int CLASSIC=0,SMOKER=1,BLAST=2,CAMPFIRE_TICKS=600;
 private static final String KEY="MiteCooking";
 public static int mode(BlockState state){return state.is(Blocks.SMOKER)?SMOKER:state.is(Blocks.BLAST_FURNACE)?BLAST:CLASSIC;}
 public static boolean managed(Object raw){return raw instanceof AbstractFurnaceBlockEntity be&&(be.getBlockState().is(Blocks.FURNACE)||be.getBlockState().is(Blocks.SMOKER)||be.getBlockState().is(Blocks.BLAST_FURNACE));}
 public static int capacity(int mode){return mode==SMOKER?1:2;}
 public static CompoundTag state(BlockEntity be){var root=be.getPersistentData();if(!root.contains(KEY,10)){var data=new CompoundTag();data.putInt("Version",1);root.put(KEY,data);}return root.getCompound(KEY);}
 public static ClassicFurnaces.Plan recipe(Level level,ItemStack input,int heat,int mode){
  if(mode==CLASSIC)return ClassicFurnaces.recipe(level,input,heat);
  if(mode==SMOKER&&FoodPreservation1905.dried(input)!=null)
   return WorldContentMode.rebuilt(level)?FoodPreservation1905.smoke(input):ClassicFurnaces.NONE;
  if(input.isEmpty()||input.getItem().isEdible()!=(mode==SMOKER))return ClassicFurnaces.NONE;
  if(mode==BLAST&&ClassicFurnaces.sandstone(input))return new ClassicFurnaces.Plan(new ItemStack(input.is(Items.RED_SANDSTONE)||input.is(Items.CUT_RED_SANDSTONE)||input.is(Items.CHISELED_RED_SANDSTONE)?Items.SMOOTH_RED_SANDSTONE:Items.SMOOTH_SANDSTONE),2,1,0);
  var plan=ClassicFurnaces.recipe(level,input,heat);
  return mode==SMOKER&&(!plan.output().getItem().isEdible()||plan.requiredHeat()>1)?ClassicFurnaces.NONE:plan;
 }
 public static boolean inputAllowed(Level level,ItemStack stack,int mode){if(stack.isEmpty())return false;if(mode==CLASSIC)return ClassicFurnaces.inputAllowed(level,stack,true);var plan=recipe(level,stack,capacity(mode),mode);return plan.exists()&&plan.requiredHeat()<=capacity(mode)||mode==BLAST&&ClassicFurnaces.sand(stack);}
 public static boolean canPlace(Object raw,int slot,Object rawStack){var be=(AbstractFurnaceBlockEntity)raw;var stack=(ItemStack)rawStack;int mode=mode(be.getBlockState());return slot==0?be.getLevel()!=null&&inputAllowed(be.getLevel(),stack,mode):slot==1&&ClassicFurnaces.fuelAllowed(stack,capacity(mode),true);}
 public static boolean canTake(Object raw,int slot,Object rawStack,Object rawDirection){var stack=(ItemStack)rawStack;return rawDirection!=Direction.DOWN||slot!=1||stack.is(Items.BUCKET)||stack.getItem() instanceof SpecialItems.MetalBucket bucket&&bucket.fluid(stack).isEmpty();}
 public static void changedInput(Object raw,int slot,Object rawStack){if(managed(raw)&&slot==0){var be=(AbstractFurnaceBlockEntity)raw;if(!ItemStack.isSameItemSameTags(be.getItem(0),(ItemStack)rawStack))state(be).putInt("Cook",0);}}
 public static ContainerData clock(AbstractFurnaceBlockEntity be){return new ContainerData(){
  public int get(int index){var data=state(be);return switch(index){case 0->data.getInt("Burn");case 1->data.getInt("Total");case 2->data.getInt("Cook");case 3->LegacyFurnace.COOK_TICKS;default->0;};}
  public void set(int index,int value){var data=state(be);switch(index){case 0->data.putInt("Burn",value);case 1->data.putInt("Total",value);case 2->data.putInt("Cook",value);}}
  public int getCount(){return 4;}
 };}
 private static ClassicFurnaces.Plan usable(AbstractFurnaceBlockEntity be,int heat,int mode){if(heat<1||heat>capacity(mode))return ClassicFurnaces.NONE;var plan=recipe(be.getLevel(),be.getItem(0),heat,mode);return heat>=plan.requiredHeat()&&ClassicFurnaces.fits(be.getItem(2),plan.output(),be.getMaxStackSize())?plan:ClassicFurnaces.NONE;}
 private static boolean canRefuel(AbstractFurnaceBlockEntity be,ItemStack fuel,int heat,int mode){
  return WorldContentEvents.itemActionAllowed(be.getLevel(),fuel)&&ClassicFurnaces.fuelAllowed(fuel,capacity(mode),true)&&usable(be,heat,mode).exists();
 }
 /** Called in place of vanilla serverTick only for the three native appliances, including worldgen furnaces. */
 public static void furnaceTick(Object rawLevel,Object rawPos,Object rawState,Object rawEntity){
  var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var be=(AbstractFurnaceBlockEntity)rawEntity;if(level.isClientSide)return;int mode=mode(be.getBlockState());
  if(mode!=CLASSIC&&!WorldContentMode.rebuilt(level))return;
  var data=state(be);
  boolean dirty=false;int steps=mode==CLASSIC?1:2; // Same200 source processing/fuel ticks; modern appliances retain100 world ticks per job.
  for(int i=0;i<steps;i++){
   int burn=Math.max(0,data.getInt("Burn")),heat=data.getInt("Heat"),cook=Math.max(0,Math.min(199,data.getInt("Cook")));
   var fuel=be.getItem(1);int fuelHeat=ThermalFurnace.Entity.heat(fuel),duration=ThermalFurnace.Entity.burnTime(fuel);
   boolean start=canRefuel(be,fuel,fuelHeat,mode);int afterHeat=burn>0?heat:0;if(burn<=1&&start&&duration>0)afterHeat=fuelHeat;var plan=usable(be,afterHeat,mode);
   var state=be.getBlockState();var next=LegacyFurnace.tick(true,burn,heat,cook,ClassicFurnaces.flooded(level,pos,state),ClassicFurnaces.smothered(level,pos,state),start,duration,fuelHeat,plan.exists());
   dirty|=burn!=next.burn()||heat!=next.heat()||cook!=next.cook()||next.consumedFuel()||next.completed();
   data.putInt("Burn",next.burn());data.putInt("Heat",next.heat());data.putInt("Cook",next.cook());
   if(next.consumedFuel()){data.putInt("Total",duration);var remaining=fuel.getCraftingRemainingItem();fuel.shrink(1);if(fuel.isEmpty())be.setItem(1,remaining);}
   if(next.completed()){
    var input=be.getItem(0);boolean clay=input.is(Items.CLAY_BALL)&&plan.output().is(Items.BRICK);var output=be.getItem(2);
    ClassicFurnaces.captureSpongeWater(be,input,plan);
    input.shrink(plan.consumed());if(output.isEmpty())be.setItem(2,plan.output().copy());else output.grow(plan.output().getCount());
    int produced=plan.output().getCount();if(clay){var current=be.getItem(2);int extra=LegacyFurnace.extraClay(current.getMaxStackSize(),current.getCount(),input.getCount());current.grow(extra);input.shrink(extra);produced+=extra;}
    FurnaceExperience.completed(be,plan,produced);
   }
  }
  FurnaceEmbers.update(level,pos,be.getBlockState(),data.getInt("Burn"),data.getInt("Total"),()->{
   var pending=be.getItem(1);
   return canRefuel(be,pending,ThermalFurnace.Entity.heat(pending),mode);
  });
  if(dirty)be.setChanged();
 }
 public static void open(ServerPlayer player,AbstractFurnaceBlockEntity be){int cap=capacity(mode(be.getBlockState()));NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new SourceFurnaceMenu(id,inv,be),be.getDisplayName()),buf->{buf.writeBlockPos(be.getBlockPos());buf.writeVarInt(cap);buf.writeBoolean(true);buf.writeVarInt(mode(be.getBlockState()));});}
 @SubscribeEvent(priority=EventPriority.HIGH) public static void use(PlayerInteractEvent.RightClickBlock event){
  var level=event.getLevel();var be=level.getBlockEntity(event.getPos());var player=event.getEntity();if(!player.mayUseItemAt(event.getPos(),event.getFace(),event.getItemStack())||!level.mayInteract(player,event.getPos()))return;
  if(managed(be)){
   if(player.isSecondaryUseActive()&&(!player.getMainHandItem().isEmpty()||!player.getOffhandItem().isEmpty())){event.setUseBlock(Event.Result.DENY);return;}
   event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));if(!level.isClientSide&&ClassicFurnaces.openFront(level,event.getPos(),be.getBlockState())&&player instanceof ServerPlayer server)open(server,(AbstractFurnaceBlockEntity)be);return;
  }
  if(be instanceof CampfireBlockEntity campfire)CampfireFuel.use(event,campfire);
 }
 public static ClassicFurnaces.Plan campfireFood(Level level,ItemStack input){return FoodPreservation1905.dried(input)!=null?ClassicFurnaces.NONE:recipe(level,input,1,SMOKER);}
 private static ClassicFurnaces.Plan food(Level level,ItemStack input){return campfireFood(level,input);}
 public static Optional<CampfireCookingRecipe> campfireRecipe(Object raw,Object rawStack){var be=(CampfireBlockEntity)raw;var input=(ItemStack)rawStack;if(be.getLevel()==null||be.getItems().stream().noneMatch(ItemStack::isEmpty))return Optional.empty();var plan=food(be.getLevel(),input);return plan.exists()?Optional.of(new CampfireCookingRecipe(Content.id("campfire_food"),"",CookingBookCategory.FOOD,Ingredient.of(input.copyWithCount(1)),plan.output().copy(),0,CAMPFIRE_TICKS)):Optional.empty();}
 public static boolean placeFood(Object raw,Object rawEntity,Object rawStack,int ignoredVanillaTime){var be=(CampfireBlockEntity)raw;var stack=(ItemStack)rawStack;if(be.getLevel()==null||!food(be.getLevel(),stack).exists())return false;for(int i=0;i<4;i++)if(be.getItems().get(i).isEmpty()){be.getItems().set(i,stack.split(1));state(be).putInt("Slot"+i,0);be.setChanged();be.getLevel().sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),3);be.getLevel().gameEvent((Entity)rawEntity,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,be.getBlockPos());return true;}return false;}
 public static void campfireTick(Object rawLevel,Object rawPos,Object rawState,Object rawEntity){var level=(Level)rawLevel;var pos=(BlockPos)rawPos;var be=(CampfireBlockEntity)rawEntity;if(level.isClientSide||!CampfireFuel.burning(level,pos,be))return;var data=state(be);
  for(int i=0;i<4;i++){var plan=food(level,be.getItems().get(i));if(!plan.exists()){data.putInt("Slot"+i,0);continue;}int progress=data.getInt("Slot"+i)+1;if(progress>=CAMPFIRE_TICKS){Block.popResource(level,pos,plan.output().copy());be.getItems().set(i,ItemStack.EMPTY);progress=0;level.sendBlockUpdated(pos,be.getBlockState(),be.getBlockState(),3);}data.putInt("Slot"+i,progress);}be.setChanged();
 }
 public static void campfireCold(Object rawLevel,Object rawPos,Object rawState,Object rawEntity){var be=(CampfireBlockEntity)rawEntity;CampfireFuel.cold(be);var data=state(be);boolean changed=false;for(int i=0;i<4;i++)if(data.getInt("Slot"+i)!=0){data.putInt("Slot"+i,0);changed=true;}if(changed)be.setChanged();}
 private ModernCooking(){}
}
