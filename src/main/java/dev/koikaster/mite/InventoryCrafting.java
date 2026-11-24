package dev.koikaster.mite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/** 0.4.17: 2x2 inventory crafting with quality selection, XP deduction, and pure-vanilla overflow/close rules. */
public final class InventoryCrafting {
 public static final class OutputSlot extends ResultSlot {
  private final State state;
  public OutputSlot(Player player,CraftingContainer grid,Container result,int index,int x,int y){super(player,grid,result,index,x,y);state=new State(player,grid);}
  public boolean mayPickup(Player player){return state.ready();}
  public ItemStack remove(int amount){return state.ready()?super.remove(amount):ItemStack.EMPTY;}
  boolean allowsIngredients(){return state.allowsIngredients();}
  boolean allowsWorkbench(){return state.allowsWorkbench();}
  public void onTake(Player player,ItemStack stack){
   int qCost=!stack.isEmpty()?state.xpCostFor(stack):state.currentXpCost();
   ClassicCoins.settle(player,state.grid);
   if(player instanceof ServerPlayer server&&!server.isCreative()&&qCost>0){
    if(ClassicQualityCrafting.availableXp(player)<qCost)throw new IllegalStateException("Inventory quality result taken without required experience");
    PlayerState.addXp(server,-qCost);
    Network.sync(server);
   }
   state.consume();
   ClassicCraftRepair.settle(player,state.grid);
   super.onTake(player,stack);
   player.causeFoodExhaustion(.15f);
   state.refresh();
  }
 }
 private static State state(Object raw){
  var menu=(InventoryMenu)raw;return !menu.slots.isEmpty()&&menu.getSlot(0) instanceof OutputSlot slot?slot.state:null;
 }
 public static void changed(Object raw){
  var state=state(raw);
  if(state==null)return;
  var menu=(InventoryMenu)raw;
  if(!state.owner.level().isClientSide&&ModernContentPolicy.removed(menu.getSlot(0).getItem())){
   menu.getSlot(0).set(ItemStack.EMPTY);
  }
  state.recipeManager=state.owner.level().getRecipeManager();
  state.refresh();
 }
 public static void beginClose(Object raw){
  var state=state(raw);
  if(state==null)return;
  if(state.pureVanilla()){
   CraftingCheckpoints.consume(state.owner,"inventory",state.signature);
   state.progress=0;state.requested=false;
   if(!state.owner.level().isClientSide){
    for(int i=0;i<state.grid.getContainerSize();i++){
     var rem=state.grid.removeItemNoUpdate(i);
     if(!rem.isEmpty()&&!state.owner.getInventory().add(rem))state.owner.drop(rem,false);
    }
    state.owner.inventoryMenu.getSlot(0).set(ItemStack.EMPTY);
   }
  }else{
   state.save();
  }
  state.closing=true;
 }
 public static void endClose(Object raw){var state=state(raw);if(state!=null){state.closing=false;state.requested=false;state.signature="";state.progress=0;state.total=1;state.qualitySelection=-1;state.publish(true);}}
 public static boolean click(Object raw,int slot,int button,Object rawType,Object rawPlayer){
  if(!(raw instanceof InventoryMenu menu)||slot!=0)return false;
  var type=(net.minecraft.world.inventory.ClickType)rawType;var player=(Player)rawPlayer;var state=state(menu);
  if(state==null||player!=state.owner||player.isCreative())return false;
  state.refresh();
  if((type==net.minecraft.world.inventory.ClickType.PICKUP||type==net.minecraft.world.inventory.ClickType.QUICK_MOVE)&&button==1&&ClassicQualityCrafting.supportsQuality(state.currentRecipe,state.assembledOutput)){
   if(!player.level().isClientSide&&state.hasCurrentResult()){
    state.cycleQuality();
    menu.broadcastChanges();
   }
   return true;
  }
  if(!state.allowsIngredients()||!state.allowsWorkbench()||!SourceSkillCrafting1710.allowed(player,state.currentRecipe,state.grid,state.assembledOutput)||SourceCraftingContext1533.upperBodyInWeb(player)||!state.affordableExperience())return true;
  if((type==net.minecraft.world.inventory.ClickType.PICKUP||type==net.minecraft.world.inventory.ClickType.QUICK_MOVE)&&(button==0||button==1)&&!state.signature.isEmpty()){
   if(!player.level().isClientSide&&state.hasCurrentResult()){
    state.requested=true;
    OriginalCurseEffects.has(player,8,true);
   }
  }
  return true;
 }
 public static boolean canQuickMove(Object raw,int index){
  var menu=(InventoryMenu)raw;var state=state(raw);boolean allowed=index>=0&&index<menu.slots.size()&&(index!=0||state!=null&&state.ready());
  return allowed;
 }
 public static ItemStack emptyResult(){return ItemStack.EMPTY;}
 public static void tick(Player player){
  var state=state(player.inventoryMenu);if(state==null||player.level().isClientSide)return;
  state.refresh();if(player.containerMenu!=player.inventoryMenu||state.closing)return;
  if(!state.allowsIngredients()||!state.allowsWorkbench()||!SourceSkillCrafting1710.allowed(player,state.currentRecipe,state.grid,state.assembledOutput)||SourceCraftingContext1533.upperBodyInWeb(player)||!state.affordableExperience()){if(state.requested||state.progress>0){CraftingCheckpoints.consume(player,"inventory",state.signature);state.requested=false;state.progress=0;state.publish(true);}return;}
  long now=player.level().getGameTime();if(state.lastWorld==player.level()&&state.lastTick==now)return;state.lastWorld=player.level();state.lastTick=now;
  if(state.requested&&!state.signature.isEmpty()&&(player.isCreative()||SurvivalFood.hasFoodEnergy(player))&&state.progress<state.total){
   state.progress++;if(state.progress%20==0||state.progress==state.total)state.save();state.publish(state.progress%5==0||state.progress==state.total);
  }
  boolean overflow=state.pureVanilla();
  if(state.requested&&state.ready()&&(overflow||ContinuousCrafting.canStore(player,player.inventoryMenu.getSlot(0).getItem()))){
   int paidXp=state.currentXpCost()+ClassicCoins.mintCost(player.inventoryMenu.getSlot(0).getItem());
   String previous=state.signature;
   var outSlot=player.inventoryMenu.getSlot(0);
   var copy=outSlot.getItem().copy();
   var taken=player.inventoryMenu.quickMoveStack(player,0);
   if(taken.isEmpty()&&overflow&&!copy.isEmpty()){
    copy.getItem().onCraftedBy(copy,player.level(),player);
    outSlot.onQuickCraft(ItemStack.EMPTY,copy);
    outSlot.set(ItemStack.EMPTY);
    outSlot.onTake(player,copy);
    player.drop(copy,false);
    taken=copy;
   }
   state.refresh();
   if(!taken.isEmpty()){
    state.progress=0;state.requested=(!overflow||paidXp==0)&&!state.signature.isEmpty()&&previous.equals(state.signature);
    state.publish(true);state.save();player.inventoryMenu.broadcastChanges();
   }
  }
 }
 public static int progress(Player player){var state=state(player.inventoryMenu);if(state==null)return 0;state.refresh();return state.value(false);}
 public static int total(Player player){var state=state(player.inventoryMenu);if(state==null)return 1;state.refresh();return state.value(true);}
 private static final class State {
  final Player owner;final CraftingContainer grid;
  private RecipeManager recipeManager;
  private CraftingRecipe currentRecipe;
  private ItemStack assembledOutput=ItemStack.EMPTY;
  String signature="";int progress,total=1,qualitySelection=-1;long lastTick=Long.MIN_VALUE;net.minecraft.world.level.Level lastWorld;boolean closing,requested;
  State(Player owner,CraftingContainer grid){this.owner=owner;this.grid=grid;ClassicCoins.bind(grid,owner);}
  boolean pureVanilla(){return !WorldContentMode.rebuilt(owner.level());}
  int currentXpCost(){return xpCostFor(assembledOutput);}
  int xpCostFor(ItemStack out){
   if(out==null||out.isEmpty()||!ClassicQualityCrafting.supportsQuality(currentRecipe,out))return 0;
   float baseDiff=CraftingDifficulty.difficulty(owner,grid,currentRecipe);
   return ClassicQualityCrafting.xpCost(owner,baseDiff,ClassicQuality.get(out),false);
  }
  boolean affordableExperience(){
   return ClassicCoins.affordable(owner,assembledOutput)&&ClassicQualityCrafting.availableXp(owner)>=ClassicCoins.mintCost(assembledOutput)+currentXpCost();
  }
  void cycleQuality(){
   if(currentRecipe==null||assembledOutput.isEmpty())return;
   var baseOut=currentRecipe.assemble(grid,owner.level().registryAccess());
   boolean hasSkill=ClassicQualityCrafting.hasSkill(owner,currentRecipe,grid,baseOut);
   float baseDiff=CraftingDifficulty.difficulty(owner,grid,currentRecipe);
   var minQ=ClassicQualityCrafting.minQuality(owner,baseOut,hasSkill);
   var maxQ=ClassicQualityCrafting.maxQuality(owner,baseDiff,baseOut,hasSkill,false);
   requested=false;progress=0;
   if(!pureVanilla())CraftingCheckpoints.consume(owner,"inventory",signature);
   qualitySelection=ClassicQualityCrafting.cycleNext(qualitySelection,minQ,maxQ).ordinal();
   refresh();
   if(!pureVanilla())CraftingCheckpoints.consume(owner,"inventory",signature);
   progress=0;publish(true);
  }
  void refresh(){
   if(closing)return;
   var level=owner.level();
   var manager=level.getRecipeManager();
   boolean recipesReloaded=recipeManager!=null&&recipeManager!=manager;
   recipeManager=manager;
   if(recipesReloaded&&!level.isClientSide){
    owner.inventoryMenu.slotsChanged(grid);
    return;
   }
   CraftingRecipe previousRecipe=currentRecipe;
   var recipe=manager.getRecipeFor(RecipeType.CRAFTING,grid,level);
   currentRecipe=recipe.orElse(null);
   var output=recipe.map(r->r.assemble(grid,level.registryAccess())).orElse(ItemStack.EMPTY);
   if(!output.isEmpty()&&(!output.isItemEnabled(level.enabledFeatures())||ModernContentPolicy.removed(output)))output=ItemStack.EMPTY;
   if(ClassicQualityCrafting.supportsQuality(currentRecipe,output)){
    boolean hasSkill=ClassicQualityCrafting.hasSkill(owner,currentRecipe,grid,output);
    float baseDiff=CraftingDifficulty.difficulty(owner,grid,currentRecipe);
    var minQ=ClassicQualityCrafting.minQuality(owner,output,hasSkill);
    var maxQ=ClassicQualityCrafting.maxQuality(owner,baseDiff,output,hasSkill,false);
    if(previousRecipe!=currentRecipe||qualitySelection<0){
     qualitySelection=minQ.ordinal();
    }else{
     qualitySelection=ClassicQualityCrafting.clampToRange(qualitySelection,minQ,maxQ).ordinal();
    }
    output=ClassicQuality.set(output.copy(),ClassicQuality.Level.byOrdinal(qualitySelection));
   }else if(!output.isEmpty()){
    qualitySelection=-1;
   }
   assembledOutput=output;
   if(!level.isClientSide&&owner.inventoryMenu!=null&&!owner.inventoryMenu.slots.isEmpty()){
    var slot0=owner.inventoryMenu.getSlot(0);
    if(!ItemStack.matches(slot0.getItem(),output))slot0.set(output.copy());
   }
   StringBuilder key=new StringBuilder();
   if(!output.isEmpty()){
    key.append(recipe.orElseThrow().getId()).append('|').append(output.save(new net.minecraft.nbt.CompoundTag()));
    for(int i=0;i<grid.getContainerSize();i++){var stack=grid.getItem(i);if(!stack.isEmpty()){var item=stack.copy();item.setCount(1);key.append('|').append(i).append(':').append(item.save(new net.minecraft.nbt.CompoundTag()));}}
   }
   String next=key.toString();int nextTotal=output.isEmpty()?1:CraftingDifficulty.period(owner,grid,recipe.orElse(null),false);
   if(!signature.equals(next)){
    save();requested=false;signature=next;total=nextTotal;progress=pureVanilla()||next.isEmpty()?0:CraftingCheckpoints.restore(owner,"inventory",next,total);publish(true);
   }else if(previousRecipe!=currentRecipe&&(!allowsIngredients()||!allowsWorkbench()||!SourceSkillCrafting1710.allowed(owner,currentRecipe,grid,output)||!affordableExperience())){
    requested=false;progress=0;total=nextTotal;if(!pureVanilla())CraftingCheckpoints.consume(owner,"inventory",signature);publish(true);
   }else {boolean changed=total!=nextTotal;total=nextTotal;progress=Math.min(progress,total);if(changed)publish(true);}
  }
  int value(boolean duration){
   if(!owner.level().isClientSide)return duration?total:progress;
   var data=PlayerState.data(owner);boolean same=!signature.isEmpty()&&data.getString("InventoryCraftKey").equals(CraftingCheckpoints.id("inventory",signature));
   return same?(duration?Math.max(1,data.getInt("InventoryCraftTotal")):Math.max(0,data.getInt("InventoryCraftProgress"))):(duration?total:0);
  }
  boolean hasCurrentResult(){
   return !assembledOutput.isEmpty()&&ItemStack.matches(assembledOutput,owner.inventoryMenu.getSlot(0).getItem());
  }
  boolean allowsIngredients(){return SourceR196Crafting.allows(owner.level(),grid,currentRecipe);}
  boolean allowsWorkbench(){return SourceR196Crafting.allowsWorkbench(owner.level(),grid,currentRecipe,owner.inventoryMenu.getSlot(0).getItem(),0);}
  boolean ready(){
   refresh();
   if(!owner.level().isClientSide&&!hasCurrentResult())return false;
   return allowsIngredients()&&allowsWorkbench()&&SourceSkillCrafting1710.allowed(owner,currentRecipe,grid,assembledOutput)&&!SourceCraftingContext1533.upperBodyInWeb(owner)
       &&affordableExperience()
       &&(owner.isCreative()||!signature.isEmpty()&&SurvivalFood.hasFoodEnergy(owner)&&value(false)>=value(true));
  }
  void save(){if(!pureVanilla()&&!signature.isEmpty())CraftingCheckpoints.save(owner,"inventory",signature,progress);}
  void consume(){
   if(!pureVanilla())CraftingCheckpoints.consume(owner,"inventory",signature);requested=false;progress=0;publish(true);
  }
  void publish(boolean send){
   if(owner.level().isClientSide)return;
   var data=PlayerState.data(owner);String key=signature.isEmpty()?"":CraftingCheckpoints.id("inventory",signature);
   data.putString("InventoryCraftKey",key);data.putInt("InventoryCraftProgress",progress);data.putInt("InventoryCraftTotal",total);
   if(send&&owner instanceof ServerPlayer player&&!(player instanceof net.minecraftforge.common.util.FakePlayer)&&player.connection!=null)Network.crafting(player,key,progress,total);
  }
 }
 private InventoryCrafting(){}
}
